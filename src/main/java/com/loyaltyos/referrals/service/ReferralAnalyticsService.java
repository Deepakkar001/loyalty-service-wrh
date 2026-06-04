package com.loyaltyos.referrals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.referrals.dto.ReferralTimeToPurchaseResponse;
import com.loyaltyos.referrals.dto.ReferralTopReferrerResponse;
import com.loyaltyos.referrals.dto.ReferralTrendPointResponse;
import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.enums.ReferralStatus;
import com.loyaltyos.referrals.repository.ReferralRepository;
import com.loyaltyos.referrals.support.ReferralProgressTracker;
import com.loyaltyos.referrals.support.ReferralProgressTracker.ProgressState;
import com.loyaltyos.referrals.support.ReferralProgressTracker.PurchaseEvent;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralAnalyticsService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);

    private final ReferralRepository referralRepository;
    private final ObjectMapper objectMapper;

    public ReferralAnalyticsService(ReferralRepository referralRepository, ObjectMapper objectMapper) {
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional(readOnly = true)
    public List<ReferralTrendPointResponse> trends(
        String tenantId,
        String programmeUid,
        String granularity,
        int days
    ) {
        String programme = normalize(programmeUid);
        int windowDays = Math.min(Math.max(days, 1), 365);
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        boolean weekly = "WEEKLY".equalsIgnoreCase(granularity);

        List<Referral> referrals = referralRepository.findByTenantIdAndProgrammeUid(tenantId, programme).stream()
            .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(since))
            .toList();

        Map<String, ReferralTrendPointResponse> buckets = new LinkedHashMap<>();
        for (Referral r : referrals) {
            String key = bucketKey(r.getCreatedAt(), weekly);
            ReferralTrendPointResponse point = buckets.computeIfAbsent(key, k -> {
                ReferralTrendPointResponse p = new ReferralTrendPointResponse();
                p.setPeriodStart(k);
                return p;
            });
            point.setReferrals(point.getReferrals() + 1);
            if (r.getStatus() == ReferralStatus.REWARDED) {
                point.setRewarded(point.getRewarded() + 1);
            }
        }

        List<ReferralTrendPointResponse> sorted = new ArrayList<>(buckets.values());
        sorted.sort(Comparator.comparing(ReferralTrendPointResponse::getPeriodStart));
        return sorted;
    }

    @Transactional(readOnly = true)
    public List<ReferralTopReferrerResponse> topReferrers(String tenantId, String programmeUid, int limit) {
        String programme = normalize(programmeUid);
        int top = Math.min(Math.max(limit, 1), 50);

        Map<String, ReferralTopReferrerResponse> byReferrer = new HashMap<>();
        for (Referral r : referralRepository.findByTenantIdAndProgrammeUid(tenantId, programme)) {
            ReferralTopReferrerResponse row = byReferrer.computeIfAbsent(r.getReferrerCustomerId(), id -> {
                ReferralTopReferrerResponse t = new ReferralTopReferrerResponse();
                t.setReferrerCustomerId(id);
                return t;
            });
            row.setReferralCount(row.getReferralCount() + 1);
            if (r.getStatus() == ReferralStatus.REWARDED) {
                row.setRewardedCount(row.getRewardedCount() + 1);
            }
        }

        return byReferrer.values().stream()
            .sorted(Comparator.comparingLong(ReferralTopReferrerResponse::getReferralCount).reversed())
            .limit(top)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReferralTimeToPurchaseResponse timeToFirstPurchase(String tenantId, String programmeUid) {
        String programme = normalize(programmeUid);
        ReferralTimeToPurchaseResponse resp = new ReferralTimeToPurchaseResponse();
        double totalHours = 0;
        long samples = 0;

        for (Referral r : referralRepository.findByTenantIdAndProgrammeUid(tenantId, programme)) {
            if (r.getCreatedAt() == null) {
                continue;
            }
            ProgressState progress = ReferralProgressTracker.load(r, objectMapper);
            if (progress.getPurchases() == null || progress.getPurchases().isEmpty()) {
                if (r.getPurchaseCount() > 0 && r.getUpdatedAt() != null) {
                    totalHours += hoursBetween(r.getCreatedAt(), r.getUpdatedAt());
                    samples++;
                }
                continue;
            }
            PurchaseEvent first = progress.getPurchases().stream()
                .filter(p -> p.getAt() != null)
                .min(Comparator.comparing(PurchaseEvent::getAt))
                .orElse(null);
            if (first != null && first.getAt() != null) {
                totalHours += hoursBetween(r.getCreatedAt(), first.getAt());
                samples++;
            }
        }

        resp.setSampleSize(samples);
        resp.setAverageHoursToFirstPurchase(samples > 0 ? totalHours / samples : 0);
        return resp;
    }

    private static double hoursBetween(Instant start, Instant end) {
        return ChronoUnit.SECONDS.between(start, end) / 3600.0;
    }

    private static String bucketKey(Instant at, boolean weekly) {
        if (weekly) {
            return at.atZone(ZoneOffset.UTC).toLocalDate()
                .with(java.time.DayOfWeek.MONDAY)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return DAY_FMT.format(at);
    }

    private static String normalize(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }
}
