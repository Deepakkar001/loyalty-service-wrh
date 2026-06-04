package com.loyaltyos.referrals.service;

import com.loyaltyos.referrals.dto.ReferralDashboardResponse;
import com.loyaltyos.referrals.dto.ReferralListItemResponse;
import com.loyaltyos.referrals.entity.Referral;
import com.loyaltyos.referrals.entity.ReferralProgramme;
import com.loyaltyos.referrals.entity.ReferralRewardIssued;
import com.loyaltyos.referrals.enums.ReferralStatus;
import com.loyaltyos.referrals.model.ReferralProgrammeConfig;
import com.loyaltyos.referrals.repository.ReferralRepository;
import com.loyaltyos.referrals.repository.ReferralRewardIssuedRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReferralAdminService {

    private final ReferralProgrammeService programmeService;
    private final ReferralRepository referralRepository;
    private final ReferralRewardIssuedRepository rewardIssuedRepository;

    public ReferralAdminService(
        ReferralProgrammeService programmeService,
        ReferralRepository referralRepository,
        ReferralRewardIssuedRepository rewardIssuedRepository
    ) {
        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");
        this.referralRepository = Objects.requireNonNull(referralRepository, "referralRepository");
        this.rewardIssuedRepository = Objects.requireNonNull(rewardIssuedRepository, "rewardIssuedRepository");
    }

    @Transactional(readOnly = true)
    public ReferralDashboardResponse dashboard(String tenantId, String programmeUid) {
        String programme = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
        List<Referral> all = referralRepository.findByTenantIdAndProgrammeUid(tenantId, programme);

        ReferralDashboardResponse d = new ReferralDashboardResponse();
        d.setTotalReferrals(all.size());
        d.setSignedUp(all.stream().filter(r ->
            r.getStatus() == ReferralStatus.SIGNED_UP || r.getStatus() == ReferralStatus.REWARDED
        ).count());
        d.setRewarded(all.stream().filter(r -> r.getStatus() == ReferralStatus.REWARDED).count());
        d.setFraudFlagged(all.stream().filter(r -> r.getStatus() == ReferralStatus.FRAUD_FLAGGED).count());

        BigDecimal points = rewardIssuedRepository.findByTenantIdAndProgrammeUid(tenantId, programme).stream()
            .map(ReferralRewardIssued::getPointsAwarded)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        d.setTotalPointsIssued(points);
        if (d.getTotalReferrals() > 0) {
            d.setConversionRatePercent(
                (d.getRewarded() * 100.0) / d.getTotalReferrals()
            );
            d.setAveragePointsPerReferral(
                points.divide(BigDecimal.valueOf(d.getTotalReferrals()), 2, RoundingMode.HALF_UP)
            );
        }
        return d;
    }

    @Transactional(readOnly = true)
    public List<ReferralListItemResponse> listReferrals(
        String tenantId,
        String programmeUid,
        String statusFilter
    ) {
        String programme = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
        List<Referral> rows = referralRepository.findByTenantIdAndProgrammeUid(tenantId, programme);
        return rows.stream()
            .filter(r -> statusFilter == null || statusFilter.isBlank()
                || r.getStatus().name().equalsIgnoreCase(statusFilter.trim()))
            .sorted(Comparator.comparing(Referral::getCreatedAt).reversed())
            .map(ReferralAdminService::toListItem)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public String exportReferralsCsv(String tenantId, String programmeUid, String statusFilter) {
        List<ReferralListItemResponse> items = listReferrals(tenantId, programmeUid, statusFilter);
        StringBuilder sb = new StringBuilder();
        sb.append("referralUid,referrerCustomerId,refereeCustomerId,status,referralCodeUsed,purchaseCount,createdAt,updatedAt\n");
        for (ReferralListItemResponse item : items) {
            sb.append(csv(item.getReferralUid())).append(',');
            sb.append(csv(item.getReferrerCustomerId())).append(',');
            sb.append(csv(item.getRefereeCustomerId())).append(',');
            sb.append(csv(item.getStatus())).append(',');
            sb.append(csv(item.getReferralCodeUsed())).append(',');
            sb.append(item.getPurchaseCount()).append(',');
            sb.append(item.getCreatedAt()).append(',');
            sb.append(item.getUpdatedAt()).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static ReferralListItemResponse toListItem(Referral r) {
        ReferralListItemResponse item = new ReferralListItemResponse();
        item.setReferralUid(r.getReferralUid());
        item.setReferrerCustomerId(r.getReferrerCustomerId());
        item.setRefereeCustomerId(r.getRefereeCustomerId());
        item.setStatus(r.getStatus().name());
        item.setReferralCodeUsed(r.getReferralCodeUsed());
        item.setPurchaseCount(r.getPurchaseCount());
        item.setCreatedAt(r.getCreatedAt());
        item.setUpdatedAt(r.getUpdatedAt());
        return item;
    }

    @Transactional(readOnly = true)
    public ReferralProgramme getProgramme(String tenantId, String programmeUid) {
        return programmeService.getRequired(tenantId, programmeUid);
    }

    public ReferralProgrammeConfig readConfig(ReferralProgramme programme) {
        return programmeService.readConfig(programme);
    }
}
