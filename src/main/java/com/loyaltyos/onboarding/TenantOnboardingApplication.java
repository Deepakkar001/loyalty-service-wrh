package com.loyaltyos.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.loyaltyos.onboarding", "com.loyaltyos.campaigns"})
@EntityScan(basePackages = {"com.loyaltyos.onboarding", "com.loyaltyos.campaigns"})
@EnableJpaRepositories(basePackages = {"com.loyaltyos.onboarding", "com.loyaltyos.campaigns"})
@EnableScheduling
public class TenantOnboardingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantOnboardingApplication.class, args);
    }
}

