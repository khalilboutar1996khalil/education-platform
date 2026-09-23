package com.example.education_platform.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.course.entity.Chapter;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.entity.Lesson;
import com.example.education_platform.course.entity.LessonType;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
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

/** Chapter and lesson editing, the position renumbering behind it, and marking lessons done. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CurriculumApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String STUDENT = "amira@eduflow.dz";
    private static final String OTHER_LEVEL_STUDENT = "yacine@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long courseId;
    private Long chapterId;
    private Long firstLessonId;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        users.saveAndFlush(new User("Yacine Cherif", OTHER_LEVEL_STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.SECOND_AS));

        Course algo = new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A");
        Chapter intro = algo.addChapter(new Chapter("Introduction", null), null);
        intro.addLesson(new Lesson("Variables", LessonType.VIDEO, 12), null);
        intro.addLesson(new Lesson("Exercices", LessonType.PDF, null), null);
        Course saved = courses.saveAndFlush(algo);

        courseId = saved.getId();
        chapterId = saved.getChapters().getFirst().getId();
        firstLessonId = saved.getChapters().getFirst().getLessons().getFirst().getId();
    }

    // ---------- chapters ----------

    @Test
    void addingAChapterWithoutAPositionAppends() throws Exception {
        JsonNode course = bodyOf(post("/api/v1/courses/" + courseId + "/chapters", adminToken(), """
                {"title":"Boucles"}"""), 201);

        assertThat(titles(course.at("/chapters"))).containsExactly("Introduction", "Boucles");
        assertThat(positions(course.at("/chapters"))).containsExactly(1, 2);
        assertThat(course.at("/chapters/1/id").isNull())
                .describedAs("the caller needs the new chapter's generated id").isFalse();
    }

    @Test
    void insertingAChapterShiftsTheOthersAndKeepsPositionsContiguous() throws Exception {
        post("/api/v1/courses/" + courseId + "/chapters", adminToken(), """
                {"title":"Boucles"}""");

        JsonNode course = bodyOf(post("/api/v1/courses/" + courseId + "/chapters", adminToken(), """
                {"title":"Prérequis","position":1}"""), 201);

        assertThat(titles(course.at("/chapters"))).containsExactly("Prérequis", "Introduction", "Boucles");
        assertThat(positions(course.at("/chapters"))).containsExactly(1, 2, 3);
    }

    @Test
    void movingAChapterRenumbersTheSiblings() throws Exception {
        post("/api/v1/courses/" + courseId + "/chapters", adminToken(), """
                {"title":"Boucles"}""");
        post("/api/v1/courses/" + courseId + "/chapters", adminToken(), """
                {"title":"Tableaux"}""");

        JsonNode course = bodyOf(patch("/api/v1/chapters/" + chapterId + "/position", adminToken(), """
                {"position":3}"""), 200);

        assertThat(titles(course.at("/chapters"))).containsExactly("Boucles", "Tableaux", "Introduction");
        assertThat(positions(course.at("/chapters"))).containsExactly(1, 2, 3);
    }

    @Test
    void deletingAChapterClosesTheGapAndTakesItsLessons() throws Exception {
        post("/api/v1/courses/" + courseId + "/chapters", adminToken(), """
                {"title":"Boucles"}""");

        assertThat(delete("/api/v1/chapters/" + chapterId, adminToken())).hasStatus(204);

        JsonNode course = bodyOf(get("/api/v1/courses/" + courseId, adminToken()), 200);
        assertThat(titles(course.at("/chapters"))).containsExactly("Boucles");
        assertThat(positions(course.at("/chapters"))).containsExactly(1);
    }

    @Test
    void updatingAChapterCanAlsoMoveIt() throws Exception {
        post("/api/v1/courses/" + courseId + "/chapters", adminToken(), """
                {"title":"Boucles"}""");

        JsonNode course = bodyOf(put("/api/v1/chapters/" + chapterId, adminToken(), """
                {"title":"Introduction révisée","summary":"Mise à jour","position":2}"""), 200);

        assertThat(titles(course.at("/chapters"))).containsExactly("Boucles", "Introduction révisée");
    }

    // ---------- lessons ----------

    @Test
    void addingALessonAtAPositionShiftsTheRest() throws Exception {
        JsonNode chapter = bodyOf(post("/api/v1/chapters/" + chapterId + "/lessons", adminToken(), """
                {"title":"Quiz d'entrée","type":"QUIZ","position":1}"""), 201);

        assertThat(titles(chapter.at("/lessons")))
                .containsExactly("Quiz d'entrée", "Variables", "Exercices");
        assertThat(positions(chapter.at("/lessons"))).containsExactly(1, 2, 3);
        assertThat(chapter.at("/lessons/0/id").isNull())
                .describedAs("the caller needs the new lesson's generated id").isFalse();
        assertThat(chapter.at("/lessons/0/completed").asBoolean()).isFalse();
    }

    @Test
    void movingALessonRenumbersItsChapter() throws Exception {
        JsonNode chapter = bodyOf(patch("/api/v1/lessons/" + firstLessonId + "/position", adminToken(), """
                {"position":2}"""), 200);

        assertThat(titles(chapter.at("/lessons"))).containsExactly("Exercices", "Variables");
        assertThat(positions(chapter.at("/lessons"))).containsExactly(1, 2);
    }

    @Test
    void deletingALessonClosesTheGap() throws Exception {
        assertThat(delete("/api/v1/lessons/" + firstLessonId, adminToken())).hasStatus(204);

        JsonNode course = bodyOf(get("/api/v1/courses/" + courseId, adminToken()), 200);
        JsonNode lessons = course.at("/chapters/0/lessons");
        assertThat(titles(lessons)).containsExactly("Exercices");
        assertThat(positions(lessons)).containsExactly(1);
    }

    @Test
    void aLessonWithoutATypeIsRejectedInFrench() {
        assertThat(post("/api/v1/chapters/" + chapterId + "/lessons", adminToken(), """
                {"title":"Sans type"}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.type")
                        .isEqualTo("Le type de leçon est obligatoire"));
    }

    @Test
    void aStudentCannotEditTheCurriculum() {
        assertThat(post("/api/v1/courses/" + courseId + "/chapters", studentToken(), """
                {"title":"Nope"}""")).hasStatus(403);
        assertThat(delete("/api/v1/lessons/" + firstLessonId, studentToken())).hasStatus(403);
    }

    @Test
    void editingAnUnknownChapterIsNotFound() {
        assertThat(patch("/api/v1/chapters/999999/position", adminToken(), """
                {"position":1}""")).hasStatus(404);
    }

    // ---------- progress ----------

    @Test
    void markingALessonDoneShowsUpInTheDetailAndTheProgress() throws Exception {
        assertThat(put("/api/v1/lessons/" + firstLessonId + "/completion", studentToken(), null))
                .hasStatus(204);

        JsonNode detail = bodyOf(get("/api/v1/courses/" + courseId, studentToken()), 200);
        assertThat(detail.at("/chapters/0/lessons/0/completed").asBoolean()).isTrue();
        assertThat(detail.at("/chapters/0/lessons/1/completed").asBoolean()).isFalse();

        JsonNode card = bodyOf(get("/api/v1/courses", studentToken()), 200).at("/content/0");
        assertThat(card.at("/progressPercent").asInt()).isEqualTo(50);
    }

    @Test
    void markingTheSameLessonTwiceIsIdempotent() throws Exception {
        assertThat(put("/api/v1/lessons/" + firstLessonId + "/completion", studentToken(), null))
                .hasStatus(204);
        assertThat(put("/api/v1/lessons/" + firstLessonId + "/completion", studentToken(), null))
                .describedAs("a second mark must not blow up on the unique constraint").hasStatus(204);

        assertThat(bodyOf(get("/api/v1/courses", studentToken()), 200)
                .at("/content/0/progressPercent").asInt()).isEqualTo(50);
    }

    @Test
    void unmarkingALessonTakesTheProgressBack() throws Exception {
        put("/api/v1/lessons/" + firstLessonId + "/completion", studentToken(), null);

        assertThat(delete("/api/v1/lessons/" + firstLessonId + "/completion", studentToken()))
                .hasStatus(204);
        assertThat(bodyOf(get("/api/v1/courses", studentToken()), 200)
                .at("/content/0/progressPercent").asInt()).isZero();
    }

    @Test
    void unmarkingSomethingNeverMarkedIsStillNoContent() {
        assertThat(delete("/api/v1/lessons/" + firstLessonId + "/completion", studentToken()))
                .hasStatus(204);
    }

    @Test
    void aStudentCannotMarkALessonOfAnotherLevel() {
        assertThat(put("/api/v1/lessons/" + firstLessonId + "/completion", otherLevelToken(), null))
                .hasStatus(403);
    }

    @Test
    void anAdminHasNoProgressToMark() {
        assertThat(put("/api/v1/lessons/" + firstLessonId + "/completion", adminToken(), null))
                .hasStatus(403);
    }

    // ---------- helpers ----------

    /** Direct children only — findValues() would recurse into each chapter's lessons. */
    private static List<String> titles(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.get("title").asText()));
        return values;
    }

    private static List<Integer> positions(JsonNode array) {
        List<Integer> values = new ArrayList<>();
        array.forEach(node -> values.add(node.get("position").asInt()));
        return values;
    }

    private String adminToken() {
        return tokenFor(ADMIN);
    }

    private String studentToken() {
        return tokenFor(STUDENT);
    }

    private String otherLevelToken() {
        return tokenFor(OTHER_LEVEL_STUDENT);
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
        var request = mvc.put().uri(uri).header("Authorization", "Bearer " + token);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return request.exchange();
    }

    private MvcTestResult patch(String uri, String token, String body) {
        return mvc.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult delete(String uri, String token) {
        return mvc.delete().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }
}
