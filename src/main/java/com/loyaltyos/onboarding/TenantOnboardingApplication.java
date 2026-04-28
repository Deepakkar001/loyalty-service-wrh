package com.loyaltyos.onboarding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TenantOnboardingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantOnboardingApplication.class, args);
    }
}

