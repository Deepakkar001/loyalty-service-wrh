package com.loyaltyos.voucher.scheduler;

import com.loyaltyos.voucher.config.VoucherProperties;
import com.loyaltyos.voucher.entity.VoucherInventory;
import com.loyaltyos.voucher.enums.VoucherAuditEventType;
import com.loyaltyos.voucher.enums.VoucherStatus;
import com.loyaltyos.voucher.repository.VoucherInventoryRepository;
import com.loyaltyos.voucher.service.VoucherAuditService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "loyaltyos.voucher.expiry-job-enabled", havingValue = "true", matchIfMissing = true)
public class VoucherExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(VoucherExpiryScheduler.class);

    private final VoucherInventoryRepository inventoryRepository;
    private final VoucherAuditService auditService;
    private final VoucherProperties properties;

    public VoucherExpiryScheduler(
        VoucherInventoryRepository inventoryRepository,
        VoucherAuditService auditService,
        VoucherProperties properties
    ) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.auditService = Objects.requireNonNull(auditService);
        this.properties = Objects.requireNonNull(properties);
    }

    @Scheduled(cron = "${loyaltyos.voucher.expiry-cron:0 0 2 * * *}")
    @Transactional
    public void expireVouchers() {
        if (!properties.isEnabled()) {
            return;
        }
        List<VoucherInventory> expired = inventoryRepository.findByStatusAndExpiresAtBefore(
            VoucherStatus.AVAILABLE,
            Instant.now()
        );
        for (VoucherInventory code : expired) {
            code.setStatus(VoucherStatus.EXPIRED);
            inventoryRepository.save(code);
            auditService.logEvent(
                code.getTenantId(),
                VoucherAuditEventType.EXPIRED,
                code.getInventoryUid(),
                null,
                "expiry-job",
                null,
                "Code expired by scheduled job",
                null
            );
        }
        if (!expired.isEmpty()) {
            log.info("Voucher expiry job marked {} codes as EXPIRED", expired.size());
        }
    }
}
