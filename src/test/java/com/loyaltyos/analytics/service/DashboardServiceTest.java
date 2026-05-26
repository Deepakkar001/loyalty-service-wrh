package com.loyaltyos.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.loyaltyos.analytics.dto.DashboardOverviewResponse;
import com.loyaltyos.analytics.dto.DashboardRedemptionRow;
import com.loyaltyos.analytics.dto.DashboardTopRuleRow;
import com.loyaltyos.analytics.dto.DashboardVolumePoint;
import com.loyaltyos.analytics.dto.SegmentAnalysisRow;
import com.loyaltyos.analytics.dto.TierDistributionRow;
import com.loyaltyos.analytics.repository.AnalyticsQueryRepository;
import com.loyaltyos.analytics.repository.DashboardQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    private DashboardQueryRepository dashboardRepo;
    private AnalyticsQueryRepository analyticsRepo;
    private DashboardService service;

    @BeforeEach
    void setUp() {
        dashboardRepo = mock(DashboardQueryRepository.class);
        analyticsRepo = mock(AnalyticsQueryRepository.class);
        service = new DashboardService(dashboardRepo, analyticsRepo);
    }

    @Test
    void getOverview_returnsAggregatedMetrics() {
        String tenantId = "t1";
        String programmeUid = "default";

        when(dashboardRepo.hasLedgerActivity(tenantId, programmeUid)).thenReturn(true);
        when(dashboardRepo.countActiveMembers(tenantId, programmeUid)).thenReturn(100L);
        when(dashboardRepo.countDistinctActiveCustomers(eq(tenantId), eq(programmeUid), any(), any()))
            .thenReturn(40L, 35L);
        when(dashboardRepo.sumPointsByType(eq(tenantId), eq(programmeUid), eq("CREDIT"), any(), any()))
            .thenReturn(new BigDecimal("1000"), new BigDecimal("800"));
        when(dashboardRepo.sumPointsByType(eq(tenantId), eq(programmeUid), eq("DEBIT"), any(), any()))
            .thenReturn(new BigDecimal("200"), new BigDecimal("150"));
        when(dashboardRepo.avgSuccessfulEventAmount(eq(tenantId), any(), any()))
            .thenReturn(Optional.of(new BigDecimal("500")), Optional.of(new BigDecimal("400")));
        when(dashboardRepo.getDailyVolumeSeries(eq(tenantId), eq(programmeUid), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(List.of(new DashboardVolumePoint("2026-05-20", new BigDecimal("100"), new BigDecimal("20"))));
        when(dashboardRepo.getTopRules(eq(tenantId), eq(programmeUid), any(), any(), anyInt()))
            .thenReturn(List.of(new DashboardTopRuleRow("r1", "Rule A", 10, new BigDecimal("500"))));
        when(dashboardRepo.getTopRedemptions(eq(tenantId), eq(programmeUid), any(), any(), anyInt()))
            .thenReturn(List.of(new DashboardRedemptionRow("reward-1", 3, new BigDecimal("90"))));
        when(analyticsRepo.getEngagementSegments(tenantId, programmeUid))
            .thenReturn(List.of(
                new SegmentAnalysisRow("ACTIVE", 70, BigDecimal.TEN, BigDecimal.valueOf(700)),
                new SegmentAnalysisRow("DORMANT", 30, BigDecimal.ONE, BigDecimal.valueOf(30))
            ));
        when(analyticsRepo.getTierDistribution(tenantId, programmeUid))
            .thenReturn(List.of(new TierDistributionRow("Gold", 1, 10, BigDecimal.ZERO, BigDecimal.ONE)));
        when(analyticsRepo.getRetentionCohort(tenantId, programmeUid)).thenReturn(List.of());

        DashboardOverviewResponse res = service.getOverview(tenantId, programmeUid);

        assertEquals(programmeUid, res.programmeUid());
        assertTrue(res.hasData());
        assertEquals(0, new BigDecimal("1000").compareTo(res.pointsIssuedToday().value()));
        assertEquals(1, res.topRules().size());
        assertEquals(70.0, res.engagement().activePct(), 0.01);
    }
}
