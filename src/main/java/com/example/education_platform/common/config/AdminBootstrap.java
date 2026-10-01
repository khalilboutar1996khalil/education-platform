package com.example.education_platform.common.config;

import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator on a database that has none, so a fresh production install can
 * be logged into. Runs under every profile but does nothing once any admin exists: changing
 * ADMIN_PASSWORD later never overwrites a password, and the variables can be removed after the
 * first start.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    static final int MIN_PASSWORD_LENGTH = 10;

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);
    private static final String DEFAULT_FULL_NAME = "Administrateur";

    private final AdminBootstrapProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.existsByRole(Role.ADMIN)) {
            return;
        }
        if (!properties.isConfigured()) {
            LOG.warn("No administrator exists and ADMIN_EMAIL / ADMIN_PASSWORD are not set: "
                    + "nobody can log in to manage the platform");
            return;
        }
        if (properties.password().length() < MIN_PASSWORD_LENGTH) {
            // Fail the start rather than publish a platform guarded by a weak password
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) {
            throw new IllegalStateException("ADMIN_EMAIL " + email + " already belongs to a non-admin account");
        }
        String fullName = properties.fullName() == null || properties.fullName().isBlank()
                ? DEFAULT_FULL_NAME : properties.fullName().trim();
        users.save(new User(fullName, email, passwordEncoder.encode(properties.password()), Role.ADMIN, null));
        LOG.info("First administrator {} created", email);
    }
}
