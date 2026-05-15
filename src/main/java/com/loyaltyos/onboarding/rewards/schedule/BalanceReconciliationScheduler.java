package com.loyaltyos.onboarding.rewards.schedule;

import com.loyaltyos.onboarding.rewards.service.BalanceReconciliationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "loyalty.rewards", name = "reconciliation-job-enabled", havingValue = "true")
public class BalanceReconciliationScheduler {

    private final BalanceReconciliationService balanceReconciliationService;

    public BalanceReconciliationScheduler(BalanceReconciliationService balanceReconciliationService) {
        this.balanceReconciliationService = balanceReconciliationService;
    }

    @Scheduled(fixedDelayString = "${loyalty.rewards.reconciliation-fixed-delay-ms:3600000}")
    public void tick() {
        balanceReconciliationService.runReconciliationSample();
    }
}
