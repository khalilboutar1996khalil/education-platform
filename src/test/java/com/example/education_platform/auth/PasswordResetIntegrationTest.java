package com.example.education_platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.auth.entity.PasswordResetToken;
import com.example.education_platform.auth.repository.PasswordResetTokenRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
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

/** Forgot-password: the half of feature 1 that Step 2 left unbuilt. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PasswordResetIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String NEW_PASSWORD = "Brand-New-Secret-1";
    private static final String EMAIL = "amira@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordResetTokenRepository resetTokens;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Amira Benali", EMAIL,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
    }

    @Test
    void askingForALinkIssuesAHashedTokenAndNeverTheTokenItself() {
        assertThat(forgot(EMAIL)).hasStatus(204);

        PasswordResetToken stored = resetTokens.findAll().getFirst();
        assertThat(stored.getTokenHash()).hasSize(64);
        assertThat(stored.getUser().getEmail()).isEqualTo(EMAIL);
        assertThat(stored.getUsedAt()).isNull();
    }

    @Test
    void anUnknownAddressAnswersExactlyTheSame() {
        assertThat(forgot("nobody@eduflow.dz"))
                .describedAs("a different answer would say who has an account").hasStatus(204);
        assertThat(resetTokens.count()).isZero();
    }

    @Test
    void theLinkSetsTheNewPasswordAndRetiresTheOldOne() throws Exception {
        String raw = issueTokenDirectly();

        assertThat(reset(raw, NEW_PASSWORD)).hasStatus(204);
        assertThat(login(EMAIL, NEW_PASSWORD)).hasStatusOk();
        assertThat(login(EMAIL, PASSWORD))
                .describedAs("the old password is gone").hasStatus(401);
    }

    @Test
    void aResetLinkWorksExactlyOnce() {
        String raw = issueTokenDirectly();
        reset(raw, NEW_PASSWORD);

        assertThat(reset(raw, "Another-Password-99"))
                .describedAs("replaying a spent link must fail").hasStatus(422);
    }

    @Test
    void anExpiredLinkIsRefused() {
        User user = users.findByEmail(EMAIL).orElseThrow();
        String raw = "expired-token-value";
        resetTokens.saveAndFlush(new PasswordResetToken(user, sha256(raw),
                Instant.now().minus(1, ChronoUnit.HOURS)));

        assertThat(reset(raw, NEW_PASSWORD)).hasStatus(422);
    }

    @Test
    void anInventedTokenIsRefused() {
        assertThat(reset("never-issued", NEW_PASSWORD)).hasStatus(422);
    }

    @Test
    void resettingEndsSessionsOpenedWithTheOldPassword() throws Exception {
        String refreshToken = JSON.readTree(login(EMAIL, PASSWORD).getResponse().getContentAsString())
                .at("/tokens/refreshToken").asText();

        reset(issueTokenDirectly(), NEW_PASSWORD);

        assertThat(post("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .describedAs("a password change signs out every session").hasStatus(401);
    }

    @Test
    void aShortNewPasswordIsRejectedInFrench() {
        assertThat(reset(issueTokenDirectly(), "court"))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.newPassword")
                        .isEqualTo("Le mot de passe doit contenir au moins 10 caractères"));
    }

    // ---------- helpers ----------

    /** The raw token only ever leaves through the mail, so a test has to plant its own. */
    private String issueTokenDirectly() {
        User user = users.findByEmail(EMAIL).orElseThrow();
        String raw = "test-reset-token-" + user.getId();
        resetTokens.saveAndFlush(new PasswordResetToken(user, sha256(raw),
                Instant.now().plus(1, ChronoUnit.HOURS)));
        return raw;
    }

    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private MvcTestResult forgot(String email) {
        return post("/api/v1/auth/forgot-password", """
                {"email":"%s"}""".formatted(email));
    }

    private MvcTestResult reset(String token, String newPassword) {
        return post("/api/v1/auth/reset-password", """
                {"token":"%s","newPassword":"%s"}""".formatted(token, newPassword));
    }

    private MvcTestResult login(String email, String password) {
        return post("/api/v1/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    private MvcTestResult post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }
}
