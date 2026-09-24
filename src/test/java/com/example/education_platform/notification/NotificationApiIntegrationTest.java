package com.example.education_platform.notification;

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

/** Notifications arrive from domain events, never from a request. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.storage.location=${java.io.tmpdir}/eduflow-notification-test")
class NotificationApiIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final String AMIRA = "amira@eduflow.dz";
    private static final String YACINE = "yacine@eduflow.dz";
    private static final String SECOND_AS = "sofiane@eduflow.dz";

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

    private Long tpId;
    private Long yacineId;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        users.saveAndFlush(new User("Amira Benali", AMIRA,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        yacineId = users.saveAndFlush(new User("Yacine Cherif", YACINE,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS)).getId();
        users.saveAndFlush(new User("Sofiane Meziane", SECOND_AS,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.SECOND_AS));

        Course course = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A"));
        Assignment tp = new Assignment(course, "TP n°1", AssignmentType.TP, WorkMode.PAIR);
        tp.setStatus(AssignmentStatus.OPEN);
        tp.setDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        tp.setMaxPoints(new BigDecimal("20"));
        tpId = assignments.saveAndFlush(tp).getId();
    }

    // ---------- arriving from events ----------

    @Test
    void markingASubmissionNotifiesBothMembersOfThePair() throws Exception {
        long submissionId = handInAsPair();
        post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":16,"feedback":"Bien"}""");

        JsonNode amiras = bodyOf(get("/api/v1/my/notifications", token(AMIRA)), 200);
        assertThat(amiras.at("/totalElements").asLong()).isEqualTo(1);
        assertThat(amiras.at("/content/0/type").asText()).isEqualTo("GRADED");
        assertThat(amiras.at("/content/0/title").asText()).contains("TP n°1");
        assertThat(amiras.at("/content/0/read").asBoolean()).isFalse();

        assertThat(bodyOf(get("/api/v1/my/notifications", token(YACINE)), 200)
                .at("/totalElements").asLong())
                .describedAs("the partner shares the mark, so they hear about it too").isEqualTo(1);
    }

    @Test
    void publishingAnAnnouncementNotifiesItsAudienceOnly() throws Exception {
        long id = bodyOf(post("/api/v1/announcements", token(ADMIN), """
                {"title":"Réunion 3AS","body":"Mardi 10h","level":"THIRD_AS"}"""), 201)
                .at("/id").asLong();
        post("/api/v1/announcements/" + id + "/publish", token(ADMIN));

        assertThat(unread(AMIRA)).isEqualTo(1);
        assertThat(unread(YACINE)).isEqualTo(1);
        assertThat(unread(SECOND_AS))
                .describedAs("a 3AS notice is not 2AS's business").isZero();
    }

    @Test
    void aDraftAnnouncementNotifiesNobody() throws Exception {
        post("/api/v1/announcements", token(ADMIN), """
                {"title":"Brouillon","body":"Pas encore"}""");

        assertThat(unread(AMIRA)).isZero();
    }

    // ---------- reading ----------

    @Test
    void markingOneReadDropsTheBadgeByOne() throws Exception {
        long submissionId = handInAsPair();
        post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":16}""");
        long notificationId = bodyOf(get("/api/v1/my/notifications", token(AMIRA)), 200)
                .at("/content/0/id").asLong();

        assertThat(unread(AMIRA)).isEqualTo(1);
        assertThat(bodyOf(post("/api/v1/my/notifications/" + notificationId + "/read", token(AMIRA)), 200)
                .at("/read").asBoolean()).isTrue();
        assertThat(unread(AMIRA)).isZero();
    }

    @Test
    void markingAllReadClearsTheBadgeAndReportsHowMany() throws Exception {
        publishTwoAnnouncements();
        assertThat(unread(AMIRA)).isEqualTo(2);

        assertThat(bodyOf(post("/api/v1/my/notifications/read-all", token(AMIRA)), 200)
                .at("/markedRead").asInt()).isEqualTo(2);
        assertThat(unread(AMIRA)).isZero();
    }

    @Test
    void onlyUnreadNarrowsTheList() throws Exception {
        publishTwoAnnouncements();
        long first = bodyOf(get("/api/v1/my/notifications", token(AMIRA)), 200)
                .at("/content/0/id").asLong();
        post("/api/v1/my/notifications/" + first + "/read", token(AMIRA));

        assertThat(bodyOf(get("/api/v1/my/notifications?onlyUnread=true", token(AMIRA)), 200)
                .at("/totalElements").asLong()).isEqualTo(1);
        assertThat(bodyOf(get("/api/v1/my/notifications", token(AMIRA)), 200)
                .at("/totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void somebodyElsesNotificationCannotBeMarkedRead() throws Exception {
        publishTwoAnnouncements();
        long amiras = bodyOf(get("/api/v1/my/notifications", token(AMIRA)), 200)
                .at("/content/0/id").asLong();

        assertThat(post("/api/v1/my/notifications/" + amiras + "/read", token(SECOND_AS)))
                .hasStatus(403);
    }

    // ---------- the activity feed ----------

    @Test
    void theFeedRecordsWhatHappenedAndStaysWithinTheStudentsLevel() throws Exception {
        long submissionId = handInAsPair();
        post("/api/v1/submissions/" + submissionId + "/grade", token(ADMIN), """
                {"grade":16}""");

        JsonNode feed = bodyOf(get("/api/v1/activity", token(AMIRA)), 200);
        assertThat(feed.at("/totalElements").asLong()).isEqualTo(1);
        assertThat(feed.at("/content/0/type").asText()).isEqualTo("SUBMISSION_GRADED");
        assertThat(feed.at("/content/0/courseCode").asText()).isEqualTo("INF201");

        assertThat(bodyOf(get("/api/v1/activity", token(SECOND_AS)), 200).at("/totalElements").asLong())
                .describedAs("another level's activity is not theirs to see").isZero();
        assertThat(bodyOf(get("/api/v1/activity", token(ADMIN)), 200).at("/totalElements").asLong())
                .describedAs("an admin sees everything").isEqualTo(1);
    }

    // ---------- helpers ----------

    private void publishTwoAnnouncements() throws Exception {
        for (String title : new String[]{"Annonce 1", "Annonce 2"}) {
            long id = bodyOf(post("/api/v1/announcements", token(ADMIN), """
                    {"title":"%s","body":"Contenu","level":"THIRD_AS"}""".formatted(title)), 201)
                    .at("/id").asLong();
            post("/api/v1/announcements/" + id + "/publish", token(ADMIN));
        }
    }

    private long handInAsPair() throws Exception {
        long id = bodyOf(put("/api/v1/assignments/" + tpId + "/submission", token(AMIRA), """
                {"partnerId":%d}""".formatted(yacineId)), 200).at("/id").asLong();
        MockMultipartFile file = new MockMultipartFile("file", "travail.pdf", "application/pdf",
                "contenu".getBytes(StandardCharsets.UTF_8));
        mvc.post().uri("/api/v1/assignments/" + tpId + "/submission/files").multipart().file(file)
                .header("Authorization", "Bearer " + token(AMIRA)).exchange();
        post("/api/v1/assignments/" + tpId + "/submission/submit", token(AMIRA), null);
        return id;
    }

    private long unread(String email) throws Exception {
        return bodyOf(get("/api/v1/my/notifications/unread-count", token(email)), 200)
                .at("/unread").asLong();
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
}
