package com.example.education_platform.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.education_platform.access.entity.AccessRequest;
import com.example.education_platform.access.repository.AccessRequestRepository;
import com.example.education_platform.assignment.entity.Assignment;
import com.example.education_platform.assignment.entity.AssignmentStatus;
import com.example.education_platform.assignment.entity.AssignmentType;
import com.example.education_platform.assignment.entity.WorkMode;
import com.example.education_platform.assignment.repository.AssignmentRepository;
import com.example.education_platform.course.entity.Course;
import com.example.education_platform.course.repository.CourseRepository;
import com.example.education_platform.mail.service.EmailService;
import com.example.education_platform.notification.job.DeadlineReminderJob;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/** The moments that send a mail, checked against a mocked sender rather than a real inbox. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmailTriggersIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PASSWORD = "Correct-Horse-1";
    private static final String ADMIN = "prof@eduflow.dz";
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private AccessRequestRepository requests;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private DeadlineReminderJob reminderJob;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User amira;

    @BeforeEach
    void seed() {
        users.saveAndFlush(new User("Karim Haddad", ADMIN,
                passwordEncoder.encode(PASSWORD), Role.ADMIN, null));
        amira = users.saveAndFlush(new User("Amira Benali", "amira@eduflow.dz",
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
    }

    @Test
    void rejectingAnAccessRequestTellsThePersonWhy() {
        AccessRequest request = requests.saveAndFlush(
                new AccessRequest("Yacine Cherif", "yacine@eduflow.dz", Level.THIRD_AS, null));

        assertThat(post("/api/v1/access-requests/" + request.getId() + "/reject", """
                {"note":"Classe complète"}""")).hasStatus(200);

        verify(emailService).sendAccessRejected("yacine@eduflow.dz", "Yacine Cherif", "Classe complète");
    }

    @Test
    void anAdminPasswordResetMailsTheTemporaryPasswordItReturns() throws Exception {
        MvcTestResult result = post("/api/v1/users/" + amira.getId() + "/reset-password", null);

        String temporary = bodyOf(result, 200).get("temporaryPassword").asText();
        verify(emailService).sendTemporaryPassword("amira@eduflow.dz", "Amira Benali", temporary);
    }

    @Test
    void aStudentWhoHasNotHandedInIsRemindedByMailToo() {
        Assignment due = dueIn(NOW.plus(24, ChronoUnit.HOURS).plusSeconds(60));

        assertThat(reminderJob.remind(NOW)).isEqualTo(1);

        verify(emailService).sendDeadlineReminder(eq("amira@eduflow.dz"), eq("Amira Benali"),
                eq("TP"), eq(due.getDeadline()), anyString());
    }

    @Test
    void workDueNextWeekSendsNoMailYet() {
        dueIn(NOW.plus(7, ChronoUnit.DAYS));

        reminderJob.remind(NOW);

        verify(emailService, never()).sendDeadlineReminder(any(), any(), any(), any(), any());
    }

    @Test
    void aMailThatFailsDoesNotCancelTheInAppReminder() {
        dueIn(NOW.plus(24, ChronoUnit.HOURS).plusSeconds(60));
        doThrow(new IllegalStateException("SMTP down")).when(emailService)
                .sendDeadlineReminder(any(), any(), any(), any(), any());

        assertThat(reminderJob.remind(NOW))
                .describedAs("the notification still counts when the mail could not go").isEqualTo(1);
    }

    private Assignment dueIn(Instant deadline) {
        Course course = courses.saveAndFlush(
                new Course("INF201", "Algorithmique", null, Level.THIRD_AS, "#16A34A"));
        Assignment assignment = new Assignment(course, "TP", AssignmentType.TP, WorkMode.INDIVIDUAL);
        assignment.setStatus(AssignmentStatus.OPEN);
        assignment.setDeadline(deadline);
        return assignments.saveAndFlush(assignment);
    }

    private String token() {
        try {
            return bodyOf(mvc.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"%s","password":"%s"}""".formatted(ADMIN, PASSWORD)).exchange(), 200)
                    .at("/tokens/accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("could not log in as admin", e);
        }
    }

    private JsonNode bodyOf(MvcTestResult result, int expectedStatus) throws Exception {
        assertThat(result).hasStatus(expectedStatus);
        return JSON.readTree(result.getResponse().getContentAsString());
    }

    private MvcTestResult post(String uri, String body) {
        var request = mvc.post().uri(uri).header("Authorization", "Bearer " + token());
        if (body != null) {
            request = request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return request.exchange();
    }
}
