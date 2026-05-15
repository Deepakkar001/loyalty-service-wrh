package com.loyaltyos.onboarding.rewards.schedule;

import com.loyaltyos.onboarding.rewards.service.PointsExpiryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "loyalty.rewards", name = "expiry-job-enabled", havingValue = "true")
public class PointsExpiryJobScheduler {

    private final PointsExpiryService pointsExpiryService;

    public PointsExpiryJobScheduler(PointsExpiryService pointsExpiryService) {
        this.pointsExpiryService = pointsExpiryService;
    }

    @Scheduled(fixedDelayString = "${loyalty.rewards.expiry-fixed-delay-ms:3600000}")
    public void tick() {
        pointsExpiryService.runGlobalExpiryBatch();
    }
}
