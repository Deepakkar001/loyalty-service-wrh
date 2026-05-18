package com.loyaltyos.campaigns.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.repository.CampaignParticipationRepository;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CampaignAnalyticsServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private CampaignParticipationRepository participationRepository;

    @InjectMocks
    private CampaignAnalyticsService analyticsService;

    @Test
    void getCampaignStats_returnsSeparatePointsAndCashback() {
        Campaign c = new Campaign();
        c.setCampaignUid("camp-1");
        c.setName("Summer");
        c.setStatus(CampaignStatus.ACTIVE);
        c.setBudgetTotal(new BigDecimal("1000"));
        c.setBudgetConsumed(new BigDecimal("250"));

        when(campaignRepository.findByTenantIdAndCampaignUid("t1", "camp-1")).thenReturn(Optional.of(c));
        when(participationRepository.countByTenantIdAndCampaignUid("t1", "camp-1")).thenReturn(5L);
        when(participationRepository.countDistinctCustomers("t1", "camp-1")).thenReturn(3L);
        when(participationRepository.sumPointsAwarded("t1", "camp-1")).thenReturn(new BigDecimal("120"));
        when(participationRepository.sumCashbackRecorded("t1", "camp-1")).thenReturn(new BigDecimal("45"));

        var stats = analyticsService.getCampaignStats("t1", "camp-1");

        assertEquals(new BigDecimal("120"), stats.getTotalPointsIssued());
        assertEquals(new BigDecimal("45"), stats.getTotalCashbackRecorded());
        assertEquals(5L, stats.getTotalParticipations());
    }
}
