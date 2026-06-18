package com.loyaltyos.merchants.service;

import com.loyaltyos.merchants.dto.MerchantPendingConfigApprovalResponse;
import com.loyaltyos.merchants.dto.MerchantPendingFinanceAgreementResponse;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.entity.MerchantAgreement;
import com.loyaltyos.merchants.entity.MerchantApprovalRequest;
import com.loyaltyos.merchants.enums.MerchantAgreementStatus;
import com.loyaltyos.merchants.enums.MerchantApprovalRequestType;
import com.loyaltyos.merchants.enums.MerchantApprovalStatus;
import com.loyaltyos.merchants.repository.MerchantAgreementRepository;
import com.loyaltyos.merchants.repository.MerchantApprovalRequestRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantGovernanceService {

    private final MerchantAgreementRepository agreementRepository;
    private final MerchantApprovalRequestRepository approvalRequestRepository;
    private final MerchantRepository merchantRepository;

    public MerchantGovernanceService(
        MerchantAgreementRepository agreementRepository,
        MerchantApprovalRequestRepository approvalRequestRepository,
        MerchantRepository merchantRepository
    ) {
        this.agreementRepository = Objects.requireNonNull(agreementRepository, "agreementRepository");
        this.approvalRequestRepository = Objects.requireNonNull(approvalRequestRepository, "approvalRequestRepository");
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
    }

    @Transactional(readOnly = true)
    public List<MerchantPendingFinanceAgreementResponse> listPendingFinanceAgreements(String tenantId) {
        Map<String, Merchant> merchantsByUid = merchantRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            .stream()
            .collect(Collectors.toMap(Merchant::getMerchantUid, m -> m, (a, b) -> a));
        return agreementRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(
                tenantId, MerchantAgreementStatus.PENDING_FINANCE)
            .stream()
            .map(agreement -> toFinanceResponse(agreement, merchantsByUid.get(agreement.getMerchantUid())))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<MerchantPendingConfigApprovalResponse> listPendingConfigApprovals(String tenantId) {
        Map<String, Merchant> merchantsByUid = merchantRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
            .stream()
            .collect(Collectors.toMap(Merchant::getMerchantUid, m -> m, (a, b) -> a));
        return approvalRequestRepository.findByTenantIdAndStatusOrderByRequestedAtDesc(
                tenantId, MerchantApprovalStatus.PENDING)
            .stream()
            .filter(req -> req.getRequestType() == MerchantApprovalRequestType.CONFIG_UPDATE)
            .map(req -> toConfigResponse(req, merchantsByUid.get(req.getMerchantUid())))
            .toList();
    }

    private static MerchantPendingFinanceAgreementResponse toFinanceResponse(
        MerchantAgreement agreement,
        Merchant merchant
    ) {
        MerchantPendingFinanceAgreementResponse response = new MerchantPendingFinanceAgreementResponse();
        response.setAgreementUid(agreement.getAgreementUid());
        response.setMerchantUid(agreement.getMerchantUid());
        response.setMerchantLegalName(merchant != null ? merchant.getLegalName() : agreement.getMerchantUid());
        response.setTermsVersion(agreement.getTermsVersion());
        response.setEffectiveDate(agreement.getEffectiveDate());
        response.setRevenueSharePct(agreement.getRevenueSharePct());
        response.setSettlementCycle(
            agreement.getSettlementCycle() != null ? agreement.getSettlementCycle().name() : null);
        response.setProposedEarnRateMultiplier(agreement.getProposedEarnRateMultiplier());
        response.setSubmittedByEmail(agreement.getSubmittedByEmail());
        response.setSignedAt(agreement.getSignedAt());
        return response;
    }

    private static MerchantPendingConfigApprovalResponse toConfigResponse(
        MerchantApprovalRequest request,
        Merchant merchant
    ) {
        MerchantPendingConfigApprovalResponse response = new MerchantPendingConfigApprovalResponse();
        response.setRequestUid(request.getRequestUid());
        response.setMerchantUid(request.getMerchantUid());
        response.setMerchantLegalName(merchant != null ? merchant.getLegalName() : request.getMerchantUid());
        response.setRequestedBy(request.getRequestedBy());
        response.setRequestedAt(request.getRequestedAt());
        response.setPayloadJson(request.getPayloadJson());
        return response;
    }
}
