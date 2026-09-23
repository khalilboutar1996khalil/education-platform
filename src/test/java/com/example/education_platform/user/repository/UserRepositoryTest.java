package com.example.education_platform.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.education_platform.common.config.JpaAuditingConfig;
import com.example.education_platform.user.entity.Level;
import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.entity.UserStatus;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Runs against the real Postgres test database with Flyway applied, so it also proves the V2
 * migration matches the entity — Hibernate's {@code ddl-auto: validate} fails the context otherwise.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository repository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void savesStudentAndFindsItByEmail() {
        repository.saveAndFlush(student("amira@eduflow.dz"));

        assertThat(repository.findByEmail("amira@eduflow.dz"))
                .hasValueSatisfying(found -> {
                    assertThat(found.getFullName()).isEqualTo("Amira Benali");
                    assertThat(found.getRole()).isEqualTo(Role.STUDENT);
                    assertThat(found.getLevel()).isEqualTo(Level.THIRD_AS);
                    assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
                    assertThat(found.getLocale()).isEqualTo("fr");
                    assertThat(found.isNotifyByEmail()).isTrue();
                });
    }

    @Test
    void fillsAuditColumnsFromTheAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                "prof@eduflow.dz", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        User saved = repository.saveAndFlush(student("karim@eduflow.dz"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedBy()).isEqualTo("prof@eduflow.dz");
        assertThat(saved.getUpdatedBy()).isEqualTo("prof@eduflow.dz");
        assertThat(saved.isDeleted()).isFalse();
    }

    @Test
    void fallsBackToSystemAuditorWhenNobodyIsAuthenticated() {
        User saved = repository.saveAndFlush(student("seed@eduflow.dz"));

        assertThat(saved.getCreatedBy()).isEqualTo("system");
    }

    @Test
    void rejectsADuplicateEmail() {
        repository.saveAndFlush(student("dup@eduflow.dz"));

        assertThat(repository.existsByEmail("dup@eduflow.dz")).isTrue();
        assertThatThrownBy(() -> repository.saveAndFlush(student("dup@eduflow.dz")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAStudentWithoutALevel() {
        User noLevel = new User("Sans Niveau", "nolevel@eduflow.dz", "hash", Role.STUDENT, null);

        assertThatThrownBy(() -> repository.saveAndFlush(noLevel))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsAnAdminCarryingALevel() {
        User adminWithLevel = new User("Prof", "prof2@eduflow.dz", "hash", Role.ADMIN, Level.THIRD_AS);

        assertThatThrownBy(() -> repository.saveAndFlush(adminWithLevel))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static User student(String email) {
        return new User("Amira Benali", email, "not-a-real-hash", Role.STUDENT, Level.THIRD_AS);
    }
}
