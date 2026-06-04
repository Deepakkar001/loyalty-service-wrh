package com.loyaltyos.referrals.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.loyaltyos.referrals.dto.ReferralIssuanceLine;
import com.loyaltyos.rewards.dto.RewardIssueCommandDto;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReferralIssuanceSupportTest {

    @Test
    void sumPoints_splitsByCustomer() {
        RewardIssueCommandDto refereeCmd = new RewardIssueCommandDto();
        refereeCmd.setPointsToAward(new BigDecimal("50"));
        RewardIssueCommandDto referrerCmd = new RewardIssueCommandDto();
        referrerCmd.setPointsToAward(new BigDecimal("200"));

        List<ReferralIssuanceLine> lines = List.of(
            new ReferralIssuanceLine("referee-002", refereeCmd),
            new ReferralIssuanceLine("cust_125", referrerCmd)
        );

        assertThat(ReferralIssuanceSupport.sumPointsForCustomer(lines, "referee-002"))
            .isEqualByComparingTo("50");
        assertThat(ReferralIssuanceSupport.sumPointsExcludingCustomer(lines, "referee-002"))
            .isEqualByComparingTo("200");
    }
}
