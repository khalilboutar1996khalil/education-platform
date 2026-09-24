package com.example.education_platform.security;

import com.example.education_platform.common.exception.TooManyLoginAttemptsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Throttles password guessing per email address, in memory.
 *
 * <p>In-memory means the counters reset on restart and are not shared between instances. That is
 * enough for a single-instance deployment; a second instance needs the counters in Postgres or
 * Redis instead, which is what the {@code login_attempts} table in the data model is for.
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    /** Above this many tracked addresses, expired entries are swept out on the next failure. */
    private static final int PURGE_THRESHOLD = 10_000;

    private final LoginRateLimitProperties properties;
    private final Map<String, Failures> byEmail = new ConcurrentHashMap<>();

    /** Called before the password is checked. */
    public void checkAllowed(String email) {
        Instant now = Instant.now();
        String key = key(email);
        Failures failures = byEmail.get(key);
        if (failures == null) {
            return;
        }
        if (failures.hasExpired(now, properties.lockout())) {
            byEmail.remove(key);
            return;
        }
        if (failures.count() >= properties.maxFailures()) {
            throw new TooManyLoginAttemptsException(failures.remainingLockout(now, properties.lockout()));
        }
    }

    public void recordFailure(String email) {
        Instant now = Instant.now();
        byEmail.compute(key(email), (ignored, existing) ->
                existing == null || existing.hasExpired(now, properties.lockout())
                        ? new Failures(1, now)
                        : new Failures(existing.count() + 1, now));
        if (byEmail.size() > PURGE_THRESHOLD) {
            byEmail.values().removeIf(failures -> failures.hasExpired(now, properties.lockout()));
        }
    }

    public void recordSuccess(String email) {
        byEmail.remove(key(email));
    }

    private static String key(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private record Failures(int count, Instant lastFailureAt) {

        boolean hasExpired(Instant now, Duration lockout) {
            return lastFailureAt.plus(lockout).isBefore(now);
        }

        Duration remainingLockout(Instant now, Duration lockout) {
            return Duration.between(now, lastFailureAt.plus(lockout));
        }
    }
}
