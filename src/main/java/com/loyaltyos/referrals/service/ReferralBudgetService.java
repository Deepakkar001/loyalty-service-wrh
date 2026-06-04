package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.model.ReferralPointsBudget;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.repository.ReferralRewardIssuedRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReferralBudgetService {

    private final ReferralRewardIssuedRepository rewardsIssuedRepository;

    public ReferralBudgetService(ReferralRewardIssuedRepository rewardsIssuedRepository) {
        this.rewardsIssuedRepository = Objects.requireNonNull(rewardsIssuedRepository, "rewardsIssuedRepository");
    }

    public void assertWithinBudget(
        String tenantId,
        String programmeUid,
        ReferralProgrammeConfig config,
        BigDecimal additionalPoints
    ) {
        if (config == null || config.getPointsBudget() == null) {
            return;
        }
        ReferralPointsBudget budget = config.getPointsBudget();
        if (budget.getMaxPoints() == null || budget.getMaxPoints().signum() <= 0) {
            return;
        }
        BigDecimal add = additionalPoints != null ? additionalPoints : BigDecimal.ZERO;
        if (add.signum() <= 0) {
            return;
        }

        Instant since = windowStart(budget);
        BigDecimal spent = rewardsIssuedRepository.sumPointsAwardedSince(tenantId, programmeUid, since);
        if (spent.add(add).compareTo(budget.getMaxPoints()) > 0) {
            throw new ReferralException(
                "REFERRAL_POINTS_BUDGET_EXCEEDED",
                "Referral programme points budget exceeded for period " + budget.getPeriod().name()
            );
        }
    }

    private static Instant windowStart(ReferralPointsBudget budget) {
        Instant now = Instant.now();
        return switch (budget.getPeriod()) {
            case LIFETIME -> Instant.EPOCH;
            case CALENDAR_MONTH -> {
                ZonedDateTime z = now.atZone(ZoneOffset.UTC);
                yield z.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0).toInstant();
            }
            case ROLLING_DAY -> {
                int days = budget.getWindowDays() != null && budget.getWindowDays() > 0
                    ? budget.getWindowDays()
                    : 30;
                yield now.minus(days, ChronoUnit.DAYS);
            }
        };
    }
}
