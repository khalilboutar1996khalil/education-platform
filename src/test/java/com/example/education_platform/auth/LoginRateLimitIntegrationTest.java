package com.example.education_platform.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * The limiter is an in-memory singleton, so its counters survive the per-test rollback — each test
 * therefore uses its own email address. The threshold is lowered here rather than globally so the
 * rest of the suite, which fails logins freely, is never throttled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.security.login.max-failures=3",
        "app.security.login.lockout=15m"
})
class LoginRateLimitIntegrationTest {

    private static final String PASSWORD = "Correct-Horse-1";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void afterThreeFailuresTheAddressIsLockedOut() {
        String email = seedUser("locked@eduflow.dz");

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(login(email, "wrong-password"))
                    .describedAs("failure %d is still a plain 401", attempt).hasStatus(401);
        }

        MvcTestResult lockedOut = login(email, "wrong-password");
        assertThat(lockedOut).hasStatus(429);
        assertThat(lockedOut.getResponse().getHeader("Retry-After"))
                .describedAs("a 429 should say when to come back").isNotNull();
    }

    @Test
    void theLockoutIgnoresTheCorrectPasswordToo() {
        String email = seedUser("stubborn@eduflow.dz");

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(login(email, "wrong-password")).hasStatus(401);
        }

        assertThat(login(email, PASSWORD))
                .describedAs("guessing the password on the last try must not unlock the account")
                .hasStatus(429);
    }

    @Test
    void theResponseSaysWhyInFrench() {
        String email = seedUser("french@eduflow.dz");
        for (int attempt = 1; attempt <= 3; attempt++) {
            login(email, "wrong-password");
        }

        assertThat(login(email, "wrong-password"))
                .hasStatus(429)
                .bodyJson()
                .satisfies(body -> body.assertThat().extractingPath("$.detail")
                        .isEqualTo("Trop de tentatives de connexion. Veuillez réessayer plus tard"));
    }

    @Test
    void aSuccessfulLoginClearsTheCounter() {
        String email = seedUser("forgiven@eduflow.dz");

        assertThat(login(email, "wrong-password")).hasStatus(401);
        assertThat(login(email, "wrong-password")).hasStatus(401);
        assertThat(login(email, PASSWORD)).hasStatusOk();

        assertThat(login(email, "wrong-password"))
                .describedAs("the two earlier failures must not still count").hasStatus(401);
        assertThat(login(email, "wrong-password")).hasStatus(401);
    }

    @Test
    void theLimitIsPerAddress() {
        String locked = seedUser("noisy@eduflow.dz");
        String other = seedUser("quiet@eduflow.dz");

        for (int attempt = 1; attempt <= 4; attempt++) {
            login(locked, "wrong-password");
        }

        assertThat(login(locked, "wrong-password")).hasStatus(429);
        assertThat(login(other, PASSWORD))
                .describedAs("one throttled address must not lock anyone else out").hasStatusOk();
    }

    @Test
    void anUnknownAddressIsThrottledToo() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(login("ghost@eduflow.dz", "wrong-password")).hasStatus(401);
        }

        assertThat(login("ghost@eduflow.dz", "wrong-password"))
                .describedAs("otherwise unknown addresses are a free guessing oracle").hasStatus(429);
    }

    private String seedUser(String email) {
        users.saveAndFlush(new User("Test User", email,
                passwordEncoder.encode(PASSWORD), Role.STUDENT, Level.THIRD_AS));
        return email;
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password))
                .exchange();
    }
}
