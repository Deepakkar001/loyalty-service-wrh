package com.loyaltyos.onboarding.rewards.service;

import com.loyaltyos.onboarding.rewards.config.RewardEngineProperties;
import com.loyaltyos.onboarding.rules.entity.PointsLedger;
import com.loyaltyos.onboarding.rewards.entity.PointsExpiryJob;
import com.loyaltyos.onboarding.rewards.repository.PointsExpiryJobRepository;
import com.loyaltyos.onboarding.rewards.repository.PointsLedgerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PointsExpiryService {

    public static final String GLOBAL_TENANT_TAG = "GLOBAL";

    private final PointsLedgerRepository pointsLedgerRepository;
    private final PointsExpiryJobRepository pointsExpiryJobRepository;
    private final PointsExpiryTxnService pointsExpiryTxnService;
    private final RewardEngineProperties rewardEngineProperties;

    public PointsExpiryService(
        PointsLedgerRepository pointsLedgerRepository,
        PointsExpiryJobRepository pointsExpiryJobRepository,
        PointsExpiryTxnService pointsExpiryTxnService,
        RewardEngineProperties rewardEngineProperties
    ) {
        this.pointsLedgerRepository = Objects.requireNonNull(pointsLedgerRepository, "pointsLedgerRepository");
        this.pointsExpiryJobRepository = Objects.requireNonNull(pointsExpiryJobRepository, "pointsExpiryJobRepository");
        this.pointsExpiryTxnService = Objects.requireNonNull(pointsExpiryTxnService, "pointsExpiryTxnService");
        this.rewardEngineProperties = Objects.requireNonNull(rewardEngineProperties, "rewardEngineProperties");
    }

    /**
     * Processes a bounded batch of expired CREDIT rows across all tenants (operational job metadata in {@code points_expiry_jobs}).
     */
    public void runGlobalExpiryBatch() {
        Instant now = Instant.now();
        PointsExpiryJob job = new PointsExpiryJob();
        job.setTenantId(GLOBAL_TENANT_TAG);
        job.setJobUid("exp-" + LocalDate.now(ZoneOffset.UTC) + "-" + UUID.randomUUID());
        job.setBatchDate(LocalDate.now(ZoneOffset.UTC));
        job.setStatus(PointsExpiryJob.STATUS_RUNNING);
        job = pointsExpiryJobRepository.save(job);

        try {
            int limit = Math.max(1, rewardEngineProperties.getExpiryBatchLimit());
            List<Long> ids = pointsLedgerRepository.findGlobalCreditIdsEligibleForExpiry(now, limit);
            long totalExpired = 0;
            Set<String> customers = new HashSet<>();
            for (Long id : ids) {
                if (id == null) {
                    continue;
                }
                PointsLedger creditRow = pointsLedgerRepository.findById(id).orElse(null);
                if (creditRow == null) {
                    continue;
                }
                if (pointsExpiryTxnService.tryExpireCreditRow(id, now)) {
                    totalExpired++;
                    customers.add(creditRow.getTenantId() + "|" + creditRow.getCustomerId());
                }
            }
            job.setStatus(PointsExpiryJob.STATUS_SUCCESS);
            job.setTotalExpired(totalExpired);
            job.setCustomersAffected((long) customers.size());
            job.setExecutedAt(Instant.now());
            job.setErrorMessage(null);
        } catch (Exception ex) {
            job.setStatus(PointsExpiryJob.STATUS_FAILED);
            job.setExecutedAt(Instant.now());
            String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            job.setErrorMessage(msg.length() > 60000 ? msg.substring(0, 60000) : msg);
        }
        pointsExpiryJobRepository.save(job);
    }
}
