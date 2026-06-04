package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.enums.ReferralStatus;
import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.referrals.model.ReferralCapRule;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.repository.ReferralRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ReferralCapService {

    private static final List<ReferralStatus> COUNTED_STATUSES =
        List.of(ReferralStatus.SIGNED_UP, ReferralStatus.REWARDED);

    private final ReferralRepository referralRepository;

    public ReferralCapService(ReferralRepository referralRepository) {
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");
    }

    public void assertReferrerWithinCaps(
        String tenantId,
        String programmeUid,
        String referrerCustomerId,
        ReferralProgramme programme,
        ReferralProgrammeConfig config
    ) {
        List<ReferralCapRule> rules = config.getCapRules();
        if (rules == null || rules.isEmpty()) {
            long lifetime = referralRepository.countByTenantIdAndProgrammeUidAndReferrerCustomerIdAndStatusIn(
                tenantId, programmeUid, referrerCustomerId, COUNTED_STATUSES
            );
            if (lifetime >= programme.getMaxReferralsPerCustomer()) {
                throw new ReferralException("REFERRAL_CAP_EXCEEDED", "Referrer has reached the referral cap");
            }
            return;
        }

        for (ReferralCapRule rule : rules) {
            int max = resolveMax(rule, programme);
            if (max <= 0) {
                continue;
            }
            long count = countForRule(tenantId, programmeUid, referrerCustomerId, rule);
            if (count >= max) {
                throw new ReferralException(
                    "REFERRAL_CAP_EXCEEDED",
                    "Referrer cap exceeded: " + rule.getType().name()
                );
            }
        }
    }

    private static int resolveMax(ReferralCapRule rule, ReferralProgramme programme) {
        if (rule.getMaxCount() != null) {
            return rule.getMaxCount();
        }
        if (rule.getType() == ReferralCapRule.CapType.LIFETIME_REFERRALS) {
            return programme.getMaxReferralsPerCustomer();
        }
        return 0;
    }

    private long countForRule(String tenantId, String programmeUid, String referrerCustomerId, ReferralCapRule rule) {
        Instant since = switch (rule.getType()) {
            case LIFETIME_REFERRALS -> null;
            case CALENDAR_MONTH_REFERRALS -> startOfCurrentMonthUtc();
            case ROLLING_DAY_REFERRALS -> {
                int days = rule.getWindowDays() != null && rule.getWindowDays() > 0 ? rule.getWindowDays() : 30;
                yield Instant.now().minus(days, ChronoUnit.DAYS);
            }
        };
        if (since == null) {
            return referralRepository.countByTenantIdAndProgrammeUidAndReferrerCustomerIdAndStatusIn(
                tenantId, programmeUid, referrerCustomerId, COUNTED_STATUSES
            );
        }
        return referralRepository.countByTenantIdAndProgrammeUidAndReferrerCustomerIdAndStatusInAndCreatedAtAfter(
            tenantId, programmeUid, referrerCustomerId, COUNTED_STATUSES, since
        );
    }

    private static Instant startOfCurrentMonthUtc() {
        ZonedDateTime start = ZonedDateTime.now(ZoneOffset.UTC)
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);
        return start.toInstant();
    }
}
