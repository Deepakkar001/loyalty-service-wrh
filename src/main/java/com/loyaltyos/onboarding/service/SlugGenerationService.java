package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.repository.TenantOnboardingRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class SlugGenerationService {

    private final TenantOnboardingRepository tenantRepository;
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final int MAX_SLUG_LENGTH = 90;

    public SlugGenerationService(TenantOnboardingRepository tenantRepository) {
        this.tenantRepository = Objects.requireNonNull(tenantRepository, "tenantRepository");
    }

    public String generateUniqueSlug(String companyName) {
        String baseSlug = toSlug(companyName);
        String candidate = baseSlug;
        int suffix = 2;

        while (tenantRepository.existsBySlug(candidate)) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }
        return candidate;
    }

    private String toSlug(String input) {
        String normalized = Normalizer.normalize(input.toLowerCase().trim(), Normalizer.Form.NFD);
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        slug = slug.replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "tenant" : slug;
    }
}

