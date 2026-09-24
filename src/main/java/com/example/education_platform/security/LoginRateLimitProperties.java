package com.example.education_platform.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param maxFailures consecutive failed logins tolerated for one email address
 * @param lockout     how long the address is refused once that count is reached
 */
@ConfigurationProperties("app.security.login")
public record LoginRateLimitProperties(int maxFailures, Duration lockout) {
}
