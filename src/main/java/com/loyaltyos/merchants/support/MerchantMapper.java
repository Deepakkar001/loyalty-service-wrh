package com.loyaltyos.merchants.support;

import com.loyaltyos.merchants.dto.MerchantResponse;
import com.loyaltyos.merchants.entity.Merchant;

public final class MerchantMapper {
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
        return r;
    }
}
