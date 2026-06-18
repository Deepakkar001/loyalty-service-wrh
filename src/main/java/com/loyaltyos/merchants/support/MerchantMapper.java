package com.loyaltyos.merchants.support;

import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.entity.Merchant;
import java.util.List;

public final class MerchantMapper {

    private static final List<String> ALL_CAPABILITIES =
        List.of("campaigns", "settlement", "integration");

    private MerchantMapper() {}

    public static MerchantResponse toResponse(Merchant merchant) {
        MerchantResponse r = new MerchantResponse();
        r.setMerchantUid(merchant.getMerchantUid());
        r.setLegalName(merchant.getLegalName());
        r.setDisplayName(merchant.getDisplayName());
        r.setCategory(merchant.getCategory());
        r.setContactEmail(merchant.getContactEmail());
        r.setOnboardingStage(merchant.getOnboardingStage().name());
        r.setEarnRateMultiplier(merchant.getEarnRateMultiplier());
        r.setSettlementCycle(merchant.getSettlementCycle().name());
        r.setAgreementAcceptedAt(merchant.getAgreementAcceptedAt());
        r.setIntegrationTestPassedAt(merchant.getIntegrationTestPassedAt());
        r.setActive(merchant.isActive());
        r.setSuspended(merchant.isSuspended());
        r.setCreatedAt(merchant.getCreatedAt());
        r.setEligibleCategoriesJson(merchant.getEligibleCategoriesJson());
        r.setCapabilities(parseCapabilities(merchant.getCapabilitiesJson()));
        return r;
    }

    /**
     * Deserializes the capabilities JSON array. Returns all capabilities when the column
     * is null (pre-migration rows or new merchants that have not yet had config applied).
     */
    @SuppressWarnings("unchecked")
    private static List<String> parseCapabilities(String capabilitiesJson) {
        if (capabilitiesJson == null || capabilitiesJson.isBlank()) {
            return ALL_CAPABILITIES;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            List<String> parsed = mapper.readValue(capabilitiesJson,
                mapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return parsed == null || parsed.isEmpty() ? ALL_CAPABILITIES : parsed;
        } catch (Exception e) {
            return ALL_CAPABILITIES;
        }
    }
}
