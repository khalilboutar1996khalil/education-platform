package com.example.education_platform.grade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.WorkMode;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.quiz.entity.Choice;
import com.example.education_platform.quiz.entity.Question;
import com.example.education_platform.quiz.entity.QuestionType;
import com.example.education_platform.quiz.entity.Quiz;
import com.example.education_platform.quiz.entity.QuizStatus;
import com.example.education_platform.quiz.repository.QuizRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** Marks reaching the gradebook from all three sources, and the averages they produce. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.storage.location=${java.io.tmpdir}/eduflow-gradebook-test")
class GradebookApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String AMIRA = "amira@eduflow.dz";
    private static final String YACINE = "yacine@eduflow.dz";
    private static final String OTHER_LEVEL = "sofiane@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private QuizRepository quizzes;

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long courseId;
    private Long quizId;
    private Long tpId;
    private Long amiraId;
    private Long yacineId;
    private Long correctChoiceId;
    private Long questionId;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        amiraId = users.saveAndFlush(new User("Amira Benali", AMIRA,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS)).getId();
        yacineId = users.saveAndFlush(new User("Yacine Cherif", YACINE,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS)).getId();
        users.saveAndFlush(new User("Sofiane Meziane", OTHER_LEVEL,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.SECOND_AS));

        Course course = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A"));
        courseId = course.getId();

        Quiz quiz = new Quiz(course, "Contrôle n°1", null);
        quiz.setStatus(QuizStatus.IN_PROGRESS);
        Question question = new Question("Que vaut 2 + 2 ?", QuestionType.SINGLE_CHOICE, new BigDecimal("10"));
        question.addChoice(new Choice("3", false), null);
        question.addChoice(new Choice("4", true), null);
        quiz.addQuestion(question, null);
        Quiz savedQuiz = quizzes.saveAndFlush(quiz);
        quizId = savedQuiz.getId();
        questionId = savedQuiz.getQuestions().getFirst().getId();
        correctChoiceId = savedQuiz.getQuestions().getFirst().getChoices().get(1).getId();

        Assignment tp = new Assignment(course, "TP n°1", AssignmentType.TP, WorkMode.INDIVIDUAL);
        tp.setStatus(AssignmentStatus.OPEN);
        tp.setDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        tp.setMaxPoints(new BigDecimal("20"));
        tpId = assignments.saveAndFlush(tp).getId();
    }

    // ---------- marks arriving from their sources ----------

    @Test
    void aScoredQuizLandsInTheGradebookByItself() throws Exception {
        sitQuizPerfectly(AMIRA);

        JsonNode grades = bodyOf(get("/api/v1/my/grades/" + courseId, token(AMIRA)), 200);
        assertThat(grades.size()).isEqualTo(1);
        assertThat(grades.at("/0/kind").asText()).isEqualTo("QUIZ");
        assertThat(grades.at("/0/label").asText()).isEqualTo("Contrôle n°1");
        assertThat(grades.at("/0/score").asDouble()).isEqualTo(10.0);
        assertThat(grades.at("/0/outOfTwenty").asDouble())
                .describedAs("10 out of 10 rescales to 20").isEqualTo(20.0);
    }

    @Test
    void aMarkedSubmissionLandsInTheGradebookByItself() throws Exception {
        long submissionId = handIn(AMIRA);
        post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":15,"feedback":"Bien"}""");

        JsonNode grades = bodyOf(get("/api/v1/my/grades/" + courseId, token(AMIRA)), 200);
        assertThat(grades.size()).isEqualTo(1);
        assertThat(grades.at("/0/kind").asText()).isEqualTo("ASSIGNMENT");
        assertThat(grades.at("/0/outOfTwenty").asDouble()).isEqualTo(15.0);
    }

    @Test
    void reMarkingUpdatesTheRowRatherThanAddingASecondMark() throws Exception {
        long submissionId = handIn(AMIRA);
        post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":8}""");
        post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":14}""");

        JsonNode grades = bodyOf(get("/api/v1/my/grades/" + courseId, token(AMIRA)), 200);
        assertThat(grades.size())
                .describedAs("one grade per source, enforced by a partial unique index").isEqualTo(1);
        assertThat(grades.at("/0/score").asDouble()).isEqualTo(14.0);
    }

    // ---------- manual marks ----------

    @Test
    void aTeacherRecordsAPartielByHand() throws Exception {
        JsonNode row = bodyOf(post("/api/v1/courses/" + courseId + "/grades", token(ADMIN), """
                {"studentId":%d,"label":"Partiel 1","score":12,"maxScore":20,"weight":2}"""
                .formatted(amiraId)), 201);

        assertThat(row.at("/student/email").asText()).isEqualTo(AMIRA);
        assertThat(row.at("/grades/0/kind").asText()).isEqualTo("MANUAL");
        assertThat(row.at("/grades/0/weight").asDouble()).isEqualTo(2.0);
        assertThat(row.at("/average").asDouble()).isEqualTo(12.0);
    }

    @Test
    void aStudentFromAnotherLevelCannotBeGradedInThisModule() throws Exception {
        Long sofianeId = users.findByEmail(OTHER_LEVEL).orElseThrow().getId();

        assertThat(post("/api/v1/courses/" + courseId + "/grades", token(ADMIN), """
                {"studentId":%d,"label":"Partiel","score":10,"maxScore":20}""".formatted(sofianeId)))
                .hasStatus(422);
    }

    @Test
    void aScoreAboveItsMaximumIsRefused() {
        assertThat(post("/api/v1/courses/" + courseId + "/grades", token(ADMIN), """
                {"studentId":%d,"label":"Partiel","score":25,"maxScore":20}""".formatted(amiraId)))
                .hasStatus(422);
    }

    @Test
    void anAutomaticMarkCannotBeDeletedByHand() throws Exception {
        sitQuizPerfectly(AMIRA);
        long gradeId = bodyOf(get("/api/v1/my/grades/" + courseId, token(AMIRA)), 200)
                .at("/0/id").asLong();

        assertThat(delete("/api/v1/grades/" + gradeId, token(ADMIN)))
                .describedAs("the source would write it straight back").hasStatus(422);
    }

    @Test
    void aManualMarkCanBeDeleted() throws Exception {
        long gradeId = bodyOf(post("/api/v1/courses/" + courseId + "/grades", token(ADMIN), """
                {"studentId":%d,"label":"Erreur de saisie","score":3,"maxScore":20}"""
                .formatted(amiraId)), 201).at("/grades/0/id").asLong();

        assertThat(delete("/api/v1/grades/" + gradeId, token(ADMIN))).hasStatus(204);
        assertThat(bodyOf(get("/api/v1/my/grades/" + courseId, token(AMIRA)), 200).size()).isZero();
    }

    // ---------- averaging ----------

    @Test
    void theAverageIsWeightedAcrossEveryKindOfMark() throws Exception {
        // quiz: 10/10 -> 20/20, weight 1
        sitQuizPerfectly(AMIRA);
        // assignment: 15/20, weight 1
        long submissionId = handIn(AMIRA);
        post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":15}""");
        // manual: 10/20 with weight 2
        post("/api/v1/courses/" + courseId + "/grades", token(ADMIN), """
                {"studentId":%d,"label":"Partiel 1","score":10,"maxScore":20,"weight":2}"""
                .formatted(amiraId));

        JsonNode averages = bodyOf(get("/api/v1/my/averages", token(AMIRA)), 200);
        assertThat(averages.size()).isEqualTo(1);
        assertThat(averages.at("/0/courseCode").asText()).isEqualTo("INF201");
        assertThat(averages.at("/0/gradeCount").asInt()).isEqualTo(3);
        // (20*1 + 15*1 + 10*2) / 4 = 13.75
        assertThat(averages.at("/0/average").asDouble()).isEqualTo(13.75);
    }

    @Test
    void aModuleWithNoMarksIsAbsentRatherThanZero() throws Exception {
        assertThat(bodyOf(get("/api/v1/my/averages", token(AMIRA)), 200).size())
                .describedAs("no marks is not the same as a zero").isZero();
    }

    // ---------- the gradebook ----------

    @Test
    void theGradebookListsEveryStudentHoldingAMark() throws Exception {
        sitQuizPerfectly(AMIRA);
        post("/api/v1/courses/" + courseId + "/grades", token(ADMIN), """
                {"studentId":%d,"label":"Partiel 1","score":14,"maxScore":20}""".formatted(yacineId));

        JsonNode book = bodyOf(get("/api/v1/courses/" + courseId + "/gradebook", token(ADMIN)), 200);

        assertThat(book.size()).isEqualTo(2);
        assertThat(book.at("/0/student/fullName").asText())
                .describedAs("sorted by name").isEqualTo("Amira Benali");
        assertThat(book.at("/0/average").asDouble()).isEqualTo(20.0);
        assertThat(book.at("/1/average").asDouble()).isEqualTo(14.0);
    }

    // ---------- access ----------

    @Test
    void aStudentCannotReadTheGradebookOrRecordMarks() {
        assertThat(get("/api/v1/courses/" + courseId + "/gradebook", token(AMIRA))).hasStatus(403);
        assertThat(post("/api/v1/courses/" + courseId + "/grades", token(AMIRA), """
                {"studentId":%d,"label":"20 partout","score":20,"maxScore":20}""".formatted(amiraId)))
                .hasStatus(403);
    }

    @Test
    void anAdminHasNoMarksOfTheirOwn() {
        assertThat(get("/api/v1/my/averages", token(ADMIN))).hasStatus(403);
    }

    @Test
    void aStudentOnlyEverSeesTheirOwnMarks() throws Exception {
        post("/api/v1/courses/" + courseId + "/grades", token(ADMIN), """
                {"studentId":%d,"label":"Partiel 1","score":18,"maxScore":20}""".formatted(yacineId));

        assertThat(bodyOf(get("/api/v1/my/grades/" + courseId, token(AMIRA)), 200).size())
                .describedAs("Yacine's mark is not Amira's business").isZero();
        assertThat(bodyOf(get("/api/v1/my/grades/" + courseId, token(YACINE)), 200).size()).isEqualTo(1);
    }

    // ---------- helpers ----------

    private void sitQuizPerfectly(String email) throws Exception {
        long attemptId = bodyOf(post("/api/v1/quizzes/" + quizId + "/attempts", token(email), null), 200)
                .at("/id").asLong();
        put("/api/v1/attempts/" + attemptId + "/answers", token(email), """
                {"answers":[{"questionId":%d,"selectedChoiceIds":[%d]}]}"""
                .formatted(questionId, correctChoiceId));
        post("/api/v1/attempts/" + attemptId + "/submit", token(email), null);
    }

    private long handIn(String email) throws Exception {
        long id = bodyOf(put("/api/v1/assignments/" + tpId + "/submission", token(email), "{}"), 200)
                .at("/id").asLong();
        MockMultipartFile file = new MockMultipartFile("file", "travail.pdf", "application/pdf",
                "contenu".getBytes(StandardCharsets.UTF_8));
        mvc.post().uri("/api/v1/assignments/" + tpId + "/submission/files").multipart().file(file)
                .header("Authorization", "Bearer " + token(email)).exchange();
        post("/api/v1/assignments/" + tpId + "/submission/submit", token(email), null);
        return id;
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

    private MvcTestResult post(String uri, String token, String body) {
        var request = mvc.post().uri(uri).header("Authorization", "Bearer " + token);
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return request.exchange();
    }

    private MvcTestResult put(String uri, String token, String body) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult delete(String uri, String token) {
        return mvc.delete().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }
}
