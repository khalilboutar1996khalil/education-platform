package com.example.education_platform.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * The full JPA wiring (createdBy/updatedBy actually landing on a row) is proved once a real
 * entity extends BaseEntity, starting with Step 2's User; this covers the auditor-resolution
 * rule itself in isolation.
 */
class JpaAuditingConfigTest {

    private final AuditorAware<String> auditorProvider = new JpaAuditingConfig().auditorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsSystemWhenNoAuthenticationIsPresent() {
        SecurityContextHolder.clearContext();

        assertThat(auditorProvider.getCurrentAuditor()).contains("system");
    }

    @Test
    void returnsSystemForTheAnonymousPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(authenticated("anonymousUser"));

        assertThat(auditorProvider.getCurrentAuditor()).contains("system");
    }

    @Test
    void returnsTheAuthenticatedPrincipalsName() {
        SecurityContextHolder.getContext().setAuthentication(authenticated("alice"));

        assertThat(auditorProvider.getCurrentAuditor()).contains("alice");
    }

    private static TestingAuthenticationToken authenticated(String principal) {
        var token = new TestingAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        token.setAuthenticated(true);
        return token;
    }
}
