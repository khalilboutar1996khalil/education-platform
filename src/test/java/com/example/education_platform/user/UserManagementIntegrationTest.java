package com.example.education_platform.user;

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

/** Admin student management and the /me profile, driven through real JWTs from the login endpoint. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserManagementIntegrationTest {

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

    private Long studentId;

    @BeforeEach
    void seedAccounts() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        studentId = users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS)).getId();
    }

    // ---------- inviting ----------

    @Test
    void adminInvitesAStudentWhoCanThenLogIn() throws Exception {
        JsonNode invited = bodyOf(post("/api/v1/users", adminToken(), """
                {"fullName":"Yacine Cherif","email":"YACINE@eduflow.dz","level":"SECOND_AS"}"""), 201);

        assertThat(invited.at("/user/email").asText())
                .describedAs("email is normalised to lower case").isEqualTo("yacine@eduflow.dz");
        assertThat(invited.at("/user/role").asText()).isEqualTo("STUDENT");
        assertThat(invited.at("/user/level").asText()).isEqualTo("SECOND_AS");

        String temporaryPassword = invited.at("/temporaryPassword").asText();
        assertThat(temporaryPassword).isNotBlank();
        assertThat(login("yacine@eduflow.dz", temporaryPassword)).hasStatusOk();
    }

    @Test
    void invitingAnExistingEmailConflicts() {
        assertThat(post("/api/v1/users", adminToken(), """
                {"fullName":"Doublon","email":"%s","level":"THIRD_AS"}""".formatted(STUDENT)))
                .hasStatus(409);
    }

    @Test
    void aStudentWithoutALevelIsRejectedInFrench() {
        assertThat(post("/api/v1/users", adminToken(), """
                {"fullName":"Sans Niveau","email":"x@eduflow.dz","level":null}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.level")
                        .isEqualTo("Le niveau est obligatoire pour un élève"));
    }

    @Test
    void aStudentCannotInvite() {
        assertThat(post("/api/v1/users", studentToken(), """
                {"fullName":"Nope","email":"nope@eduflow.dz","level":"THIRD_AS"}"""))
                .hasStatus(403);
    }

    // ---------- listing ----------

    @Test
    void adminListsUsersInThePageResponseShape() throws Exception {
        JsonNode page = bodyOf(get("/api/v1/users?size=1&page=0", adminToken()), 200);

        assertThat(page.at("/content").isArray()).isTrue();
        assertThat(page.at("/content").size()).isEqualTo(1);
        assertThat(page.at("/page").asInt()).isZero();
        assertThat(page.at("/size").asInt()).isEqualTo(1);
        assertThat(page.at("/totalElements").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(page.at("/last").isBoolean()).isTrue();
    }

    @Test
    void filtersNarrowTheListAndCombine() throws Exception {
        assertThat(bodyOf(get("/api/v1/users?role=ADMIN", adminToken()), 200).at("/totalElements").asLong())
                .isEqualTo(1);
        assertThat(bodyOf(get("/api/v1/users?role=STUDENT&level=THIRD_AS", adminToken()), 200)
                .at("/totalElements").asLong()).isEqualTo(1);
        assertThat(bodyOf(get("/api/v1/users?role=STUDENT&level=SECOND_AS", adminToken()), 200)
                .at("/totalElements").asLong()).isZero();
        assertThat(bodyOf(get("/api/v1/users?search=BENALI", adminToken()), 200).at("/totalElements").asLong())
                .describedAs("search is case-insensitive over name and email").isEqualTo(1);
    }

    @Test
    void aStudentCannotListUsers() {
        assertThat(get("/api/v1/users", studentToken())).hasStatus(403);
    }

    // ---------- ownership ----------

    @Test
    void aStudentReadsTheirOwnRecordButNotSomebodyElses() throws Exception {
        Long adminId = users.findByEmail(ADMIN).orElseThrow().getId();

        assertThat(bodyOf(get("/api/v1/users/" + studentId, studentToken()), 200).at("/email").asText())
                .isEqualTo(STUDENT);
        assertThat(get("/api/v1/users/" + adminId, studentToken())).hasStatus(403);
        assertThat(get("/api/v1/users/" + studentId, adminToken()))
                .describedAs("an admin may read anyone").hasStatusOk();
    }

    @Test
    void readingAnUnknownUserIsNotFound() {
        assertThat(get("/api/v1/users/999999", adminToken())).hasStatus(404);
    }

    // ---------- status ----------

    @Test
    void disablingAUserRevokesTheirSessions() throws Exception {
        String refreshToken = bodyOf(login(STUDENT, PASSWORD), 200).at("/tokens/refreshToken").asText();

        assertThat(patch("/api/v1/users/" + studentId + "/status", adminToken(), """
                {"status":"DISABLED"}""")).hasStatusOk();

        assertThat(post("/api/v1/auth/refresh", null, """
                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .describedAs("a disabled account must not be able to refresh").hasStatus(401);
        assertThat(login(STUDENT, PASSWORD)).hasStatus(401);
    }

    // ---------- password reset ----------

    @Test
    void adminResetsAStudentPasswordAndOldSessionsEnd() throws Exception {
        String refreshToken = bodyOf(login(STUDENT, PASSWORD), 200).at("/tokens/refreshToken").asText();

        JsonNode reset = bodyOf(post("/api/v1/users/" + studentId + "/reset-password", adminToken(), "{}"), 200);
        String temporaryPassword = reset.at("/temporaryPassword").asText();

        assertThat(reset.at("/user/email").asText()).isEqualTo(STUDENT);
        assertThat(temporaryPassword).isNotBlank();
        assertThat(login(STUDENT, PASSWORD)).describedAs("the old password stops working").hasStatus(401);
        assertThat(login(STUDENT, temporaryPassword)).hasStatusOk();
        assertThat(post("/api/v1/auth/refresh", null, """
                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .describedAs("sessions opened with the old password are revoked").hasStatus(401);
    }

    @Test
    void anAdminPasswordCannotBeResetThisWay() {
        Long adminId = users.findByEmail(ADMIN).orElseThrow().getId();
        assertThat(post("/api/v1/users/" + adminId + "/reset-password", adminToken(), "{}")).hasStatus(422);
    }

    @Test
    void aStudentCannotResetPasswords() {
        assertThat(post("/api/v1/users/" + studentId + "/reset-password", studentToken(), "{}")).hasStatus(403);
    }

    // ---------- profile ----------

    @Test
    void meReturnsTheTokenOwnerAndUpdatesApply() throws Exception {
        assertThat(bodyOf(get("/api/v1/me", studentToken()), 200).at("/email").asText()).isEqualTo(STUDENT);

        JsonNode updated = bodyOf(patch("/api/v1/me", studentToken(), """
                {"fullName":"Amira B. Benali","locale":"en","notifyByEmail":false}"""), 200);

        assertThat(updated.at("/fullName").asText()).isEqualTo("Amira B. Benali");
        assertThat(updated.at("/initials").asText()).isEqualTo("AB");
        assertThat(updated.at("/email").asText())
                .describedAs("a profile update must not let the user change their email").isEqualTo(STUDENT);
    }

    @Test
    void meRequiresAToken() {
        assertThat(mvc.get().uri("/api/v1/me")).hasStatus(401);
    }

    @Test
    void changingThePasswordNeedsTheCurrentOneAndEndsOtherSessions() throws Exception {
        String refreshToken = bodyOf(login(STUDENT, PASSWORD), 200).at("/tokens/refreshToken").asText();
        String token = studentToken();

        assertThat(post("/api/v1/me/password", token, """
                {"currentPassword":"not-my-password","newPassword":"Brand-New-Secret-1"}"""))
                .hasStatus(401);

        assertThat(post("/api/v1/me/password", token, """
                {"currentPassword":"%s","newPassword":"Brand-New-Secret-1"}""".formatted(PASSWORD)))
                .hasStatus(204);

        assertThat(login(STUDENT, "Brand-New-Secret-1")).hasStatusOk();
        assertThat(login(STUDENT, PASSWORD)).hasStatus(401);
        assertThat(post("/api/v1/auth/refresh", null, """
                {"refreshToken":"%s"}""".formatted(refreshToken)))
                .describedAs("changing a password must end sessions opened with the old one")
                .hasStatus(401);
    }

    @Test
    void aShortNewPasswordIsRejectedInFrench() {
        assertThat(post("/api/v1/me/password", studentToken(), """
                {"currentPassword":"%s","newPassword":"short"}""".formatted(PASSWORD)))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.newPassword")
                        .isEqualTo("Le mot de passe doit contenir au moins 10 caractères"));
    }

    // ---------- helpers ----------

    private String adminToken() {
        return tokenFor(ADMIN);
    }

    private String studentToken() {
        return tokenFor(STUDENT);
    }

    private String tokenFor(String email) {
        try {
            return bodyOf(login(email, PASSWORD), 200).at("/tokens/accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("could not log in as " + email, e);
        }
    }

    private MvcTestResult login(String email, String password) {
        return post("/api/v1/auth/login", null, """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    private JsonNode bodyOf(MvcTestResult result, int expectedStatus) throws Exception {
        assertThat(result).hasStatus(expectedStatus);
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private MvcTestResult get(String uri, String token) {
        return mvc.get().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult post(String uri, String token, String body) {
        var request = mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
        return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
    }

    private MvcTestResult patch(String uri, String token, String body) {
        return mvc.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }
}
