package com.example.education_platform.quiz;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
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
class QuizAuthoringApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String STUDENT = "amira@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long courseId;
    private Long quizId;

    @BeforeEach
    void seed() throws Exception {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", STUDENT,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        courseId = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A")).getId();

        quizId = bodyOf(post("/api/v1/courses/" + courseId + "/quizzes", adminToken(), """
                {"title":"Contrôle n°1","description":"Les bases","durationMinutes":30,
                 "maxAttempts":2,"shuffleQuestions":false}"""), 201).at("/id").asLong();
    }

    // ---------- creating ----------

    @Test
    void aNewQuizStartsAsADraftWithNoQuestions() throws Exception {
        JsonNode quiz = bodyOf(get("/api/v1/quizzes/" + quizId, adminToken()), 200);

        assertThat(quiz.at("/status").asText()).isEqualTo("DRAFT");
        assertThat(quiz.at("/courseCode").asText()).isEqualTo("INF201");
        assertThat(quiz.at("/questions").size()).isZero();
        assertThat(quiz.at("/totalPoints").asDouble()).isZero();
        assertThat(quiz.at("/submissions").asLong()).isZero();
        assertThat(quiz.at("/averageScore").isNull()).isTrue();
    }

    @Test
    void aDeadlineBeforeTheOpeningDateIsRefused() {
        assertThat(post("/api/v1/courses/" + courseId + "/quizzes", adminToken(), """
                {"title":"Incohérent","opensAt":"2026-05-10T10:00:00Z",
                 "deadline":"2026-05-09T10:00:00Z","maxAttempts":1}""")).hasStatus(422);
    }

    // ---------- questions ----------

    @Test
    void addingQuestionsAccumulatesTheTotalPoints() throws Exception {
        addSingleChoice("Que vaut 2 + 2 ?", 5);
        JsonNode quiz = addMultipleChoice("Lesquels sont des langages ?", 3);

        assertThat(quiz.at("/questions").size()).isEqualTo(2);
        assertThat(quiz.at("/totalPoints").asDouble()).isEqualTo(8.0);
        assertThat(quiz.at("/questions/0/position").asInt()).isEqualTo(1);
        assertThat(quiz.at("/questions/1/position").asInt()).isEqualTo(2);
        assertThat(quiz.at("/questions/0/choices").size()).isEqualTo(3);
        assertThat(quiz.at("/questions/0/choices/1/correct").asBoolean())
                .describedAs("the admin view carries the answer key").isTrue();
    }

    @Test
    void deletingAQuestionRenumbersTheRest() throws Exception {
        addSingleChoice("Première", 2);
        JsonNode quiz = addSingleChoice("Deuxième", 2);
        long firstId = quiz.at("/questions/0/id").asLong();

        JsonNode after = bodyOf(delete("/api/v1/questions/" + firstId, adminToken()), 200);
        assertThat(after.at("/questions").size()).isEqualTo(1);
        assertThat(after.at("/questions/0/text").asText()).isEqualTo("Deuxième");
        assertThat(after.at("/questions/0/position").asInt()).isEqualTo(1);
    }

    @Test
    void updatingAQuestionReplacesItsChoices() throws Exception {
        JsonNode quiz = addSingleChoice("Que vaut 2 + 2 ?", 5);
        long questionId = quiz.at("/questions/0/id").asLong();

        JsonNode updated = bodyOf(put("/api/v1/questions/" + questionId, adminToken(), """
                {"text":"Que vaut 3 + 3 ?","type":"TRUE_FALSE","points":2,
                 "choices":[{"text":"Vrai","correct":false},{"text":"Faux","correct":true}]}"""), 200);

        assertThat(updated.at("/questions/0/text").asText()).isEqualTo("Que vaut 3 + 3 ?");
        assertThat(updated.at("/questions/0/choices").size()).isEqualTo(2);
        assertThat(updated.at("/totalPoints").asDouble()).isEqualTo(2.0);
    }

    // ---------- the type rules ----------

    @Test
    void aSingleChoiceQuestionNeedsExactlyOneCorrectChoice() {
        assertThat(post("/api/v1/quizzes/" + quizId + "/questions", adminToken(), """
                {"text":"Deux bonnes réponses ?","type":"SINGLE_CHOICE","points":1,
                 "choices":[{"text":"A","correct":true},{"text":"B","correct":true}]}"""))
                .hasStatus(422);
    }

    @Test
    void aChoiceQuestionNeedsAtLeastTwoChoices() {
        assertThat(post("/api/v1/quizzes/" + quizId + "/questions", adminToken(), """
                {"text":"Une seule option ?","type":"SINGLE_CHOICE","points":1,
                 "choices":[{"text":"A","correct":true}]}""")).hasStatus(422);
    }

    @Test
    void aTrueFalseQuestionNeedsExactlyTwoChoices() {
        assertThat(post("/api/v1/quizzes/" + quizId + "/questions", adminToken(), """
                {"text":"Vrai ou faux ?","type":"TRUE_FALSE","points":1,
                 "choices":[{"text":"Vrai","correct":true},{"text":"Faux","correct":false},
                            {"text":"Peut-être","correct":false}]}""")).hasStatus(422);
    }

    @Test
    void anOpenQuestionCannotCarryChoices() {
        assertThat(post("/api/v1/quizzes/" + quizId + "/questions", adminToken(), """
                {"text":"Expliquez","type":"OPEN","points":5,
                 "choices":[{"text":"A","correct":true}]}""")).hasStatus(422);
    }

    @Test
    void aMissingQuestionTypeIsRejectedInFrench() {
        assertThat(post("/api/v1/quizzes/" + quizId + "/questions", adminToken(), """
                {"text":"Sans type","points":1}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.type")
                        .isEqualTo("Le type de question est obligatoire"));
    }

    // ---------- status ----------

    @Test
    void anEmptyQuizCannotBeOpened() {
        assertThat(patch("/api/v1/quizzes/" + quizId + "/status", adminToken(), """
                {"status":"IN_PROGRESS"}""")).hasStatus(422);
    }

    @Test
    void aQuizWithQuestionsCanBeOpenedAndThenClosed() throws Exception {
        addSingleChoice("Que vaut 2 + 2 ?", 5);

        assertThat(bodyOf(patch("/api/v1/quizzes/" + quizId + "/status", adminToken(), """
                {"status":"IN_PROGRESS"}"""), 200).at("/status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(bodyOf(patch("/api/v1/quizzes/" + quizId + "/status", adminToken(), """
                {"status":"CLOSED"}"""), 200).at("/status").asText()).isEqualTo("CLOSED");
    }

    @Test
    void questionsAreFrozenOnceTheQuizLeavesDraft() throws Exception {
        addSingleChoice("Que vaut 2 + 2 ?", 5);
        patch("/api/v1/quizzes/" + quizId + "/status", adminToken(), """
                {"status":"IN_PROGRESS"}""");

        assertThat(post("/api/v1/quizzes/" + quizId + "/questions", adminToken(), """
                {"text":"Ajoutée trop tard","type":"TRUE_FALSE","points":1,
                 "choices":[{"text":"Vrai","correct":true},{"text":"Faux","correct":false}]}"""))
                .describedAs("editing a live paper would rescore attempts already sat")
                .hasStatus(422);
    }

    // ---------- access ----------

    @Test
    void aStudentCannotReachAuthoringAtAll() {
        assertThat(get("/api/v1/quizzes/" + quizId, studentToken()))
                .describedAs("this view carries the answer key").hasStatus(403);
        assertThat(get("/api/v1/quizzes", studentToken())).hasStatus(403);
        assertThat(post("/api/v1/courses/" + courseId + "/quizzes", studentToken(), """
                {"title":"Nope","maxAttempts":1}""")).hasStatus(403);
    }

    @Test
    void anUnknownQuizIsNotFound() {
        assertThat(get("/api/v1/quizzes/999999", adminToken())).hasStatus(404);
    }

    // ---------- helpers ----------

    private JsonNode addSingleChoice(String text, int points) throws Exception {
        return bodyOf(post("/api/v1/quizzes/" + quizId + "/questions", adminToken(), """
                {"text":"%s","type":"SINGLE_CHOICE","points":%d,
                 "choices":[{"text":"A","correct":false},{"text":"B","correct":true},
                            {"text":"C","correct":false}]}""".formatted(text, points)), 201);
    }

    private JsonNode addMultipleChoice(String text, int points) throws Exception {
        return bodyOf(post("/api/v1/quizzes/" + quizId + "/questions", adminToken(), """
                {"text":"%s","type":"MULTIPLE_CHOICE","points":%d,
                 "choices":[{"text":"Java","correct":true},{"text":"HTML","correct":false},
                            {"text":"Python","correct":true}]}""".formatted(text, points)), 201);
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

    private MvcTestResult post(String uri, String token, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult put(String uri, String token, String body) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult patch(String uri, String token, String body) {
        return mvc.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult delete(String uri, String token) {
        return mvc.delete().uri(uri).header("Authorization", "Bearer " + token).exchange();
    }
}
