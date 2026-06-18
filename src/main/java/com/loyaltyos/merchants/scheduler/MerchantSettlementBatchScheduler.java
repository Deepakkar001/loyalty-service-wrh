package com.loyaltyos.merchants.scheduler;

import com.loyaltyos.merchants.config.MerchantProperties;
import com.loyaltyos.merchants.entity.Merchant;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.repository.MerchantRepository;
import com.loyaltyos.merchants.repository.MerchantSettlementCycleRepository;
import com.loyaltyos.merchants.service.MerchantSettlementService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "loyaltyos.merchant.enabled", havingValue = "true", matchIfMissing = true)
public class MerchantSettlementBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(MerchantSettlementBatchScheduler.class);

    private final MerchantProperties merchantProperties;
    private final MerchantRepository merchantRepository;
    private final MerchantSettlementCycleRepository cycleRepository;
    private final MerchantSettlementService settlementService;

    public MerchantSettlementBatchScheduler(
        MerchantProperties merchantProperties,
        MerchantRepository merchantRepository,
        MerchantSettlementCycleRepository cycleRepository,
        MerchantSettlementService settlementService
    ) {
        this.merchantProperties = merchantProperties;
        this.merchantRepository = merchantRepository;
        this.cycleRepository = cycleRepository;
        this.settlementService = settlementService;
    }

    @Scheduled(cron = "${loyaltyos.merchant.settlement-batch-cron:0 30 2 1 * *}")
    @ConditionalOnProperty(name = "loyaltyos.merchant.settlement-batch-job-enabled", havingValue = "true")
    public void generatePreviousMonthStatements() {
        if (!merchantProperties.isEnabled()) {
            return;
        }
        YearMonth period = YearMonth.now().minusMonths(1);
        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();
        List<Merchant> merchants = merchantRepository.findByOnboardingStage(MerchantOnboardingStage.ACTIVE);
        int generated = 0;
        int skipped = 0;
        for (Merchant merchant : merchants) {
            String tenantId = merchant.getTenantId();
            String merchantUid = merchant.getMerchantUid();
            if (cycleRepository.existsByTenantIdAndMerchantUidAndPeriodStartAndPeriodEnd(
                tenantId, merchantUid, periodStart, periodEnd)) {
                skipped++;
                continue;
            }
            try {
                settlementService.generateMonthlyCycle(tenantId, merchantUid, period);
                generated++;
            } catch (RuntimeException e) {
                log.warn(
                    "Settlement batch skipped merchant {} tenant {}: {}",
                    merchantUid,
                    tenantId,
                    e.getMessage()
                );
            }
        }
        log.info(
            "Merchant settlement batch for {} complete: generated={}, skipped={}, merchants={}",
            period,
            generated,
            skipped,
            merchants.size()
        );
    }
}
