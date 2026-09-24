package com.example.education_platform.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.access.entity.AccessRequestStatus;
import com.example.education_platform.access.repository.AccessRequestRepository;
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

/** The public "demander un accès" form, and the admin deciding on it. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessRequestApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String STUDENT = "amira@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AccessRequestRepository requests;

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

    // ---------- the public form ----------

    @Test
    void anybodyCanAskForAccessWithoutAnAccount() {
        assertThat(submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS"))
                .describedAs("the person asking has no account by definition").hasStatus(202);

        assertThat(requests.count()).isEqualTo(1);
        assertThat(requests.findAll().getFirst().getStatus()).isEqualTo(AccessRequestStatus.PENDING);
    }

    @Test
    void theEmailIsNormalisedBeforeItIsStored() {
        submit("Yacine Cherif", "YACINE@Eduflow.DZ", "THIRD_AS");

        assertThat(requests.findAll().getFirst().getEmail()).isEqualTo("yacine@eduflow.dz");
    }

    @Test
    void anAddressThatAlreadyHasAnAccountGetsTheSameAnswerAndNoRequest() {
        assertThat(submit("Amira Benali", STUDENT, "THIRD_AS"))
                .describedAs("a different answer would let anyone test which addresses exist")
                .hasStatus(202);

        assertThat(requests.count()).isZero();
    }

    @Test
    void askingTwiceWhileTheFirstIsPendingDoesNotQueueTwo() {
        submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS");
        assertThat(submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS")).hasStatus(202);

        assertThat(requests.count()).isEqualTo(1);
    }

    @Test
    void aFormMissingItsLevelIsRejectedInFrench() {
        assertThat(post("/api/v1/access-requests", null, """
                {"fullName":"Yacine","email":"yacine@eduflow.dz"}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.level")
                        .isEqualTo("Le niveau est obligatoire pour un élève"));
    }

    // ---------- the admin's queue ----------

    @Test
    void onlyAnAdminSeesTheQueue() throws Exception {
        submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS");

        assertThat(get("/api/v1/access-requests", token(STUDENT))).hasStatus(403);
        assertThat(get("/api/v1/access-requests", null)).hasStatus(401);
        assertThat(bodyOf(get("/api/v1/access-requests", token(ADMIN)), 200)
                .at("/totalElements").asLong()).isEqualTo(1);
    }

    @Test
    void theQueueCanBeNarrowedToWhatIsStillWaiting() throws Exception {
        submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS");
        long id = firstRequestId();
        reject(id, "Pas de place cette année");

        assertThat(bodyOf(get("/api/v1/access-requests?status=PENDING", token(ADMIN)), 200)
                .at("/totalElements").asLong()).isZero();
        assertThat(bodyOf(get("/api/v1/access-requests?status=REJECTED", token(ADMIN)), 200)
                .at("/totalElements").asLong()).isEqualTo(1);
    }

    // ---------- deciding ----------

    @Test
    void approvingCreatesTheAccountAndLetsThemLogInStraightAway() throws Exception {
        submit("Yacine Cherif", "yacine@eduflow.dz", "SECOND_AS");
        long id = firstRequestId();

        JsonNode approved = bodyOf(post("/api/v1/access-requests/" + id + "/approve",
                token(ADMIN), null), 200);

        assertThat(approved.at("/request/status").asText()).isEqualTo("APPROVED");
        assertThat(approved.at("/request/createdUser/email").asText()).isEqualTo("yacine@eduflow.dz");
        assertThat(approved.at("/request/createdUser/level").asText()).isEqualTo("SECOND_AS");
        assertThat(approved.at("/request/reviewedBy/email").asText()).isEqualTo(ADMIN);

        String temporaryPassword = approved.at("/temporaryPassword").asText();
        assertThat(login("yacine@eduflow.dz", temporaryPassword))
                .describedAs("the account works immediately").hasStatusOk();
    }

    @Test
    void rejectingRecordsTheReasonAndCreatesNoAccount() throws Exception {
        submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS");
        long id = firstRequestId();

        JsonNode rejected = bodyOf(reject(id, "Pas de place cette année"), 200);

        assertThat(rejected.at("/status").asText()).isEqualTo("REJECTED");
        assertThat(rejected.at("/decisionNote").asText()).isEqualTo("Pas de place cette année");
        assertThat(rejected.at("/createdUser").isNull()).isTrue();
        assertThat(users.existsByEmail("yacine@eduflow.dz")).isFalse();
    }

    @Test
    void aDecidedRequestCannotBeDecidedAgain() throws Exception {
        submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS");
        long id = firstRequestId();
        reject(id, "Non");

        assertThat(post("/api/v1/access-requests/" + id + "/approve", token(ADMIN), null))
                .hasStatus(422);
        assertThat(reject(id, "Encore non")).hasStatus(422);
    }

    @Test
    void refusingSomebodyDoesNotStopThemAskingAgain() throws Exception {
        submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS");
        reject(firstRequestId(), "Pas cette année");

        assertThat(submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS")).hasStatus(202);
        assertThat(requests.count())
                .describedAs("a refusal is not a ban").isEqualTo(2);
    }

    @Test
    void aStudentCannotDecideOnRequests() throws Exception {
        submit("Yacine Cherif", "yacine@eduflow.dz", "THIRD_AS");
        long id = firstRequestId();

        assertThat(post("/api/v1/access-requests/" + id + "/approve", token(STUDENT), null))
                .hasStatus(403);
    }

    // ---------- helpers ----------

    private long firstRequestId() {
        return requests.findAll().getFirst().getId();
    }

    private MvcTestResult submit(String fullName, String email, String level) {
        return post("/api/v1/access-requests", null, """
                {"fullName":"%s","email":"%s","level":"%s","message":"Je souhaite m'inscrire"}"""
                .formatted(fullName, email, level));
    }

    private MvcTestResult reject(long id, String note) {
        return post("/api/v1/access-requests/" + id + "/reject", token(ADMIN), """
                {"note":"%s"}""".formatted(note));
    }

    private MvcTestResult login(String email, String password) {
        return post("/api/v1/auth/login", null, """
                {"email":"%s","password":"%s"}""".formatted(email, password));
    }

    private String token(String email) {
        try {
            return bodyOf(login(email, PASSWORD), 200).at("/tokens/accessToken").asText();
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

    private MvcTestResult post(String uri, String token, String body) {
        var request = mvc.post().uri(uri);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return (token == null ? request : request.header("Authorization", "Bearer " + token)).exchange();
    }
}
