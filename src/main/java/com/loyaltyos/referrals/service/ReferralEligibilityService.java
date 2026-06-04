package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.model.ReferralEligibilityRules;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.rewards.repository.PointsLedgerRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReferralEligibilityService {

    private final PointsLedgerRepository pointsLedgerRepository;

    public ReferralEligibilityService(PointsLedgerRepository pointsLedgerRepository) {
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
    }

    public void assertReferrerEligible(
        String tenantId,
        String programmeUid,
        String referrerCustomerId,
        ReferralProgrammeConfig config
    ) {
        ReferralEligibilityRules rules = config != null && config.getEligibility() != null
            ? config.getEligibility()
            : new ReferralEligibilityRules();
        if (!rules.isReferrerMustHaveLedgerActivity()) {
            return;
        }
        if (!hasLedgerActivity(tenantId, programmeUid, referrerCustomerId)) {
            throw new ReferralException(
                "REFERRER_NOT_ENROLLED",
                "Referrer must have existing loyalty activity before referring"
            );
        }
    }

    public void assertRefereeEligible(
        String tenantId,
        String programmeUid,
        String refereeCustomerId,
        ReferralProgrammeConfig config
    ) {
        ReferralEligibilityRules rules = config != null && config.getEligibility() != null
            ? config.getEligibility()
            : new ReferralEligibilityRules();
        if (!rules.isRefereeMustBeNewCustomer()) {
            return;
        }
        if (hasLedgerActivity(tenantId, programmeUid, refereeCustomerId)) {
            throw new ReferralException(
                "REFEREE_NOT_NEW",
                "Referee already has loyalty activity and cannot qualify as a new referral"
            );
        }
    }

    private boolean hasLedgerActivity(String tenantId, String programmeUid, String customerId) {
        return pointsLedgerRepository.existsByTenantIdAndProgrammeUidAndCustomerId(
            tenantId, programmeUid, customerId
        );
    }
}
