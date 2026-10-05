package com.example.education_platform.level;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
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

/** Levels are data the admin manages: a new class year needs no release. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LevelApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String STUDENT = "amira@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
    }

    @Test
    void theSixOriginalLevelsAreListedInSchoolOrderWithoutSigningIn() throws Exception {
        JsonNode body = bodyOf(get("/api/v1/levels", null), 200);

        assertThat(body).hasSize(6);
        assertThat(body.get(0).get("code").asText()).isEqualTo("SEVENTH_BASE");
        assertThat(body.get(5).get("code").asText()).isEqualTo("FOURTH_AS");
        assertThat(body.get(3).get("name").asText()).isEqualTo("2ᵉ AS informatique");
    }

    @Test
    void anAdminCreatesALevelAndItGoesLastWithAnUpperCaseReference() throws Exception {
        JsonNode created = bodyOf(create("1as", "1ʳᵉ année secondaire"), 201);

        assertThat(created.get("code").asText()).isEqualTo("1AS");
        assertThat(created.get("active").asBoolean()).isTrue();
        JsonNode list = bodyOf(get("/api/v1/levels", null), 200);
        assertThat(list.get(list.size() - 1).get("code").asText()).isEqualTo("1AS");
    }

    @Test
    void aStudentCanRegisterInALevelCreatedByTheAdmin() throws Exception {
        create("1AS", "1ʳᵉ année secondaire");

        JsonNode body = bodyOf(register("yacine@eduflow.dz", "1AS"), 201);

        assertThat(body.at("/user/level").asText()).isEqualTo("1AS");
        assertThat(users.findByEmail("yacine@eduflow.dz").orElseThrow().getLevel()).isEqualTo(new Level("1AS"));
    }

    @Test
    void registeringInALevelThatDoesNotExistIsAFieldError() throws Exception {
        assertThat(bodyOf(register("yacine@eduflow.dz", "NOPE"), 400).at("/errors/level").asText()).isNotBlank();
        assertThat(users.findByEmail("yacine@eduflow.dz")).isEmpty();
    }

    @Test
    void aDeactivatedLevelDisappearsForTheRegistrationFormButNotForTheAdmin() throws Exception {
        long id = bodyOf(create("1AS", "1ʳᵉ année secondaire"), 201).get("id").asLong();

        bodyOf(patch("/api/v1/levels/" + id + "/deactivate", token(ADMIN)), 200);

        assertThat(bodyOf(get("/api/v1/levels", null), 200)).hasSize(6);
        assertThat(bodyOf(get("/api/v1/levels", token(ADMIN)), 200)).hasSize(7);
        assertThat(register("yacine@eduflow.dz", "1AS")).hasStatus(400);

        bodyOf(patch("/api/v1/levels/" + id + "/activate", token(ADMIN)), 200);
        assertThat(register("yacine@eduflow.dz", "1AS")).hasStatus(201);
    }

    @Test
    void renamingKeepsTheReference() throws Exception {
        long id = bodyOf(create("1AS", "1ʳᵉ année"), 201).get("id").asLong();

        JsonNode renamed = bodyOf(put("/api/v1/levels/" + id, token(ADMIN), """
                {"name":"1ʳᵉ année secondaire informatique","position":5}"""), 200);

        assertThat(renamed.get("code").asText()).isEqualTo("1AS");
        assertThat(renamed.get("name").asText()).isEqualTo("1ʳᵉ année secondaire informatique");
        assertThat(bodyOf(get("/api/v1/levels", null), 200).get(0).get("code").asText())
                .describedAs("position 5 sorts it before 7ᵉ année (10)").isEqualTo("1AS");
    }

    @Test
    void aReferenceCanOnlyBeUsedOnce() {
        assertThat(create("second_as", "Doublon")).hasStatus(409);
    }

    @Test
    void aReferenceWithSpacesOrAccentsIsRejected() throws Exception {
        assertThat(bodyOf(create("1ère AS", "1ʳᵉ année"), 400).at("/errors/code").asText()).isNotBlank();
    }

    @Test
    void onlyAnAdminManagesLevels() {
        assertThat(post("/api/v1/levels", token(STUDENT), """
                {"code":"1AS","name":"1ʳᵉ année"}""")).hasStatus(403);
        assertThat(post("/api/v1/levels", null, """
                {"code":"1AS","name":"1ʳᵉ année"}""")).hasStatus(401);
    }

    private MvcTestResult create(String code, String name) {
        return post("/api/v1/levels", token(ADMIN), """
                {"code":"%s","name":"%s"}""".formatted(code, name));
    }

    private MvcTestResult register(String email, String level) {
        return post("/api/v1/auth/register", null, """
                {"fullName":"Yacine Cherif","email":"%s","password":"%s","level":"%s"}"""
                .formatted(email, PASSWORD, level));
    }

    private String token(String email) {
        try {
            return bodyOf(post("/api/v1/auth/login", null, """
                    {"email":"%s","password":"%s"}""".formatted(email, PASSWORD)), 200)
                    .at("/tokens/accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("could not log in as " + email, e);
        }
    }

    private JsonNode bodyOf(MvcTestResult result, int expectedStatus) throws Exception {
        assertThat(result).hasStatus(expectedStatus);
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private MvcTestResult get(String uri, String token) {
        var request = mvc.get().uri(uri);
        return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
    }

    private MvcTestResult patch(String uri, String token) {
        return mvc.patch().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult put(String uri, String token, String body) {
        return mvc.put().uri(uri).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult post(String uri, String token, String body) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
    }
}
