package com.loyaltyos.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.loyaltyos.onboarding", "com.loyaltyos.campaigns",
    "com.loyaltyos.rules", "com.loyaltyos.rewards", "com.loyaltyos.analytics",
    "com.loyaltyos.integration", "com.loyaltyos.voucher", "com.loyaltyos.referrals",
    "com.loyaltyos.support"
})
@EntityScan(basePackages = {
    "com.loyaltyos.onboarding", "com.loyaltyos.campaigns",
    "com.loyaltyos.rules", "com.loyaltyos.rewards", "com.loyaltyos.analytics",
    "com.loyaltyos.integration", "com.loyaltyos.voucher", "com.loyaltyos.referrals",
    "com.loyaltyos.support"
})
@EnableJpaRepositories(basePackages = {
    "com.loyaltyos.onboarding", "com.loyaltyos.campaigns",
    "com.loyaltyos.rules", "com.loyaltyos.rewards", "com.loyaltyos.analytics",
    "com.loyaltyos.integration", "com.loyaltyos.voucher", "com.loyaltyos.referrals",
    "com.loyaltyos.support"
})
@EnableScheduling
public class TenantOnboardingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantOnboardingApplication.class, args);
    }
}

