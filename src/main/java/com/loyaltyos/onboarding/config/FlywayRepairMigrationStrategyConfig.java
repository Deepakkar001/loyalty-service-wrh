package com.loyaltyos.onboarding.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Opt-in local/dev escape hatch:
 * If a migration was marked as failed (e.g. power loss / crash mid-run), Flyway validation blocks app startup.
 * Enabling this property runs Flyway repair before migrate once.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.flyway", name = "repair-on-startup", havingValue = "true")
public class FlywayRepairMigrationStrategyConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairMigrationStrategyConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayRepairThenMigrate() {
        return (Flyway flyway) -> {
            log.warn("Flyway repair-on-startup is ENABLED. Running repair() then migrate(). Disable after first successful start.");
            flyway.repair();
            flyway.migrate();
        };
    }
}

