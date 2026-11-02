package com.example.education_platform.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * The test suite runs on the test profile, so a typo in application-prod.yml only shows up when the
 * server starts in production. This binds the production pool settings the way Spring Boot does.
 */
class ProductionConfigTest {

    @Test
    void theProductionConnectionPoolSettingsBind() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader()
                .load("application-prod", new ClassPathResource("application-prod.yml"))
                .forEach(environment.getPropertySources()::addFirst);

        try (HikariDataSource pool = Binder.get(environment)
                .bindOrCreate("spring.datasource.hikari", HikariDataSource.class)) {
            assertThat(pool.getIdleTimeout()).isEqualTo(30_000);
            assertThat(pool.getMinimumIdle()).isZero();
            assertThat(pool.getMaximumPoolSize()).isEqualTo(5);
        }
    }
}
