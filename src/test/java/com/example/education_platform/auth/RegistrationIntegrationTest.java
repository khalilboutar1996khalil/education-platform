package com.example.education_platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.classcode.entity.ClassCode;
import com.example.education_platform.classcode.repository.ClassCodeRepository;
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

/** Self-registration: a class code buys an active account and a session, with no admin in between. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String SECOND_AS_CODE = "INF2AS-TEST";
    private static final String THIRD_AS_CODE = "INF3AS-TEST";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private ClassCodeRepository classCodes;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        classCodes.saveAndFlush(new ClassCode(SECOND_AS_CODE, Level.SECOND_AS, "2ᵉ AS"));
        classCodes.saveAndFlush(new ClassCode(THIRD_AS_CODE, Level.THIRD_AS, "3ᵉ AS"));
    }

    @Test
    void aValidCodeCreatesAnActiveStudentAndSignsThemInImmediately() throws Exception {
        JsonNode body = bodyOf(register("Yacine Cherif", "yacine@eduflow.dz", PASSWORD, SECOND_AS_CODE), 201);

        assertThat(body.at("/tokens/accessToken").asText())
                .describedAs("registering signs you in, so no second login call is needed").isNotBlank();
        assertThat(body.at("/tokens/refreshToken").asText()).isNotBlank();
        assertThat(body.at("/user/role").asText()).isEqualTo("STUDENT");
        assertThat(body.at("/user/status").asText()).isEqualTo("ACTIVE");

        User created = users.findByEmail("yacine@eduflow.dz").orElseThrow();
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void theCodeDecidesTheLevelSoAStudentCannotPickOne() throws Exception {
        JsonNode body = bodyOf(register("Lina Ait Ali", "lina2@eduflow.dz", PASSWORD, THIRD_AS_CODE), 201);

        assertThat(body.at("/user/level").asText()).isEqualTo("THIRD_AS");
        assertThat(users.findByEmail("lina2@eduflow.dz").orElseThrow().getLevel()).isEqualTo(Level.THIRD_AS);
    }

    @Test
    void theNewAccountCanLogInWithThePasswordItChose() throws Exception {
        register("Yacine Cherif", "yacine@eduflow.dz", PASSWORD, SECOND_AS_CODE);

        assertThat(bodyOf(login("yacine@eduflow.dz", PASSWORD), 200).at("/user/email").asText())
                .isEqualTo("yacine@eduflow.dz");
    }

    @Test
    void anUnknownCodeIsRefusedAndCreatesNothing() {
        assertThat(register("Yacine Cherif", "yacine@eduflow.dz", PASSWORD, "NOPE-9999")).hasStatus(422);

        assertThat(users.findByEmail("yacine@eduflow.dz")).isEmpty();
    }

    @Test
    void aRetiredCodeStopsWorking() {
        ClassCode code = classCodes.findByCodeIgnoreCaseAndActiveTrue(SECOND_AS_CODE).orElseThrow();
        code.deactivate();
        classCodes.saveAndFlush(code);

        assertThat(register("Yacine Cherif", "yacine@eduflow.dz", PASSWORD, SECOND_AS_CODE)).hasStatus(422);
    }

    @Test
    void theCodeIsAcceptedWhateverTheCaseAndSpacing() throws Exception {
        assertThat(bodyOf(register("Yacine Cherif", "yacine@eduflow.dz", PASSWORD, "  inf2as-test "), 201)
                .at("/user/level").asText()).isEqualTo("SECOND_AS");
    }

    @Test
    void anAddressThatAlreadyHasAnAccountIsRefused() {
        users.saveAndFlush(new User("Amira Benali", "amira@eduflow.dz",
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));

        assertThat(register("Amira Benali", "amira@eduflow.dz", PASSWORD, SECOND_AS_CODE)).hasStatus(409);
    }

    @Test
    void theEmailIsNormalisedBeforeItIsStored() throws Exception {
        bodyOf(register("Yacine Cherif", "YACINE@Eduflow.DZ", PASSWORD, SECOND_AS_CODE), 201);

        assertThat(users.findByEmail("yacine@eduflow.dz")).isPresent();
    }

    @Test
    void aShortPasswordIsRejectedWithAFieldError() throws Exception {
        MvcTestResult result = register("Yacine Cherif", "yacine@eduflow.dz", "court", SECOND_AS_CODE);

        assertThat(bodyOf(result, 400).at("/errors/password").asText()).isNotBlank();
        assertThat(users.findByEmail("yacine@eduflow.dz")).isEmpty();
    }

    @Test
    void aMissingClassCodeIsRejectedWithAFieldError() throws Exception {
        MvcTestResult result = register("Yacine Cherif", "yacine@eduflow.dz", PASSWORD, "");

        assertThat(bodyOf(result, 400).at("/errors/classCode").asText()).isNotBlank();
    }

    private MvcTestResult register(String fullName, String email, String password, String classCode) {
        return post("/api/v1/auth/register", """
                {"fullName":"%s","email":"%s","password":"%s","classCode":"%s"}"""
                .formatted(fullName, email, password, classCode));
    }

    private MvcTestResult login(String email, String password) {
        return post("/api/v1/auth/login", """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    private JsonNode bodyOf(MvcTestResult result, int expectedStatus) throws Exception {
        assertThat(result).hasStatus(expectedStatus);
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private MvcTestResult post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }
}
