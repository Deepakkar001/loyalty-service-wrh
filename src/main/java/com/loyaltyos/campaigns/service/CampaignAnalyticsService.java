package com.loyaltyos.campaigns.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.campaigns.dto.CampaignParticipationAggregate;
import com.loyaltyos.campaigns.dto.CampaignParticipationResponse;
import com.loyaltyos.campaigns.dto.CampaignPerformanceReportResponse;
import com.loyaltyos.campaigns.dto.CampaignPerformanceRow;
import com.loyaltyos.campaigns.dto.CampaignPerformanceSummary;
import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.entity.CampaignParticipation;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.enums.CustomerScope;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.repository.CampaignAnalyticsQueryRepository;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignAnalyticsService {

    private final CampaignRepository campaignRepository;
    private final CampaignParticipationRepository participationRepository;
    private final CampaignAnalyticsQueryRepository analyticsQueryRepository;

    public CampaignAnalyticsService(
        CampaignRepository campaignRepository,
        CampaignParticipationRepository participationRepository,
        CampaignAnalyticsQueryRepository analyticsQueryRepository
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.participationRepository = Objects.requireNonNull(participationRepository, "participationRepository");
        this.analyticsQueryRepository = Objects.requireNonNull(analyticsQueryRepository, "analyticsQueryRepository");
    }

    @Transactional(readOnly = true)
    public CampaignStatsResponse getCampaignStats(String tenantId, String campaignUid) {
        Campaign c = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));

        long totalParticipations = participationRepository.countByTenantIdAndCampaignUid(tenantId, campaignUid);
        long uniqueCustomers = participationRepository.countDistinctCustomers(tenantId, campaignUid);
        BigDecimal points = nullToZero(participationRepository.sumPointsAwarded(tenantId, campaignUid));
        BigDecimal cashback = nullToZero(participationRepository.sumCashbackRecorded(tenantId, campaignUid));
        BigDecimal budgetConsumed = c.getBudgetConsumed() == null ? BigDecimal.ZERO : c.getBudgetConsumed();
        BigDecimal budgetTotal = c.getBudgetTotal() == null ? BigDecimal.ZERO : c.getBudgetTotal();

        CampaignStatsResponse s = new CampaignStatsResponse();
        s.setCampaignUid(c.getCampaignUid());
        s.setCampaignName(c.getName());
        s.setStatus(c.getStatus());
        s.setBudgetTotal(budgetTotal);
        s.setBudgetConsumed(budgetConsumed);
        s.setBudgetConsumedPct(consumedPct(budgetConsumed, budgetTotal));
        s.setBudgetRemaining(budgetTotal.subtract(budgetConsumed).max(BigDecimal.ZERO));
        s.setTotalParticipations(totalParticipations);
        s.setUniqueCustomersReached(uniqueCustomers);
        s.setTotalPointsIssued(points);
        s.setTotalCashbackRecorded(cashback);
        s.setCustomerScope(c.getCustomerScope());
        s.setAwardType(extractAwardType(c.getOfferConfig()));
        s.setTargetAudienceSize(c.getCustomerCount() == null ? 0 : c.getCustomerCount());
        s.setMaxParticipations(c.getMaxParticipations());
        s.setMaxPerCustomer(c.getMaxPerCustomer());
        applyDerivedMetrics(
            s::setAvgPointsPerParticipation,
            s::setAvgCashbackPerParticipation,
            s::setAvgParticipationsPerCustomer,
            s::setAudienceReachPct,
            s::setParticipationCapPct,
            s::setRewardCostPerParticipation,
            totalParticipations,
            uniqueCustomers,
            points,
            cashback,
            budgetConsumed,
            c.getCustomerScope(),
            c.getCustomerCount(),
            c.getMaxParticipations()
        );
        return s;
    }

    @Transactional(readOnly = true)
    public CampaignPerformanceReportResponse buildPerformanceReport(
        String tenantId,
        String programmeUid,
        LocalDate from,
        LocalDate to
    ) {
        long periodDays = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate priorFrom = from.minusDays(periodDays);

        List<Campaign> campaigns = campaignRepository.findByTenantIdAndProgrammeUidOrderByPriorityDescCreatedAtDesc(
            tenantId,
            programmeUid
        );
        Map<String, CampaignParticipationAggregate> periodByCampaign =
            analyticsQueryRepository.getParticipationAggregatesByCampaign(tenantId, programmeUid, from, to);
        Map<String, CampaignParticipationAggregate> priorByCampaign =
            analyticsQueryRepository.getParticipationAggregatesByCampaign(tenantId, programmeUid, priorFrom, from.minusDays(1));
        Map<String, CampaignParticipationAggregate> allTimeByCampaign =
            analyticsQueryRepository.getAllTimeParticipationAggregatesByCampaign(tenantId, programmeUid);

        CampaignPerformanceSummary summary = new CampaignPerformanceSummary();
        summary.setProgrammeUid(programmeUid);
        summary.setFromDate(from.toString());
        summary.setToDate(to.toString());
        summary.setTotalCampaigns(campaigns.size());
        summary.setActiveCampaigns(
            (int) campaigns.stream().filter(c -> c.getStatus() == CampaignStatus.ACTIVE).count()
        );
        summary.setParticipationsInPeriod(
            analyticsQueryRepository.countProgrammeParticipations(tenantId, programmeUid, from, to)
        );
        summary.setParticipationsPriorPeriod(
            analyticsQueryRepository.countProgrammeParticipations(tenantId, programmeUid, priorFrom, from.minusDays(1))
        );
        summary.setUniqueCustomersInPeriod(
            analyticsQueryRepository.countProgrammeUniqueCustomers(tenantId, programmeUid, from, to)
        );
        summary.setPointsInPeriod(analyticsQueryRepository.sumProgrammePoints(tenantId, programmeUid, from, to));
        summary.setCashbackInPeriod(analyticsQueryRepository.sumProgrammeCashback(tenantId, programmeUid, from, to));
        summary.setTotalBudgetAllocated(campaigns.stream()
            .map(Campaign::getBudgetTotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setTotalBudgetConsumed(campaigns.stream()
            .map(C -> C.getBudgetConsumed() == null ? BigDecimal.ZERO : C.getBudgetConsumed())
            .reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.setPeriodOverPeriodChangePct(
            percentChange(summary.getParticipationsInPeriod(), summary.getParticipationsPriorPeriod())
        );

        List<CampaignPerformanceRow> rows = new ArrayList<>();
        for (Campaign campaign : campaigns) {
            CampaignParticipationAggregate period = periodByCampaign.get(campaign.getCampaignUid());
            CampaignParticipationAggregate prior = priorByCampaign.get(campaign.getCampaignUid());
            CampaignParticipationAggregate allTime = allTimeByCampaign.get(campaign.getCampaignUid());
            rows.add(toPerformanceRow(campaign, period, prior, allTime));
        }
        rows.sort(Comparator.comparingLong(CampaignPerformanceRow::getParticipationsInPeriod).reversed());

        CampaignPerformanceReportResponse report = new CampaignPerformanceReportResponse();
        report.setSummary(summary);
        report.setDailyParticipations(
            analyticsQueryRepository.getDailyParticipationTrend(tenantId, programmeUid, from, to)
        );
        report.setCampaigns(rows);
        return report;
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

    private CampaignPerformanceRow toPerformanceRow(
        Campaign campaign,
        CampaignParticipationAggregate period,
        CampaignParticipationAggregate prior,
        CampaignParticipationAggregate allTime
    ) {
        long participationsInPeriod = period == null ? 0L : period.participations();
        long uniqueInPeriod = period == null ? 0L : period.uniqueCustomers();
        BigDecimal pointsInPeriod = period == null ? BigDecimal.ZERO : period.pointsIssued();
        BigDecimal cashbackInPeriod = period == null ? BigDecimal.ZERO : period.cashbackRecorded();
        long participationsAllTime = allTime == null ? 0L : allTime.participations();
        long uniqueAllTime = allTime == null ? 0L : allTime.uniqueCustomers();
        long priorParticipations = prior == null ? 0L : prior.participations();

        BigDecimal budgetTotal = campaign.getBudgetTotal() == null ? BigDecimal.ZERO : campaign.getBudgetTotal();
        BigDecimal budgetConsumed = campaign.getBudgetConsumed() == null ? BigDecimal.ZERO : campaign.getBudgetConsumed();

        CampaignPerformanceRow row = new CampaignPerformanceRow();
        row.setCampaignUid(campaign.getCampaignUid());
        row.setCampaignName(campaign.getName());
        row.setStatus(campaign.getStatus());
        row.setCustomerScope(campaign.getCustomerScope());
        row.setAwardType(extractAwardType(campaign.getOfferConfig()));
        row.setValidFrom(campaign.getValidFrom());
        row.setValidUntil(campaign.getValidUntil());
        row.setBudgetTotal(budgetTotal);
        row.setBudgetConsumed(budgetConsumed);
        row.setBudgetConsumedPct(consumedPct(budgetConsumed, budgetTotal));
        row.setBudgetRemaining(budgetTotal.subtract(budgetConsumed).max(BigDecimal.ZERO));
        row.setMaxParticipations(campaign.getMaxParticipations());
        row.setMaxPerCustomer(campaign.getMaxPerCustomer());
        row.setTargetAudienceSize(campaign.getCustomerCount() == null ? 0 : campaign.getCustomerCount());
        row.setParticipationsInPeriod(participationsInPeriod);
        row.setUniqueCustomersInPeriod(uniqueInPeriod);
        row.setPointsInPeriod(pointsInPeriod);
        row.setCashbackInPeriod(cashbackInPeriod);
        row.setParticipationsAllTime(participationsAllTime);
        row.setUniqueCustomersAllTime(uniqueAllTime);
        row.setPeriodOverPeriodChangePct(percentChange(participationsInPeriod, priorParticipations));
        row.setFirstParticipationAt(allTime == null ? null : allTime.firstParticipationAt());
        row.setLastParticipationAt(allTime == null ? null : allTime.lastParticipationAt());
        row.setAvgPointsPerParticipation(divide(pointsInPeriod, participationsInPeriod));
        row.setAvgCashbackPerParticipation(divide(cashbackInPeriod, participationsInPeriod));
        row.setAvgParticipationsPerCustomer(divide(BigDecimal.valueOf(participationsInPeriod), uniqueInPeriod));
        row.setAudienceReachPct(
            audienceReachPct(campaign.getCustomerScope(), campaign.getCustomerCount(), uniqueAllTime)
        );
        row.setParticipationCapPct(capPct(campaign.getMaxParticipations(), participationsAllTime));
        row.setRewardCostPerParticipation(divide(budgetConsumed, participationsInPeriod));
        return row;
    }

    private interface BigDecimalSetter {
        void set(BigDecimal value);
    }

    private static void applyDerivedMetrics(
        BigDecimalSetter avgPointsSetter,
        BigDecimalSetter avgCashbackSetter,
        BigDecimalSetter avgParticipationsPerCustomerSetter,
        BigDecimalSetter audienceReachSetter,
        BigDecimalSetter capSetter,
        BigDecimalSetter costPerParticipationSetter,
        long participations,
        long uniqueCustomers,
        BigDecimal points,
        BigDecimal cashback,
        BigDecimal budgetConsumed,
        CustomerScope customerScope,
        Integer targetAudienceSize,
        Integer maxParticipations
    ) {
        avgPointsSetter.set(divide(points, participations));
        avgCashbackSetter.set(divide(cashback, participations));
        avgParticipationsPerCustomerSetter.set(divide(BigDecimal.valueOf(participations), uniqueCustomers));
        audienceReachSetter.set(audienceReachPct(customerScope, targetAudienceSize, uniqueCustomers));
        capSetter.set(capPct(maxParticipations, participations));
        costPerParticipationSetter.set(divide(budgetConsumed, participations));
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

    private static String extractAwardType(JsonNode offerConfig) {
        if (offerConfig == null || !offerConfig.hasNonNull("awardType")) {
            return null;
        }
        return offerConfig.get("awardType").asText();
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal consumedPct(BigDecimal consumed, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal c = consumed == null ? BigDecimal.ZERO : consumed;
        return c.multiply(new BigDecimal("100")).divide(total, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal divide(BigDecimal numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal n = numerator == null ? BigDecimal.ZERO : numerator;
        return n.divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal audienceReachPct(
        CustomerScope customerScope,
        Integer targetAudienceSize,
        long uniqueCustomers
    ) {
        if (customerScope != CustomerScope.TARGETED) {
            return null;
        }
        int audience = targetAudienceSize == null ? 0 : targetAudienceSize;
        if (audience <= 0) {
            return null;
        }
        return BigDecimal.valueOf(uniqueCustomers)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(audience), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal capPct(Integer maxParticipations, long participations) {
        if (maxParticipations == null || maxParticipations <= 0) {
            return null;
        }
        return BigDecimal.valueOf(participations)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(maxParticipations), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percentChange(long current, long prior) {
        if (prior <= 0) {
            return current > 0 ? null : BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(current - prior)
            .multiply(new BigDecimal("100"))
            .divide(BigDecimal.valueOf(prior), 1, RoundingMode.HALF_UP);
    }
}
