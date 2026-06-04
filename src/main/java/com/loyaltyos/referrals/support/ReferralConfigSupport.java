package com.loyaltyos.referrals.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import java.util.Objects;

public final class ReferralConfigSupport {

    private ReferralConfigSupport() {}

    public static ReferralProgrammeConfig parseConfig(ObjectMapper objectMapper, String json) {
        if (json == null || json.isBlank()) {
            return new ReferralProgrammeConfig();
        }
        try {
            return objectMapper.readValue(json, ReferralProgrammeConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid referral programme config JSON", e);
        }
    }

    public static String toJson(ObjectMapper objectMapper, ReferralProgrammeConfig config) {
        Objects.requireNonNull(config, "config");
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize referral programme config", e);
        }
    }
}
