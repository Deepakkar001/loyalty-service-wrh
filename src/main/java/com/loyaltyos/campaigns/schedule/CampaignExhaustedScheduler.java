package com.loyaltyos.campaigns.schedule;

import com.loyaltyos.campaigns.service.CampaignExhaustionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "loyalty.campaigns", name = "exhausted-job-enabled", havingValue = "true", matchIfMissing = true)
public class CampaignExhaustedScheduler {

    private final CampaignExhaustionService campaignExhaustionService;

    public CampaignExhaustedScheduler(CampaignExhaustionService campaignExhaustionService) {
        this.campaignExhaustionService = campaignExhaustionService;
    }

    @Scheduled(fixedDelayString = "${loyalty.campaigns.exhausted-fixed-delay-ms:300000}")
    public void sweepExhaustedCampaigns() {
        campaignExhaustionService.sweepActiveCampaignsOverBudget();
    }
}
