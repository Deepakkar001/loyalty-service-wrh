package com.loyaltyos.integration.service;

import com.loyaltyos.integration.dto.IntegrationBalanceDetailResponse;
import com.loyaltyos.integration.dto.IntegrationBalanceResponse;
import com.loyaltyos.integration.dto.IntegrationTransactionResponse;
import com.loyaltyos.rewards.dto.LedgerTransactionDto;
import com.loyaltyos.rewards.dto.RewardBalanceDetailResponse;
import com.loyaltyos.rewards.dto.RewardBalanceResponse;
import com.loyaltyos.rewards.service.PointsLedgerQueryService;
import com.loyaltyos.rewards.service.RewardBalanceQueryService;
import com.loyaltyos.rewards.service.RewardIssuanceService;
import com.loyaltyos.rules.enums.LedgerEntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

@Service
public class IntegrationBalanceService {

    private final RewardIssuanceService rewardIssuanceService;
    private final RewardBalanceQueryService rewardBalanceQueryService;
    private final PointsLedgerQueryService pointsLedgerQueryService;

    public IntegrationBalanceService(
        RewardIssuanceService rewardIssuanceService,
        RewardBalanceQueryService rewardBalanceQueryService,
        PointsLedgerQueryService pointsLedgerQueryService
    ) {
        this.rewardIssuanceService = Objects.requireNonNull(rewardIssuanceService, "rewardIssuanceService");
        this.rewardBalanceQueryService = Objects.requireNonNull(rewardBalanceQueryService, "rewardBalanceQueryService");
        this.pointsLedgerQueryService = Objects.requireNonNull(pointsLedgerQueryService, "pointsLedgerQueryService");
    }

    public IntegrationBalanceResponse getBalance(String tenantId, String programmeUid, String customerId) {
        RewardBalanceResponse core = rewardIssuanceService.getBalance(tenantId, programmeUid, customerId);
        IntegrationBalanceResponse out = new IntegrationBalanceResponse();
        out.setTenantId(core.getTenantId());
        out.setProgrammeUid(core.getProgrammeUid());
        out.setCustomerId(core.getCustomerId());
        out.setBalance(core.getBalance());
        out.setLedgerDerivedBalance(core.getLedgerDerivedBalance());
        out.setUpdatedAt(core.getUpdatedAt());
        return out;
    }

    public IntegrationBalanceDetailResponse getBalanceDetail(
        String tenantId,
        String programmeUid,
        String customerId
    ) {
        RewardBalanceDetailResponse core = rewardBalanceQueryService.getBalanceDetail(
            tenantId, programmeUid, customerId
        );
        IntegrationBalanceDetailResponse out = new IntegrationBalanceDetailResponse();
        out.setTenantId(core.getTenantId());
        out.setProgrammeUid(core.getProgrammeUid());
        out.setCustomerId(core.getCustomerId());
        out.setBalance(core.getCachedBalance());
        out.setLedgerDerivedBalance(core.getLedgerDerivedBalance());
        out.setVariance(core.getVariance());
        out.setExpiringWithin7Days(core.getExpiringWithin7Days());
        return out;
    }

    public Page<IntegrationTransactionResponse> listTransactions(
        String tenantId,
        String programmeUid,
        String customerId,
        LedgerEntryType entryType,
        Instant from,
        Instant to,
        Pageable pageable
    ) {
        return pointsLedgerQueryService.listCustomerTransactions(
            tenantId, programmeUid, customerId, entryType, from, to, pageable
        ).map(IntegrationBalanceService::toTransactionResponse);
    }

    private static IntegrationTransactionResponse toTransactionResponse(LedgerTransactionDto row) {
        IntegrationTransactionResponse out = new IntegrationTransactionResponse();
        out.setLedgerId(row.getLedgerId());
        out.setIdempotencyKey(row.getIdempotencyKey());
        out.setEntryType(row.getEntryType());
        out.setPoints(row.getPoints());
        out.setSourceEventId(row.getSourceEventId());
        out.setDescription(row.getDescription());
        out.setCreatedAt(row.getCreatedAt());
        return out;
    }
}
