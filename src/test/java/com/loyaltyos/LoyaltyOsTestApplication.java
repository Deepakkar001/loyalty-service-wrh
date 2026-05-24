package com.loyaltyos;

import org.springframework.boot.SpringBootConfiguration;

/**
 * Anchor for {@code @WebMvcTest} and other slice tests in promoted modules
 * ({@code rules}, {@code rewards}, {@code analytics}) that are no longer
 * under {@code com.loyaltyos.onboarding}.
 */
@SpringBootConfiguration
public class LoyaltyOsTestApplication {
}
