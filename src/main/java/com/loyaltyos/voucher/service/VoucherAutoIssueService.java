package com.loyaltyos.voucher.service;

import com.loyaltyos.voucher.config.VoucherProperties;
import com.loyaltyos.voucher.dto.VoucherIssueResponse;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherAutoIssueService {

    private final VoucherIssueService voucherIssueService;
    private final VoucherProperties voucherProperties;

    public VoucherAutoIssueService(VoucherIssueService voucherIssueService, VoucherProperties voucherProperties) {
        this.voucherIssueService = Objects.requireNonNull(voucherIssueService, "voucherIssueService");
        this.voucherProperties = Objects.requireNonNull(voucherProperties, "voucherProperties");
    }

    public boolean isEnabled() {
        return voucherProperties.getAutoIssueFromRules() != null
            && voucherProperties.getAutoIssueFromRules().isEnabled();
    }

    /**
     * Issues a voucher code in an isolated transaction so failures don't roll back event earning.
     *
     * Important: do not swallow runtime exceptions inside this transaction; doing so can mark the transaction
     * rollback-only and cause an UnexpectedRollbackException. Catch and map exceptions at the call-site instead.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VoucherIssueResponse autoIssue(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        String customerId,
        String redemptionId,
        BigDecimal pointsToRedeem,
        BigDecimal faceValue
    ) {
        // Allow exceptions to propagate; caller will map them to a safe response without breaking points issuance.
        return voucherIssueService.issueVoucher(
            tenantId,
            programmeUid,
            catalogRewardUid,
            customerId,
            redemptionId,
            pointsToRedeem,
            faceValue
        );
    }
}

