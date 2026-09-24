package com.example.education_platform.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Jobs run everywhere except the test profile, which switches them off: a job firing mid-test
 * would change data underneath whatever is being asserted. Tests call the job methods directly
 * instead, which is the behaviour worth checking anyway — the cron expression is not.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
