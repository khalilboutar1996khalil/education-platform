package com.example.education_platform.quiz;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

/** Sitting a quiz: starting, answering, submitting, and the scoring that comes out of it. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuizAttemptApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String STUDENT = "amira@eduflow.dz";
    private static final String OTHER_STUDENT = "lina@eduflow.dz";
    private static final String OTHER_LEVEL = "sofiane@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private QuizRepository quizzes;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long quizId;
    private Long draftQuizId;
    private Long singleQuestionId;
    private Long singleCorrectChoiceId;
    private Long singleWrongChoiceId;
    private Long multiQuestionId;
    private List<Long> multiCorrectChoiceIds;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        users.saveAndFlush(new User("Lina Boudiaf", OTHER_STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        users.saveAndFlush(new User("Sofiane Meziane", OTHER_LEVEL,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.SECOND_AS));

        Course course = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A"));

        Quiz quiz = new Quiz(course, "Contrôle n°1", "Les bases");
        quiz.setStatus(QuizStatus.IN_PROGRESS);
        quiz.setMaxAttempts(2);

        Question single = new Question("Que vaut 2 + 2 ?", QuestionType.SINGLE_CHOICE, new BigDecimal("6"));
        single.addChoice(new Choice("3", false), null);
        single.addChoice(new Choice("4", true), null);
        quiz.addQuestion(single, null);

        Question multi = new Question("Lesquels sont des langages ?",
                QuestionType.MULTIPLE_CHOICE, new BigDecimal("4"));
        multi.addChoice(new Choice("Java", true), null);
        multi.addChoice(new Choice("HTML", false), null);
        multi.addChoice(new Choice("Python", true), null);
        quiz.addQuestion(multi, null);

        Quiz saved = quizzes.saveAndFlush(quiz);
        quizId = saved.getId();
        singleQuestionId = saved.getQuestions().get(0).getId();
        singleWrongChoiceId = saved.getQuestions().get(0).getChoices().get(0).getId();
        singleCorrectChoiceId = saved.getQuestions().get(0).getChoices().get(1).getId();
        multiQuestionId = saved.getQuestions().get(1).getId();
        multiCorrectChoiceIds = List.of(
                saved.getQuestions().get(1).getChoices().get(0).getId(),
                saved.getQuestions().get(1).getChoices().get(2).getId());

        Quiz draft = new Quiz(course, "Brouillon", null);
        draft.addQuestion(new Question("Secret", QuestionType.OPEN, BigDecimal.ONE), null);
        draftQuizId = quizzes.saveAndFlush(draft).getId();
    }

    // ---------- what a student may see ----------

    @Test
    void theStudentQuizListHidesDraftsAndTheAnswerKey() throws Exception {
        JsonNode page = bodyOf(get("/api/v1/my/quizzes", studentToken()), 200);

        assertThat(page.at("/totalElements").asLong())
                .describedAs("the draft must not be listed").isEqualTo(1);
        JsonNode card = page.at("/content/0");
        assertThat(card.at("/title").asText()).isEqualTo("Contrôle n°1");
        assertThat(card.at("/questionCount").asInt()).isEqualTo(2);
        assertThat(card.at("/totalPoints").asDouble()).isEqualTo(10.0);
        assertThat(card.at("/attemptsUsed").asLong()).isZero();
        assertThat(card.at("/openNow").asBoolean()).isTrue();
        assertThat(page.toString()).doesNotContain("correct");
    }

    @Test
    void startingAnAttemptReturnsTheQuestionsWithoutTheAnswers() throws Exception {
        JsonNode attempt = bodyOf(post("/api/v1/quizzes/" + quizId + "/attempts", studentToken()), 200);

        assertThat(attempt.at("/status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(attempt.at("/attemptNumber").asInt()).isEqualTo(1);
        assertThat(attempt.at("/questions").size()).isEqualTo(2);
        assertThat(attempt.at("/questions/0/choices").size()).isEqualTo(2);
        assertThat(attempt.at("/score").isNull()).isTrue();
        assertThat(attempt.toString())
                .describedAs("the student payload must never carry the key").doesNotContain("correct");
    }

    @Test
    void aStudentCannotStartAQuizOfAnotherLevel() {
        assertThat(post("/api/v1/quizzes/" + quizId + "/attempts", tokenFor(OTHER_LEVEL))).hasStatus(403);
    }

    @Test
    void aDraftQuizCannotBeStarted() {
        assertThat(post("/api/v1/quizzes/" + draftQuizId + "/attempts", studentToken())).hasStatus(422);
    }

    @Test
    void anAdminDoesNotSitQuizzes() {
        assertThat(post("/api/v1/quizzes/" + quizId + "/attempts", adminToken())).hasStatus(403);
        assertThat(get("/api/v1/my/quizzes", adminToken())).hasStatus(403);
    }

    // ---------- answering ----------

    @Test
    void answersAreUpsertedRatherThanDuplicated() throws Exception {
        long attemptId = startAttempt();

        saveAnswer(attemptId, singleQuestionId, List.of(singleWrongChoiceId));
        JsonNode after = bodyOf(saveAnswerResult(attemptId, singleQuestionId,
                List.of(singleCorrectChoiceId)), 200);

        assertThat(after.at("/answers").size())
                .describedAs("answering the same question twice updates one row").isEqualTo(1);
        assertThat(after.at("/answers/0/selectedChoiceIds/0").asLong()).isEqualTo(singleCorrectChoiceId);
        assertThat(after.at("/answers/0/awardedPoints").isNull())
                .describedAs("marks stay hidden until the paper is handed in").isTrue();
    }

    @Test
    void aChoiceFromAnotherQuestionIsRefused() throws Exception {
        long attemptId = startAttempt();

        assertThat(saveAnswerResult(attemptId, singleQuestionId, multiCorrectChoiceIds))
                .hasStatus(422);
    }

    @Test
    void aSingleChoiceQuestionRefusesTwoSelections() throws Exception {
        long attemptId = startAttempt();

        assertThat(saveAnswerResult(attemptId, singleQuestionId,
                List.of(singleCorrectChoiceId, singleWrongChoiceId))).hasStatus(422);
    }

    @Test
    void aStudentCannotTouchSomebodyElsesAttempt() throws Exception {
        long attemptId = startAttempt();

        assertThat(get("/api/v1/attempts/" + attemptId, tokenFor(OTHER_STUDENT))).hasStatus(403);
        assertThat(post("/api/v1/attempts/" + attemptId + "/submit", tokenFor(OTHER_STUDENT)))
                .hasStatus(403);
    }

    // ---------- scoring ----------

    @Test
    void aFullyCorrectPaperScoresEverything() throws Exception {
        long attemptId = startAttempt();
        saveAnswer(attemptId, singleQuestionId, List.of(singleCorrectChoiceId));
        saveAnswer(attemptId, multiQuestionId, multiCorrectChoiceIds);

        JsonNode submitted = bodyOf(post("/api/v1/attempts/" + attemptId + "/submit", studentToken()), 200);

        assertThat(submitted.at("/status").asText())
                .describedAs("no open question, so it is graded outright").isEqualTo("GRADED");
        assertThat(submitted.at("/score").asDouble()).isEqualTo(10.0);
        assertThat(submitted.at("/maxScore").asDouble()).isEqualTo(10.0);
        assertThat(submitted.at("/submittedAt").isNull()).isFalse();
    }

    @Test
    void aPartialMultipleChoiceAnswerEarnsNothing() throws Exception {
        long attemptId = startAttempt();
        saveAnswer(attemptId, singleQuestionId, List.of(singleCorrectChoiceId));
        saveAnswer(attemptId, multiQuestionId, List.of(multiCorrectChoiceIds.getFirst()));

        JsonNode submitted = bodyOf(post("/api/v1/attempts/" + attemptId + "/submit", studentToken()), 200);

        assertThat(submitted.at("/score").asDouble())
                .describedAs("6 for the single choice, 0 for the half-answered multiple").isEqualTo(6.0);
    }

    @Test
    void anUnansweredPaperScoresZeroRatherThanFailing() throws Exception {
        long attemptId = startAttempt();

        JsonNode submitted = bodyOf(post("/api/v1/attempts/" + attemptId + "/submit", studentToken()), 200);

        assertThat(submitted.at("/score").asDouble()).isZero();
        assertThat(submitted.at("/maxScore").asDouble()).isEqualTo(10.0);
    }

    @Test
    void perQuestionMarksAppearOnlyAfterSubmitting() throws Exception {
        long attemptId = startAttempt();
        saveAnswer(attemptId, singleQuestionId, List.of(singleCorrectChoiceId));

        JsonNode submitted = bodyOf(post("/api/v1/attempts/" + attemptId + "/submit", studentToken()), 200);
        assertThat(submitted.at("/answers/0/awardedPoints").asDouble()).isEqualTo(6.0);
    }

    @Test
    void aSubmittedAttemptCannotBeChangedOrResubmitted() throws Exception {
        long attemptId = startAttempt();
        post("/api/v1/attempts/" + attemptId + "/submit", studentToken());

        assertThat(saveAnswerResult(attemptId, singleQuestionId, List.of(singleCorrectChoiceId)))
                .hasStatus(422);
        assertThat(post("/api/v1/attempts/" + attemptId + "/submit", studentToken())).hasStatus(422);
    }

    // ---------- attempts and the clock ----------

    @Test
    void startingTwiceResumesRatherThanBurningAnAttempt() throws Exception {
        long first = startAttempt();
        JsonNode again = bodyOf(post("/api/v1/quizzes/" + quizId + "/attempts", studentToken()), 200);

        assertThat(again.at("/id").asLong()).isEqualTo(first);
        assertThat(again.at("/attemptNumber").asInt()).isEqualTo(1);
    }

    @Test
    void theAttemptLimitIsEnforcedAcrossSubmissions() throws Exception {
        long first = startAttempt();
        post("/api/v1/attempts/" + first + "/submit", studentToken());

        JsonNode second = bodyOf(post("/api/v1/quizzes/" + quizId + "/attempts", studentToken()), 200);
        assertThat(second.at("/attemptNumber").asInt()).isEqualTo(2);
        post("/api/v1/attempts/" + second.at("/id").asLong() + "/submit", studentToken());

        assertThat(post("/api/v1/quizzes/" + quizId + "/attempts", studentToken()))
                .describedAs("maxAttempts is 2").hasStatus(422);
    }

    @Test
    void anAttemptPastTheDeadlineIsExpiredRatherThanAccepted() throws Exception {
        long attemptId = startAttempt();

        // The deadline moves into the past while the attempt is open
        Quiz quiz = quizzes.findById(quizId).orElseThrow();
        quiz.setDeadline(Instant.now().minus(1, ChronoUnit.MINUTES));
        quizzes.saveAndFlush(quiz);

        assertThat(saveAnswerResult(attemptId, singleQuestionId, List.of(singleCorrectChoiceId)))
                .hasStatus(422);
        assertThat(bodyOf(get("/api/v1/attempts/" + attemptId, studentToken()), 200).at("/status").asText())
                .isEqualTo("EXPIRED");
    }

    // ---------- helpers ----------

    private long startAttempt() throws Exception {
        return bodyOf(post("/api/v1/quizzes/" + quizId + "/attempts", studentToken()), 200).at("/id").asLong();
    }

    private void saveAnswer(long attemptId, Long questionId, List<Long> choiceIds) throws Exception {
        bodyOf(saveAnswerResult(attemptId, questionId, choiceIds), 200);
    }

    private MvcTestResult saveAnswerResult(long attemptId, Long questionId, List<Long> choiceIds) {
        String ids = choiceIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        return put("/api/v1/attempts/" + attemptId + "/answers", studentToken(), """
                {"answers":[{"questionId":%d,"selectedChoiceIds":[%s]}]}""".formatted(questionId, ids));
    }

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

    private MvcTestResult post(String uri, String token) {
        return mvc.post().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult put(String uri, String token, String body) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }
}
