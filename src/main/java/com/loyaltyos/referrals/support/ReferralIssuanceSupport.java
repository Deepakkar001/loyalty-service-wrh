package com.loyaltyos.referrals.support;

import com.loyaltyos.referrals.dto.ReferralIssuanceLine;
import com.loyaltyos.rewards.dto.RewardIssueCommandDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class ReferralIssuanceSupport {

    private ReferralIssuanceSupport() {}

    public static BigDecimal sumPointsForCustomer(List<ReferralIssuanceLine> lines, String customerId) {
        if (lines == null || lines.isEmpty() || customerId == null || customerId.isBlank()) {
            return BigDecimal.ZERO;
        }
        String target = customerId.trim();
        BigDecimal total = BigDecimal.ZERO;
        for (ReferralIssuanceLine line : lines) {
            if (line == null || line.getCustomerId() == null || line.getCommand() == null) {
                continue;
            }
            if (!target.equals(line.getCustomerId().trim())) {
                continue;
            }
            total = total.add(pointsFromCommand(line.getCommand()));
        }
        return total;
    }

    public static BigDecimal sumPointsExcludingCustomer(List<ReferralIssuanceLine> lines, String customerId) {
        if (lines == null || lines.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String target = customerId == null ? "" : customerId.trim();
        BigDecimal total = BigDecimal.ZERO;
        for (ReferralIssuanceLine line : lines) {
            if (line == null || line.getCustomerId() == null || line.getCommand() == null) {
                continue;
            }
            if (target.equals(line.getCustomerId().trim())) {
                continue;
            }
            total = total.add(pointsFromCommand(line.getCommand()));
        }
        return total;
    }

    private static BigDecimal pointsFromCommand(RewardIssueCommandDto command) {
        Objects.requireNonNull(command, "command");
        return command.getPointsToAward() != null ? command.getPointsToAward() : BigDecimal.ZERO;
    }
}
