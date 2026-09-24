package com.example.education_platform.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The dashboard is the only cached thing: it runs a dozen aggregates and is hit on every page
 * load, while being perfectly happy a minute out of date.
 *
 * <p>Caffeine rather than the plain in-memory manager, which has no expiry at all — without a TTL
 * the numbers would freeze at whatever they were when the app started.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DASHBOARD_CACHE = "dashboard";

    /** Short enough that nobody notices the lag, long enough to absorb a burst of refreshes. */
    private static final Duration TTL = Duration.ofSeconds(60);

    @Bean
    CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(DASHBOARD_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(TTL)
                .maximumSize(1_000));
        return manager;
    }
}
