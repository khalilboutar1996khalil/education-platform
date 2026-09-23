package com.example.education_platform.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.course.entity.Chapter;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.entity.Lesson;
import com.example.education_platform.course.entity.LessonCompletion;
import com.example.education_platform.course.entity.LessonType;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.course.repository.LessonCompletionRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CourseApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String STUDENT = "amira@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private LessonCompletionRepository completions;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long thirdAsCourseId;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        User amira = users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));

        Course algo = new Course("INF201", "Algorithmique", "Bases", Level.THIRD_AS, "#16A34A");
        Chapter intro = algo.addChapter(new Chapter("Introduction", "Les bases"), null);
        intro.addLesson(new Lesson("Variables", LessonType.VIDEO, 12), null);
        intro.addLesson(new Lesson("Exercices", LessonType.PDF, null), null);
        thirdAsCourseId = courses.saveAndFlush(algo).getId();

        courses.saveAndFlush(new Course("INF101", "Initiation", null, Level.SECOND_AS, "#2563EB"));

        // One of the two lessons finished, so progress is a round 50 %
        completions.saveAndFlush(new LessonCompletion(amira,
                courses.findDetailById(thirdAsCourseId).orElseThrow()
                        .getChapters().getFirst().getLessons().getFirst()));
    }

    // ---------- visibility ----------

    @Test
    void aStudentOnlySeesTheirOwnLevel() throws Exception {
        JsonNode page = bodyOf(get("/api/v1/courses", studentToken()), 200);

        assertThat(page.at("/totalElements").asLong()).isEqualTo(1);
        assertThat(page.at("/content/0/code").asText()).isEqualTo("INF201");
    }

    @Test
    void aStudentCannotWidenTheirViewWithTheLevelParameter() throws Exception {
        JsonNode page = bodyOf(get("/api/v1/courses?level=SECOND_AS", studentToken()), 200);

        assertThat(page.at("/totalElements").asLong())
                .describedAs("the filter must not override the student's own level").isEqualTo(1);
        assertThat(page.at("/content/0/code").asText()).isEqualTo("INF201");
    }

    @Test
    void anAdminSeesEveryLevelAndCanFilter() throws Exception {
        assertThat(bodyOf(get("/api/v1/courses", adminToken()), 200).at("/totalElements").asLong())
                .isEqualTo(2);
        assertThat(bodyOf(get("/api/v1/courses?level=SECOND_AS", adminToken()), 200)
                .at("/content/0/code").asText()).isEqualTo("INF101");
    }

    @Test
    void aStudentIsRefusedAModuleOfAnotherLevel() {
        Long secondAs = courses.findAll().stream()
                .filter(c -> c.getLevel() == Level.SECOND_AS).findFirst().orElseThrow().getId();

        assertThat(get("/api/v1/courses/" + secondAs, studentToken())).hasStatus(403);
        assertThat(get("/api/v1/courses/" + secondAs, adminToken())).hasStatusOk();
    }

    // ---------- progress ----------

    @Test
    void aStudentSeesTheirOwnProgressAndNotTheClassAverage() throws Exception {
        JsonNode card = bodyOf(get("/api/v1/courses", studentToken()), 200).at("/content/0");

        assertThat(card.at("/chapterCount").asLong()).isEqualTo(1);
        assertThat(card.at("/lessonCount").asLong()).isEqualTo(2);
        assertThat(card.at("/progressPercent").asInt()).isEqualTo(50);
        assertThat(card.at("/classAveragePercent").isNull()).isTrue();
    }

    @Test
    void anAdminSeesTheClassAverageAndNoPersonalProgress() throws Exception {
        JsonNode card = bodyOf(get("/api/v1/courses?level=THIRD_AS", adminToken()), 200).at("/content/0");

        // one completion, two lessons, one active student at this level
        assertThat(card.at("/classAveragePercent").asInt()).isEqualTo(50);
        assertThat(card.at("/progressPercent").isNull()).isTrue();
    }

    @Test
    void aModuleWithNoLessonsReportsZeroRatherThanFailing() throws Exception {
        courses.saveAndFlush(new Course("INF999", "Vide", null, Level.THIRD_AS, null));

        JsonNode page = bodyOf(get("/api/v1/courses", studentToken()), 200);
        JsonNode empty = page.at("/content/1");

        assertThat(empty.at("/code").asText()).isEqualTo("INF999");
        assertThat(empty.at("/lessonCount").asLong()).isZero();
        assertThat(empty.at("/progressPercent").asInt()).isZero();
    }

    // ---------- detail ----------

    @Test
    void theDetailCarriesTheTreeAndFlagsWhatTheStudentFinished() throws Exception {
        JsonNode detail = bodyOf(get("/api/v1/courses/" + thirdAsCourseId, studentToken()), 200);

        assertThat(detail.at("/code").asText()).isEqualTo("INF201");
        assertThat(detail.at("/chapters").size()).isEqualTo(1);

        JsonNode lessons = detail.at("/chapters/0/lessons");
        assertThat(lessons.size()).isEqualTo(2);
        assertThat(lessons.at("/0/title").asText()).isEqualTo("Variables");
        assertThat(lessons.at("/0/completed").asBoolean()).isTrue();
        assertThat(lessons.at("/1/completed").asBoolean()).isFalse();
        assertThat(lessons.at("/0/position").asInt()).isEqualTo(1);
    }

    @Test
    void anUnknownModuleIsNotFound() {
        assertThat(get("/api/v1/courses/999999", adminToken())).hasStatus(404);
    }

    // ---------- writing ----------

    @Test
    void anAdminCreatesUpdatesAndDeletesAModule() throws Exception {
        JsonNode created = bodyOf(post("/api/v1/courses", adminToken(), """
                {"code":"INF301","title":"Réseaux","description":"TCP/IP","level":"THIRD_AS","color":"#DC2626"}"""),
                201);
        Long id = created.at("/id").asLong();
        assertThat(created.at("/chapters").size()).isZero();

        JsonNode updated = bodyOf(put("/api/v1/courses/" + id, adminToken(), """
                {"code":"INF301","title":"Réseaux avancés","description":"TCP/IP",
                 "level":"THIRD_AS","color":"#DC2626"}"""), 200);
        assertThat(updated.at("/title").asText()).isEqualTo("Réseaux avancés");

        assertThat(delete("/api/v1/courses/" + id, adminToken())).hasStatus(204);
        assertThat(get("/api/v1/courses/" + id, adminToken())).hasStatus(404);
    }

    @Test
    void aDuplicateCodeConflicts() {
        assertThat(post("/api/v1/courses", adminToken(), """
                {"code":"INF201","title":"Doublon","level":"THIRD_AS"}""")).hasStatus(409);
    }

    @Test
    void aMalformedCodeIsRejectedInFrench() {
        assertThat(post("/api/v1/courses", adminToken(), """
                {"code":"inf 301","title":"Réseaux","level":"THIRD_AS"}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.code")
                        .isEqualTo("Le code ne peut contenir que des majuscules, des chiffres et des tirets"));
    }

    @Test
    void aStudentCannotWriteModules() {
        assertThat(post("/api/v1/courses", studentToken(), """
                {"code":"INF777","title":"Nope","level":"THIRD_AS"}""")).hasStatus(403);
        assertThat(delete("/api/v1/courses/" + thirdAsCourseId, studentToken())).hasStatus(403);
    }

    @Test
    void modulesRequireAToken() {
        assertThat(mvc.get().uri("/api/v1/courses")).hasStatus(401);
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
