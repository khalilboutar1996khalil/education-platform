package com.example.education_platform.announcement;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
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

/** Who a notice reaches, and the audience count frozen when it goes out. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnnouncementApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String THIRD_AS = "amira@eduflow.dz";
    private static final String SECOND_AS = "sofiane@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long thirdAsCourseId;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", THIRD_AS,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        users.saveAndFlush(new User("Yacine Cherif", "yacine@eduflow.dz",
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        users.saveAndFlush(new User("Sofiane Meziane", SECOND_AS,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.SECOND_AS));

        thirdAsCourseId = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A")).getId();
    }

    // ---------- drafts ----------

    @Test
    void aNewNoticeIsADraftNobodyElseCanSee() throws Exception {
        long id = create("""
                {"title":"Rentrée","body":"Les cours reprennent lundi"}""");

        assertThat(bodyOf(get("/api/v1/announcements/" + id, token(ADMIN)), 200)
                .at("/publishedAt").isNull()).isTrue();
        assertThat(count(THIRD_AS)).describedAs("drafts stay with their author").isZero();
        assertThat(get("/api/v1/announcements/" + id, token(THIRD_AS))).hasStatus(403);
    }

    @Test
    void aDraftCanBeEditedButAPublishedNoticeCannot() throws Exception {
        long id = create("""
                {"title":"Brouillon","body":"À corriger"}""");

        assertThat(bodyOf(put("/api/v1/announcements/" + id, token(ADMIN), """
                {"title":"Corrigé","body":"Version finale"}"""), 200)
                .at("/title").asText()).isEqualTo("Corrigé");

        post("/api/v1/announcements/" + id + "/publish", token(ADMIN));
        assertThat(put("/api/v1/announcements/" + id, token(ADMIN), """
                {"title":"Trop tard","body":"Déjà lu"}"""))
                .describedAs("rewriting what people read would rewrite history").hasStatus(422);
    }

    // ---------- scope ----------

    @Test
    void aSectionWideNoticeReachesEveryLevel() throws Exception {
        publish("""
                {"title":"Rentrée","body":"Les cours reprennent lundi"}""");

        assertThat(count(THIRD_AS)).isEqualTo(1);
        assertThat(count(SECOND_AS)).isEqualTo(1);
    }

    @Test
    void aModuleScopedNoticeOnlyReachesThatModulesLevel() throws Exception {
        publish("""
                {"title":"TP reporté","body":"Le TP passe à jeudi","courseId":%d}"""
                .formatted(thirdAsCourseId));

        assertThat(count(THIRD_AS)).isEqualTo(1);
        assertThat(count(SECOND_AS)).isZero();
    }

    @Test
    void aLevelScopedNoticeOnlyReachesThatLevel() throws Exception {
        publish("""
                {"title":"Réunion 2AS","body":"Mardi à 10h","level":"SECOND_AS"}""");

        assertThat(count(SECOND_AS)).isEqualTo(1);
        assertThat(count(THIRD_AS)).isZero();
    }

    // ---------- the audience count ----------

    @Test
    void publishingFreezesHowManyStudentsItReached() throws Exception {
        long id = publish("""
                {"title":"Rentrée","body":"Lundi"}""");

        assertThat(bodyOf(get("/api/v1/announcements/" + id, token(ADMIN)), 200)
                .at("/recipientCount").asInt())
                .describedAs("three active students across both levels").isEqualTo(3);
    }

    @Test
    void aModuleScopedNoticeCountsOnlyThatLevel() throws Exception {
        long id = publish("""
                {"title":"TP reporté","body":"Jeudi","courseId":%d}""".formatted(thirdAsCourseId));

        assertThat(bodyOf(get("/api/v1/announcements/" + id, token(ADMIN)), 200)
                .at("/recipientCount").asInt())
                .describedAs("two students in 3AS").isEqualTo(2);
    }

    @Test
    void theCountIgnoresDisabledAccountsAndDoesNotMoveAfterwards() throws Exception {
        long id = publish("""
                {"title":"Rentrée","body":"Lundi"}""");
        assertThat(bodyOf(get("/api/v1/announcements/" + id, token(ADMIN)), 200)
                .at("/recipientCount").asInt()).isEqualTo(3);

        User leaver = users.findByEmail("yacine@eduflow.dz").orElseThrow();
        leaver.setStatus(UserStatus.DISABLED);
        users.saveAndFlush(leaver);

        assertThat(bodyOf(get("/api/v1/announcements/" + id, token(ADMIN)), 200)
                .at("/recipientCount").asInt())
                .describedAs("the audience moved on; the fact of the send did not").isEqualTo(3);
    }

    @Test
    void publishingTwiceIsRefused() throws Exception {
        long id = publish("""
                {"title":"Rentrée","body":"Lundi"}""");

        assertThat(post("/api/v1/announcements/" + id + "/publish", token(ADMIN))).hasStatus(422);
    }

    // ---------- access ----------

    @Test
    void aStudentCannotWriteOrPublishNotices() throws Exception {
        long id = create("""
                {"title":"Rentrée","body":"Lundi"}""");

        assertThat(post("/api/v1/announcements", token(THIRD_AS), """
                {"title":"Faux","body":"Pas moi"}""")).hasStatus(403);
        assertThat(post("/api/v1/announcements/" + id + "/publish", token(THIRD_AS))).hasStatus(403);
        assertThat(delete("/api/v1/announcements/" + id, token(THIRD_AS))).hasStatus(403);
    }

    @Test
    void aMissingBodyIsRejectedInFrench() {
        assertThat(post("/api/v1/announcements", token(ADMIN), """
                {"title":"Sans contenu"}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(b -> b.assertThat().extractingPath("$.errors.body")
                        .isEqualTo("Le contenu de l'annonce est obligatoire"));
    }

    // ---------- helpers ----------

    private long create(String body) throws Exception {
        return bodyOf(post("/api/v1/announcements", token(ADMIN), body), 201).at("/id").asLong();
    }

    private long publish(String body) throws Exception {
        long id = create(body);
        post("/api/v1/announcements/" + id + "/publish", token(ADMIN));
        return id;
    }

    private long count(String email) throws Exception {
        return bodyOf(get("/api/v1/announcements", token(email)), 200).at("/totalElements").asLong();
    }

    private String token(String email) {
        try {
            MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"%s","password":"%s"}""".formatted(email, PASSWORD))
                    .exchange();
            return bodyOf(result, 200).at("/tokens/accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("could not log in as " + email, e);
        }
    }

    private JsonNode bodyOf(MvcTestResult result, int expectedStatus) throws Exception {
        assertThat(result).hasStatus(expectedStatus);
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private MvcTestResult get(String uri, String token) {
        return mvc.get().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult post(String uri, String token) {
        return mvc.post().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult post(String uri, String token, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult put(String uri, String token, String body) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult delete(String uri, String token) {
        return mvc.delete().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }
}
