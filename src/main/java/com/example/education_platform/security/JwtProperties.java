package com.example.education_platform.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret            Base64-encoded signing key, at least 256 bits. Comes from JWT_SECRET.
 * @param accessTokenTtl    how long an access token stays valid — short, because it cannot be revoked
 * @param refreshTokenTtl   how long a refresh token stays valid — long, but revocable through the database
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
