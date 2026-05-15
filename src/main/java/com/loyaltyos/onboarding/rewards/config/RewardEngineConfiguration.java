package com.loyaltyos.onboarding.rewards.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RewardEngineProperties.class)
public class RewardEngineConfiguration {
}
