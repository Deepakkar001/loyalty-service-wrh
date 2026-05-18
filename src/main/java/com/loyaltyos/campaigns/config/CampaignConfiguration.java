package com.loyaltyos.campaigns.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CampaignProperties.class)
public class CampaignConfiguration {
}
