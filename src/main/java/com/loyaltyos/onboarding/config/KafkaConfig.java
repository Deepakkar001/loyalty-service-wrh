package com.loyaltyos.onboarding.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for tenant-onboarding-service.
 *
 * This service PRODUCES to:
 *   - platform.config.updates   : when a tenant is activated (Go-Live, Stage 6)
 *
 * Auto-create is disabled in local Docker and production.
 * All topics must be explicitly provisioned.
 */
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

    /**
     * platform.config.updates — published when tenant activates (Go-Live)
     * 8 partitions as per v3.0 architecture spec.
     */
    @Bean
    public NewTopic platformConfigUpdatesTopic() {
        // In production: replication factor = 3. Using 1 for local single-broker.
        return new NewTopic("platform.config.updates", 8, (short) 1);
    }
}

