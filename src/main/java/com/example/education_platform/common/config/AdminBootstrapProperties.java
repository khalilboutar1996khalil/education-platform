package com.example.education_platform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * First administrator of a fresh production database, from ADMIN_EMAIL / ADMIN_PASSWORD /
 * ADMIN_FULL_NAME. Only read while the database has no admin at all.
 *
 * @param email    login of the admin to create; blank disables the bootstrap
 * @param password initial password, at least {@link AdminBootstrap#MIN_PASSWORD_LENGTH} characters
 * @param fullName name shown in the UI
 */
@ConfigurationProperties("app.bootstrap.admin")
public record AdminBootstrapProperties(String email, String password, String fullName) {

    boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }
}
