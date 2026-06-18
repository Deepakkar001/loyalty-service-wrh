package com.loyaltyos.merchants.support;

import com.loyaltyos.merchants.dto.MerchantAgreementResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantAgreement;

public final class MerchantAgreementMapper {

    private MerchantAgreementMapper() {}

    public static MerchantAgreementResponse toResponse(MerchantAgreement agreement) {
        MerchantAgreementResponse r = new MerchantAgreementResponse();
        r.setAgreementUid(agreement.getAgreementUid());
        r.setMerchantUid(agreement.getMerchantUid());
        r.setTermsVersion(agreement.getTermsVersion());
        r.setEffectiveDate(agreement.getEffectiveDate());
        r.setRevenueSharePct(agreement.getRevenueSharePct());
        r.setSettlementCycle(agreement.getSettlementCycle().name());
        r.setPointsCurrency(agreement.getPointsCurrency());
        r.setExpectedDailyTxnVolume(agreement.getExpectedDailyTxnVolume());
        r.setBillingContactName(agreement.getBillingContactName());
        r.setBillingAddress(agreement.getBillingAddress());
        r.setPaymentMethod(agreement.getPaymentMethod());
        r.setContractDurationMonths(agreement.getContractDurationMonths());
        r.setAutoRenewal(agreement.getAutoRenewal());
        r.setProposedEarnRateMultiplier(agreement.getProposedEarnRateMultiplier());
        r.setMerchantFundedCampaignsAllowed(agreement.isMerchantFundedCampaignsAllowed());
        r.setSignedByName(agreement.getSignedByName());
        r.setSignedByEmail(agreement.getSignedByEmail());
        r.setSignedByDesignation(agreement.getSignedByDesignation());
        r.setSignedAt(agreement.getSignedAt());
        r.setSubmittedByEmail(agreement.getSubmittedByEmail());
        r.setStatus(agreement.getStatus().name());
        return r;
    }

    public static com.loyaltyos.merchants.dto.MerchantAgreementPrefillResponse toPrefill(
        Merchant merchant,
        boolean hasExistingAgreement
    ) {
        com.loyaltyos.merchants.dto.MerchantAgreementPrefillResponse r =
            new com.loyaltyos.merchants.dto.MerchantAgreementPrefillResponse();
        r.setMerchantUid(merchant.getMerchantUid());
        r.setLegalName(merchant.getLegalName());
        r.setDisplayName(merchant.getDisplayName());
        r.setCategory(merchant.getCategory());
        r.setContactEmail(merchant.getContactEmail());
        r.setContactPhone(merchant.getContactPhone());
        r.setTaxId(merchant.getTaxId());
        r.setEarnRateMultiplier(merchant.getEarnRateMultiplier());
        r.setSettlementCycle(merchant.getSettlementCycle().name());
        r.setHasExistingAgreement(hasExistingAgreement);
        return r;
    }
}
