package com.example.education_platform.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.WorkMode;
import com.example.education_platform.assignment.repository.AssignmentRepository;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String AMIRA = "amira@eduflow.dz";
    private static final String YACINE = "yacine@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private LessonCompletionRepository completions;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void seed() {
        // The cache outlives a rolled-back test, so it has to start empty every time
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());

        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        User amira = users.saveAndFlush(new User("Amira Benali", AMIRA,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        users.saveAndFlush(new User("Yacine Cherif", YACINE,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));

        Course course = new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A");
        Chapter chapter = course.addChapter(new Chapter("Introduction", null), null);
        chapter.addLesson(new Lesson("Variables", LessonType.VIDEO, 12), null);
        chapter.addLesson(new Lesson("Boucles", LessonType.VIDEO, 15), null);
        chapter.addLesson(new Lesson("Tableaux", LessonType.VIDEO, 20), null);
        chapter.addLesson(new Lesson("Exercices", LessonType.PDF, null), null);
        Course saved = courses.saveAndFlush(course);

        // Amira has finished one of the four lessons
        completions.saveAndFlush(new LessonCompletion(amira,
                saved.getChapters().getFirst().getLessons().getFirst()));

        Assignment tp = new Assignment(saved, "TP n°1", AssignmentType.TP, WorkMode.INDIVIDUAL);
        tp.setStatus(AssignmentStatus.OPEN);
        tp.setDeadline(Instant.now().plus(3, ChronoUnit.DAYS));
        tp.setMaxPoints(new BigDecimal("20"));
        assignments.saveAndFlush(tp);
    }

    // ---------- the student's view ----------

    @Test
    void aStudentSeesTheirOwnProgressAndWhatTheyStillOwe() throws Exception {
        JsonNode board = bodyOf(get("/api/v1/dashboard/me", token(AMIRA)), 200);

        assertThat(board.at("/courses").asLong()).isEqualTo(1);
        assertThat(board.at("/totalLessons").asLong()).isEqualTo(4);
        assertThat(board.at("/lessonsCompleted").asLong()).isEqualTo(1);
        assertThat(board.at("/progressPercent").asInt()).isEqualTo(25);
        assertThat(board.at("/assignmentsToHandIn").asLong())
                .describedAs("the open TP is still outstanding").isEqualTo(1);
        assertThat(board.at("/overallAverage").isNull())
                .describedAs("nothing marked yet is not an average of zero").isTrue();
    }

    @Test
    void theNextDeadlineIsListedWithItsKind() throws Exception {
        JsonNode deadlines = bodyOf(get("/api/v1/dashboard/me", token(AMIRA)), 200)
                .at("/upcomingDeadlines");

        assertThat(deadlines.size()).isEqualTo(1);
        assertThat(deadlines.at("/0/kind").asText()).isEqualTo("ASSIGNMENT");
        assertThat(deadlines.at("/0/title").asText()).isEqualTo("TP n°1");
        assertThat(deadlines.at("/0/courseCode").asText()).isEqualTo("INF201");
    }

    @Test
    void anAdminHasNoStudentDashboard() {
        assertThat(get("/api/v1/dashboard/me", token(ADMIN))).hasStatus(403);
    }

    // ---------- the admin's view ----------

    @Test
    void theAdminBoardCountsTheSectionAndDrawsSevenDays() throws Exception {
        JsonNode board = bodyOf(get("/api/v1/dashboard/admin", token(ADMIN)), 200);

        assertThat(board.at("/activeStudents").asLong()).isEqualTo(2);
        assertThat(board.at("/courses").asLong()).isEqualTo(1);
        assertThat(board.at("/openAssignments").asLong()).isEqualTo(1);
        assertThat(board.at("/submissionsAwaitingMarking").asLong()).isZero();
        assertThat(board.at("/weeklySubmissions").size())
                .describedAs("a quiet day is a zero, not a gap").isEqualTo(7);
        assertThat(board.at("/weeklySubmissions/0/count").asLong()).isZero();
    }

    @Test
    void aStudentCannotOpenTheAdminBoard() {
        assertThat(get("/api/v1/dashboard/admin", token(AMIRA))).hasStatus(403);
    }

    // ---------- caching ----------

    @Test
    void theCacheIsKeyedPerUserAndNeverShowsOneStudentAnothersFigures() throws Exception {
        JsonNode amiras = bodyOf(get("/api/v1/dashboard/me", token(AMIRA)), 200);
        JsonNode yacines = bodyOf(get("/api/v1/dashboard/me", token(YACINE)), 200);

        assertThat(amiras.at("/lessonsCompleted").asLong()).isEqualTo(1);
        assertThat(yacines.at("/lessonsCompleted").asLong())
                .describedAs("Yacine has finished nothing; a shared key would have said 1").isZero();
    }

    @Test
    void asecondCallWithinTheWindowIsServedFromTheCache() throws Exception {
        assertThat(bodyOf(get("/api/v1/dashboard/me", token(AMIRA)), 200)
                .at("/lessonsCompleted").asLong()).isEqualTo(1);

        // Finishing another lesson does not move the figure until the entry expires
        User amira = users.findByEmail(AMIRA).orElseThrow();
        Course course = courses.findDetailById(courses.findAll().getFirst().getId()).orElseThrow();
        completions.saveAndFlush(new LessonCompletion(amira,
                course.getChapters().getFirst().getLessons().get(1)));

        assertThat(bodyOf(get("/api/v1/dashboard/me", token(AMIRA)), 200)
                .at("/lessonsCompleted").asLong())
                .describedAs("served from the cache, one minute stale by design").isEqualTo(1);
    }

    // ---------- helpers ----------

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
}
