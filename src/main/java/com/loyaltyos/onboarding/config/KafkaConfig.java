package com.loyaltyos.onboarding.config;

/**
 * Kafka is intentionally disabled until we need event fan-out at scale.
 * <p>
 * <b>To re-enable:</b> uncomment the {@code @Configuration} class below; restore
 * {@code spring.kafka.*} in {@code application.yml}; remove {@code KafkaAutoConfiguration}
 * from {@code spring.autoconfigure.exclude}; uncomment {@code spring-kafka} in
 * {@code build.gradle}; restore {@code KafkaTemplate} + {@code send(...)} in
 * {@code ProgrammeService}, {@code TenantConfigService}, and {@code GoLiveService}.
 */
/*
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic platformConfigUpdatesTopic() {
        return new NewTopic("platform.config.updates", 8, (short) 1);
    }
}
*/

/** Marker type so this package stays explicit in the codebase (no active Kafka beans). */
public final class KafkaConfig {
    private KafkaConfig() {}
}
