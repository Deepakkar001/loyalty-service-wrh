package com.loyaltyos.campaigns.schedule;

import com.loyaltyos.campaigns.service.CampaignExpirationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "loyalty.campaigns", name = "expired-job-enabled", havingValue = "true", matchIfMissing = true)
public class CampaignExpiredScheduler {

    private final CampaignExpirationService campaignExpirationService;

    public CampaignExpiredScheduler(CampaignExpirationService campaignExpirationService) {
        this.campaignExpirationService = campaignExpirationService;
    }

    @Scheduled(fixedDelayString = "${loyalty.campaigns.expired-fixed-delay-ms:300000}")
    public void sweepExpiredCampaigns() {
        campaignExpirationService.sweepCampaignsPastValidUntil();
    }
}
