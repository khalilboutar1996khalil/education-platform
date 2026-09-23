package com.example.education_platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises the real login → refresh → logout flow end to end against Postgres. MockMvc runs on the
 * test thread, so the service joins this test's transaction and everything rolls back afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "Correct-Horse-1";
    private static final String EMAIL = "amira@eduflow.dz";
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seedStudent() {
        users.saveAndFlush(new User("Amira Benali", EMAIL,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
    }

    @Test
    void loginReturnsTokensAndProfile() throws Exception {
        JsonNode body = login(EMAIL, PASSWORD);

        assertThat(body.at("/tokens/accessToken").asText()).isNotBlank();
        assertThat(body.at("/tokens/refreshToken").asText()).isNotBlank();
        assertThat(body.at("/user/email").asText()).isEqualTo(EMAIL);
        assertThat(body.at("/user/initials").asText()).isEqualTo("AB");
        assertThat(body.at("/user/role").asText()).isEqualTo("STUDENT");
        assertThat(body.at("/user/level").asText()).isEqualTo("THIRD_AS");
        assertThat(body.at("/user/passwordHash").isMissingNode())
                .describedAs("password hash must never be serialised").isTrue();
    }

    @Test
    void loginIsCaseInsensitiveOnEmail() throws Exception {
        assertThat(login("AMIRA@Eduflow.dz", PASSWORD).at("/user/email").asText()).isEqualTo(EMAIL);
    }

    @Test
    void loginWithAWrongPasswordIsRejected() {
        assertThat(post("/api/v1/auth/login", """
                {"email":"%s","password":"wrong-password"}""".formatted(EMAIL))).hasStatus(401);
    }

    @Test
    void loginWithAnUnknownEmailIsRejected() {
        assertThat(post("/api/v1/auth/login", """
                {"email":"nobody@eduflow.dz","password":"whatever-1"}""")).hasStatus(401);
    }

    @Test
    void loginIsRejectedForADisabledAccount() {
        User user = users.findByEmail(EMAIL).orElseThrow();
        user.setStatus(UserStatus.DISABLED);
        users.saveAndFlush(user);

        assertThat(post("/api/v1/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(EMAIL, PASSWORD))).hasStatus(401);
    }

    @Test
    void anInvalidPayloadIsRejectedWithTheStandardProblemShape() {
        assertThat(post("/api/v1/auth/login", """
                {"email":"not-an-email","password":""}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> {
                    body.assertThat().extractingPath("$.detail").isEqualTo("Erreur de validation");
                    body.assertThat().extractingPath("$.errors.email").isNotNull();
                    body.assertThat().extractingPath("$.errors.password").isNotNull();
                });
    }

    @Test
    void theAccessTokenOpensAProtectedEndpoint() throws Exception {
        String accessToken = login(EMAIL, PASSWORD).at("/tokens/accessToken").asText();

        // No /api/v1 resource exists yet, so 404 already proves the token cleared authentication
        assertThat(mvc.get().uri("/api/v1/ping").header("Authorization", "Bearer " + accessToken))
                .hasStatus(404);
        assertThat(mvc.get().uri("/api/v1/ping")).hasStatus(401);
    }

    @Test
    void aTokenSignedWithAnotherKeyIsRejected() {
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwicm9sZSI6IkFETUlOIn0"
                + ".0000000000000000000000000000000000000000000";

        assertThat(mvc.get().uri("/api/v1/ping").header("Authorization", "Bearer " + forged))
                .hasStatus(401);
    }

    @Test
    void refreshRotatesTheTokenAndTheOldOneStopsWorking() throws Exception {
        JsonNode first = login(EMAIL, PASSWORD);
        String firstRefresh = first.at("/tokens/refreshToken").asText();
        String firstAccess = first.at("/tokens/accessToken").asText();

        JsonNode rotated = bodyOf(post("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}""".formatted(firstRefresh)));

        assertThat(rotated.at("/refreshToken").asText()).isNotBlank().isNotEqualTo(firstRefresh);
        assertThat(rotated.at("/accessToken").asText())
                .describedAs("the jti claim must keep tokens unique within the same second")
                .isNotBlank().isNotEqualTo(firstAccess);

        assertThat(post("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}""".formatted(firstRefresh)))
                .describedAs("a rotated token must not be replayable").hasStatus(401);
    }

    @Test
    void refreshWithAnUnknownTokenSaysTheSessionExpired() {
        assertThat(post("/api/v1/auth/refresh", """
                {"refreshToken":"not-a-real-token"}"""))
                .hasStatus(401)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.detail")
                        .isEqualTo("Session expirée, veuillez vous reconnecter"));
    }

    @Test
    void aFailedLoginStillSaysTheCredentialsAreWrong() {
        assertThat(post("/api/v1/auth/login", """
                {"email":"%s","password":"wrong-password"}""".formatted(EMAIL)))
                .hasStatus(401)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.detail")
                        .isEqualTo("Email ou mot de passe incorrect"));
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        String refreshToken = login(EMAIL, PASSWORD).at("/tokens/refreshToken").asText();

        assertThat(post("/api/v1/auth/logout", """
                {"refreshToken":"%s"}""".formatted(refreshToken))).hasStatus(204);

        assertThat(post("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}""".formatted(refreshToken))).hasStatus(401);
    }

    @Test
    void loggingOutWithAnUnknownTokenIsStillNoContent() {
        assertThat(post("/api/v1/auth/logout", """
                {"refreshToken":"never-existed"}""")).hasStatus(204);
    }

    private JsonNode login(String email, String password) throws Exception {
        return bodyOf(post("/api/v1/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, password)));
    }

    private JsonNode bodyOf(MvcTestResult result) throws Exception {
        assertThat(result).hasStatusOk();
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private MvcTestResult post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }
}
