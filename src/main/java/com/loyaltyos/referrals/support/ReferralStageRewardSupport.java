package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.model.ReferralPartyRewardConfig;
import com.loyaltyos.referrals.model.ReferralRewardType;
import com.loyaltyos.referrals.model.ReferralStageConfig;
import java.math.BigDecimal;

public final class ReferralStageRewardSupport {

    private ReferralStageRewardSupport() {}

    public static ReferralPartyRewardConfig resolveReferrerReward(ReferralStageConfig stage) {
        if (stage.getReferrerReward() != null) {
            return stage.getReferrerReward();
        }
        ReferralPartyRewardConfig cfg = new ReferralPartyRewardConfig();
        cfg.setType(ReferralRewardType.POINTS);
        cfg.setPoints(stage.getReferrerPoints() != null ? stage.getReferrerPoints() : BigDecimal.ZERO);
        return cfg;
    }

    public static ReferralPartyRewardConfig resolveRefereeReward(ReferralStageConfig stage) {
        if (stage.getRefereeReward() != null) {
            return stage.getRefereeReward();
        }
        ReferralPartyRewardConfig cfg = new ReferralPartyRewardConfig();
        cfg.setType(ReferralRewardType.POINTS);
        cfg.setPoints(stage.getRefereePoints() != null ? stage.getRefereePoints() : BigDecimal.ZERO);
        return cfg;
    }
}
