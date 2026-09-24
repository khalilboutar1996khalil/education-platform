package com.example.education_platform.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.WorkMode;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** The whole TP workflow: publish, draft, attach, hand in, mark — and who may do each. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.storage.location=${java.io.tmpdir}/eduflow-assignment-test")
class AssignmentApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String AMIRA = "amira@eduflow.dz";
    private static final String YACINE = "yacine@eduflow.dz";
    private static final String LINA = "lina@eduflow.dz";
    private static final String OTHER_LEVEL = "sofiane@eduflow.dz";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long courseId;
    private Long tpId;
    private Long draftId;
    private Long yacineId;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", AMIRA,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        yacineId = users.saveAndFlush(new User("Yacine Cherif", YACINE,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS)).getId();
        users.saveAndFlush(new User("Lina Boudiaf", LINA,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        users.saveAndFlush(new User("Sofiane Meziane", OTHER_LEVEL,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.SECOND_AS));

        Course course = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A"));
        courseId = course.getId();

        Assignment tp = new Assignment(course, "TP n°1 — Calculatrice", AssignmentType.TP, WorkMode.PAIR);
        tp.setStatus(AssignmentStatus.OPEN);
        tp.setDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        tpId = assignments.saveAndFlush(tp).getId();

        Assignment draft = new Assignment(course, "Devoir caché", AssignmentType.DEVOIR, WorkMode.INDIVIDUAL);
        draftId = assignments.saveAndFlush(draft).getId();
    }

    // ---------- visibility ----------

    @Test
    void aStudentSeesPublishedWorkAtTheirLevelOnly() throws Exception {
        JsonNode page = bodyOf(get("/api/v1/assignments", token(AMIRA)), 200);

        assertThat(page.at("/totalElements").asLong())
                .describedAs("the draft is not listed").isEqualTo(1);
        assertThat(page.at("/content/0/title").asText()).isEqualTo("TP n°1 — Calculatrice");
        assertThat(page.at("/content/0/acceptingSubmissions").asBoolean()).isTrue();
        assertThat(page.at("/content/0/mySubmissionStatus").isNull()).isTrue();
    }

    @Test
    void anAdminSeesDraftsToo() throws Exception {
        assertThat(bodyOf(get("/api/v1/assignments", token(ADMIN)), 200).at("/totalElements").asLong())
                .isEqualTo(2);
    }

    @Test
    void aStudentIsRefusedADraftAndWorkFromAnotherLevel() {
        assertThat(get("/api/v1/assignments/" + draftId, token(AMIRA))).hasStatus(403);
        assertThat(get("/api/v1/assignments/" + tpId, token(OTHER_LEVEL))).hasStatus(403);
        assertThat(get("/api/v1/assignments/" + draftId, token(ADMIN))).hasStatusOk();
    }

    @Test
    void aStudentCannotCreateOrPublishWork() {
        assertThat(post("/api/v1/courses/" + courseId + "/assignments", token(AMIRA), """
                {"title":"Nope","type":"TP","mode":"PAIR","maxPoints":20}""")).hasStatus(403);
        assertThat(patch("/api/v1/assignments/" + draftId + "/status", token(AMIRA), """
                {"status":"OPEN"}""")).hasStatus(403);
    }

    // ---------- the student's workflow ----------

    @Test
    void theFullWorkflowFromDraftToGrade() throws Exception {
        // 1. the student starts a draft and names a partner
        JsonNode draft = bodyOf(put("/api/v1/assignments/" + tpId + "/submission", token(AMIRA), """
                {"comment":"Première version","partnerId":%d}""".formatted(yacineId)), 200);
        assertThat(draft.at("/status").asText()).isEqualTo("DRAFT");
        assertThat(draft.at("/partner/email").asText()).isEqualTo(YACINE);
        long submissionId = draft.at("/id").asLong();

        // 2. attaches a file
        JsonNode withFile = bodyOf(upload("/api/v1/assignments/" + tpId + "/submission/files",
                token(AMIRA), "calculatrice.pdf"), 200);
        assertThat(withFile.at("/files").size()).isEqualTo(1);
        long fileId = withFile.at("/files/0/id").asLong();
        assertThat(withFile.at("/files/0/originalFilename").asText()).isEqualTo("calculatrice.pdf");

        // 3. hands it in
        JsonNode submitted = bodyOf(post("/api/v1/assignments/" + tpId + "/submission/submit",
                token(AMIRA), null), 200);
        assertThat(submitted.at("/status").asText())
                .describedAs("before the deadline, so not late").isEqualTo("SUBMITTED");
        assertThat(submitted.at("/submittedAt").isNull()).isFalse();

        // 4. the partner sees the same row
        assertThat(bodyOf(get("/api/v1/assignments/" + tpId + "/submission", token(YACINE)), 200)
                .at("/id").asLong())
                .describedAs("a pair shares one submission").isEqualTo(submissionId);

        // 5. it appears in the teacher's queue
        JsonNode queue = bodyOf(get("/api/v1/assignments/" + tpId + "/submissions?onlyPending=true",
                token(ADMIN)), 200);
        assertThat(queue.at("/totalElements").asLong()).isEqualTo(1);

        // 6. the teacher downloads the file and marks it
        assertThat(get("/api/v1/submissions/" + submissionId + "/files/" + fileId, token(ADMIN)))
                .hasStatusOk();
        JsonNode graded = bodyOf(post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":16.5,"feedback":"Bon travail"}"""), 200);
        assertThat(graded.at("/status").asText()).isEqualTo("GRADED");
        assertThat(graded.at("/grade").asDouble()).isEqualTo(16.5);

        // 7. both students see the mark on their list
        JsonNode card = bodyOf(get("/api/v1/assignments", token(YACINE)), 200).at("/content/0");
        assertThat(card.at("/mySubmissionStatus").asText()).isEqualTo("GRADED");
        assertThat(card.at("/myGrade").asDouble()).isEqualTo(16.5);
    }

    @Test
    void handingInWithoutAFileIsRefused() throws Exception {
        put("/api/v1/assignments/" + tpId + "/submission", token(AMIRA), """
                {"comment":"Rien joint"}""");

        assertThat(post("/api/v1/assignments/" + tpId + "/submission/submit", token(AMIRA), null))
                .hasStatus(422);
    }

    @Test
    void aSubmittedPieceOfWorkCanNoLongerBeChanged() throws Exception {
        long submissionId = handInSomething(AMIRA);

        assertThat(put("/api/v1/assignments/" + tpId + "/submission", token(AMIRA), """
                {"comment":"Trop tard"}""")).hasStatus(422);
        assertThat(upload("/api/v1/assignments/" + tpId + "/submission/files", token(AMIRA), "autre.pdf"))
                .hasStatus(422);
        assertThat(post("/api/v1/assignments/" + tpId + "/submission/submit", token(AMIRA), null))
                .hasStatus(422);
        assertThat(submissionId).isPositive();
    }

    // ---------- pair rules ----------

    @Test
    void aPartnerAlreadyInAnotherSubmissionIsRefused() throws Exception {
        put("/api/v1/assignments/" + tpId + "/submission", token(YACINE), "{}");

        assertThat(put("/api/v1/assignments/" + tpId + "/submission", token(AMIRA), """
                {"partnerId":%d}""".formatted(yacineId)))
                .describedAs("one student cannot sit in two submissions for the same work")
                .hasStatus(422);
    }

    @Test
    void aPartnerFromAnotherLevelIsRefused() throws Exception {
        Long sofianeId = users.findByEmail(OTHER_LEVEL).orElseThrow().getId();

        assertThat(put("/api/v1/assignments/" + tpId + "/submission", token(AMIRA), """
                {"partnerId":%d}""".formatted(sofianeId))).hasStatus(422);
    }

    @Test
    void anIndividualAssignmentRefusesAPartner() throws Exception {
        patch("/api/v1/assignments/" + draftId + "/status", token(ADMIN), """
                {"status":"OPEN"}""");

        assertThat(put("/api/v1/assignments/" + draftId + "/submission", token(AMIRA), """
                {"partnerId":%d}""".formatted(yacineId))).hasStatus(422);
    }

    // ---------- ownership ----------

    @Test
    void aStrangerCannotReadOrDownloadSomebodyElsesWork() throws Exception {
        long submissionId = handInSomething(AMIRA);
        long fileId = bodyOf(get("/api/v1/assignments/" + tpId + "/submission", token(AMIRA)), 200)
                .at("/files/0/id").asLong();

        assertThat(get("/api/v1/submissions/" + submissionId + "/files/" + fileId, token(LINA)))
                .hasStatus(403);
        assertThat(get("/api/v1/assignments/" + tpId + "/submission", token(LINA)))
                .describedAs("Lina has none of her own").hasStatus(404);
    }

    @Test
    void aStudentCannotGradeAnything() throws Exception {
        long submissionId = handInSomething(AMIRA);

        assertThat(post("/api/v1/submissions/" + submissionId + "/grade", token(LINA), """
                {"grade":20}""")).hasStatus(403);
        assertThat(get("/api/v1/assignments/" + tpId + "/submissions", token(LINA))).hasStatus(403);
    }

    // ---------- grading rules ----------

    @Test
    void aGradeAboveTheMaximumIsRefused() throws Exception {
        long submissionId = handInSomething(AMIRA);

        assertThat(post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":25}""")).hasStatus(422);
    }

    @Test
    void aNegativeGradeIsRejectedInFrench() throws Exception {
        long submissionId = handInSomething(AMIRA);

        assertThat(post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":-1}"""))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.errors.grade")
                        .isEqualTo("La note ne peut pas être négative"));
    }

    @Test
    void aDraftCannotBeGraded() throws Exception {
        long submissionId = bodyOf(put("/api/v1/assignments/" + tpId + "/submission", token(AMIRA),
                "{}"), 200).at("/id").asLong();

        assertThat(post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":10}""")).hasStatus(422);
    }

    // ---------- the brief ----------

    @Test
    void theSubjectSheetIsUploadedByTheAdminAndReadableByTheStudents() throws Exception {
        JsonNode withBrief = bodyOf(upload("/api/v1/assignments/" + tpId + "/brief",
                token(ADMIN), "sujet.pdf"), 200);
        assertThat(withBrief.at("/brief/originalFilename").asText()).isEqualTo("sujet.pdf");

        assertThat(get("/api/v1/assignments/" + tpId + "/brief", token(AMIRA))).hasStatusOk();
        assertThat(get("/api/v1/assignments/" + tpId + "/brief", token(OTHER_LEVEL)))
                .describedAs("another level cannot read the sheet").hasStatus(403);
    }

    @Test
    void anAssignmentWithNoSheetReports404() {
        assertThat(get("/api/v1/assignments/" + tpId + "/brief", token(AMIRA))).hasStatus(404);
    }

    @Test
    void aStudentCannotUploadASubjectSheet() {
        assertThat(upload("/api/v1/assignments/" + tpId + "/brief", token(AMIRA), "faux-sujet.pdf"))
                .hasStatus(403);
    }

    // ---------- helpers ----------

    private long handInSomething(String email) throws Exception {
        long id = bodyOf(put("/api/v1/assignments/" + tpId + "/submission", token(email), "{}"), 200)
                .at("/id").asLong();
        upload("/api/v1/assignments/" + tpId + "/submission/files", token(email), "travail.pdf");
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

    private MvcTestResult patch(String uri, String token, String body) {
        return mvc.patch().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body)
                .header("Authorization", "Bearer " + token).exchange();
    }

    private MvcTestResult upload(String uri, String token, String filename) {
        MockMultipartFile file = new MockMultipartFile("file", filename, "application/pdf",
                "contenu du fichier".getBytes(StandardCharsets.UTF_8));
        return mvc.post().uri(uri).multipart().file(file)
                .header("Authorization", "Bearer " + token).exchange();
    }
}
