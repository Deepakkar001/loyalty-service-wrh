package com.loyaltyos.rewards.catalog;

import com.loyaltyos.voucher.entity.VoucherDenominationMapping;
import com.loyaltyos.voucher.repository.VoucherDenominationMappingRepository;
import com.loyaltyos.voucher.support.VoucherDenominationSupport;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class RewardCatalogService {

    private final RewardCatalogDbMergeService catalogDbMergeService;
    private final ObjectProvider<VoucherDenominationMappingRepository> denominationMappingRepository;

    public RewardCatalogService(
        RewardCatalogDbMergeService catalogDbMergeService,
        ObjectProvider<VoucherDenominationMappingRepository> denominationMappingRepository
    ) {
        this.catalogDbMergeService = Objects.requireNonNull(catalogDbMergeService, "catalogDbMergeService");
        this.denominationMappingRepository = Objects.requireNonNull(denominationMappingRepository);
    }

    public RewardCatalogSnapshot loadCatalog(String tenantId, String programmeUid) {
        return catalogDbMergeService.mergeFromDatabase(tenantId, normalizeProgramme(programmeUid)).catalog();
    }

    public Optional<RewardCatalogItem> findActiveItem(String tenantId, String programmeUid, String rewardUid) {
        if (rewardUid == null || rewardUid.isBlank()) {
            return Optional.empty();
        }
        String uid = rewardUid.trim();
        return loadCatalog(tenantId, programmeUid).items().stream()
            .filter(i -> i.rewardUid().equals(uid))
            .filter(RewardCatalogItem::isActive)
            .findFirst();
    }

    public List<RewardCatalogItem> listActiveItems(String tenantId, String programmeUid) {
        return loadCatalog(tenantId, programmeUid).items().stream()
            .filter(RewardCatalogItem::isActive)
            .toList();
    }

    /**
     * Validates catalog redemption selection. When {@code catalogRewardUid} is set, resolves points from catalog
     * unless {@code requestedPoints} is also sent (must match catalog cost exactly).
     */
    public CatalogRedemptionResolution resolveRedemption(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        BigDecimal requestedPoints
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (catalogRewardUid == null || catalogRewardUid.isBlank()) {
            if (requestedPoints == null || requestedPoints.signum() <= 0) {
                errors.put("pointsToRedeem", "pointsToRedeem is required when catalogRewardUid is omitted");
            }
            return new CatalogRedemptionResolution(null, requestedPoints, errors);
        }

        Optional<RewardCatalogItem> item = findActiveItem(tenantId, programmeUid, catalogRewardUid);
        if (item.isEmpty()) {
            errors.put("catalogRewardUid", "Unknown or inactive catalog reward: " + catalogRewardUid.trim());
            return new CatalogRedemptionResolution(null, null, errors);
        }

        RewardCatalogItem reward = item.get();
        String catalogUid = reward.rewardUid();
        BigDecimal catalogCost = reward.pointsCost();

        VoucherDenominationMappingRepository mappingRepo = denominationMappingRepository.getIfAvailable();
        if (mappingRepo != null
            && mappingRepo.existsByTenantIdAndCatalogRewardUidAndActiveTrue(tenantId, catalogUid)) {
            if (requestedPoints == null) {
                errors.put(
                    "pointsToRedeem",
                    "pointsToRedeem is required for multi-denomination voucher catalog item " + catalogUid
                );
                return new CatalogRedemptionResolution(reward, null, errors);
            }
            List<VoucherDenominationMapping> tiers =
                mappingRepo.findByTenantIdAndCatalogRewardUidAndActiveTrueOrderByPriorityAsc(tenantId, catalogUid);
            boolean tierMatch = tiers.stream()
                .anyMatch(t -> VoucherDenominationSupport.amountsEqual(t.getPointsRequired(), requestedPoints));
            if (!tierMatch) {
                errors.put(
                    "pointsToRedeem",
                    "pointsToRedeem must match a configured denomination tier for catalog " + catalogUid
                );
            }
            return new CatalogRedemptionResolution(reward, requestedPoints, errors);
        }

        if (requestedPoints == null) {
            return new CatalogRedemptionResolution(reward, catalogCost, errors);
        }
        if (requestedPoints.compareTo(catalogCost) != 0) {
            errors.put(
                "pointsToRedeem",
                "pointsToRedeem must equal catalog pointsCost (" + catalogCost.toPlainString() + ") for reward "
                    + catalogUid
            );
        }
        return new CatalogRedemptionResolution(reward, catalogCost, errors);
    }

    public record CatalogRedemptionResolution(
        RewardCatalogItem catalogItem,
        BigDecimal resolvedPoints,
        Map<String, String> errors
    ) {
        public boolean isValid() {
            return errors == null || errors.isEmpty();
        }
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }
}
