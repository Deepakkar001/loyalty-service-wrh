package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.dto.CampaignParticipationResponse;
import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.entity.CampaignParticipation;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignAnalyticsService {

    private final CampaignRepository campaignRepository;
    private final CampaignParticipationRepository participationRepository;

    public CampaignAnalyticsService(
        CampaignRepository campaignRepository,
        CampaignParticipationRepository participationRepository
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.participationRepository = Objects.requireNonNull(participationRepository, "participationRepository");
    }

    @Transactional(readOnly = true)
    public CampaignStatsResponse getCampaignStats(String tenantId, String campaignUid) {
        Campaign c = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));

        CampaignStatsResponse s = new CampaignStatsResponse();
        s.setCampaignUid(c.getCampaignUid());
        s.setCampaignName(c.getName());
        s.setStatus(c.getStatus());
        s.setBudgetTotal(c.getBudgetTotal());
        s.setBudgetConsumed(c.getBudgetConsumed());
        s.setBudgetConsumedPct(consumedPct(c.getBudgetConsumed(), c.getBudgetTotal()));
        s.setBudgetRemaining(c.getBudgetTotal().subtract(c.getBudgetConsumed()).max(BigDecimal.ZERO));
        s.setTotalParticipations(participationRepository.countByTenantIdAndCampaignUid(tenantId, campaignUid));
        s.setUniqueCustomersReached(participationRepository.countDistinctCustomers(tenantId, campaignUid));

        BigDecimal points = participationRepository.sumPointsAwarded(tenantId, campaignUid);
        BigDecimal cashback = participationRepository.sumCashbackRecorded(tenantId, campaignUid);
        s.setTotalPointsIssued(points == null ? BigDecimal.ZERO : points);
        s.setTotalCashbackRecorded(cashback == null ? BigDecimal.ZERO : cashback);
        return s;
    }

    @Transactional(readOnly = true)
    public List<CampaignParticipationResponse> listRecentParticipations(
        String tenantId,
        String campaignUid,
        int limit
    ) {
        campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));

        int pageSize = Math.min(Math.max(limit, 1), 200);
        return participationRepository
            .findByTenantIdAndCampaignUidOrderByParticipatedAtDesc(
                tenantId,
                campaignUid,
                PageRequest.of(0, pageSize)
            )
            .stream()
            .map(this::toParticipationResponse)
            .toList();
    }

    private CampaignParticipationResponse toParticipationResponse(CampaignParticipation p) {
        CampaignParticipationResponse r = new CampaignParticipationResponse();
        r.setCampaignUid(p.getCampaignUid());
        r.setProgrammeUid(p.getProgrammeUid());
        r.setCustomerId(p.getCustomerId());
        r.setEventId(p.getEventId());
        r.setPointsAwarded(p.getPointsAwarded());
        r.setCashbackAmount(p.getCashbackAmount());
        r.setParticipatedAt(p.getParticipatedAt());
        return r;
    }

    private static BigDecimal consumedPct(BigDecimal consumed, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal c = consumed == null ? BigDecimal.ZERO : consumed;
        return c.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }
}
