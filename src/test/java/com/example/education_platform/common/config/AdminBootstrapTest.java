package com.example.education_platform.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.education_platform.user.entity.Role;
import com.example.education_platform.user.entity.User;
import com.example.education_platform.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapTest {

    private static final String PASSWORD = "Str0ng-Admin-Pass";

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Test
    void createsTheFirstAdminOnAnEmptyDatabase() {
        bootstrap(new AdminBootstrapProperties("  Owner@School.DZ ", PASSWORD, "Khalil Boutar")).run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        User admin = saved.getValue();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getLevel()).isNull();
        assertThat(admin.getEmail()).isEqualTo("owner@school.dz");
        assertThat(admin.getFullName()).isEqualTo("Khalil Boutar");
        assertThat(passwordEncoder.matches(PASSWORD, admin.getPasswordHash())).isTrue();
    }

    @Test
    void fallsBackToADefaultNameWhenNoneIsGiven() {
        bootstrap(new AdminBootstrapProperties("owner@school.dz", PASSWORD, " ")).run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getFullName()).isEqualTo("Administrateur");
    }

    @Test
    void neverTouchesAnExistingAdmin() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(true);

        bootstrap(new AdminBootstrapProperties("owner@school.dz", PASSWORD, "Owner")).run(null);

        verify(users, never()).save(any());
    }

    @Test
    void doesNothingWhenNotConfigured() {
        bootstrap(new AdminBootstrapProperties("", "", null)).run(null);

        verify(users, never()).save(any());
    }

    @Test
    void refusesAWeakPassword() {
        AdminBootstrap bootstrap = bootstrap(new AdminBootstrapProperties("owner@school.dz", "short", "Owner"));

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 10");
        verify(users, never()).save(any());
    }

    @Test
    void refusesAnEmailAlreadyUsedByAStudent() {
        when(users.existsByEmail("amira@school.dz")).thenReturn(true);
        AdminBootstrap bootstrap = bootstrap(new AdminBootstrapProperties("amira@school.dz", PASSWORD, "Owner"));

        assertThatThrownBy(() -> bootstrap.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-admin");
        verify(users, never()).save(any());
    }

    private AdminBootstrap bootstrap(AdminBootstrapProperties properties) {
        return new AdminBootstrap(properties, users, passwordEncoder);
    }
}
