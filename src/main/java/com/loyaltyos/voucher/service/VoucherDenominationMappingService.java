package com.loyaltyos.voucher.service;

import com.loyaltyos.rewards.catalog.RewardCatalogItem;
import com.loyaltyos.rewards.catalog.RewardCatalogService;
import com.loyaltyos.voucher.dto.DenominationMappingDto;
import com.loyaltyos.voucher.dto.DenominationMappingItemRequest;
import com.loyaltyos.voucher.dto.DenominationMappingsResponse;
import com.loyaltyos.voucher.dto.VoucherStockBreakdownDto;
import com.loyaltyos.voucher.entity.VoucherDenominationMapping;
import com.loyaltyos.voucher.enums.VoucherStatus;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import com.loyaltyos.voucher.exception.VoucherOutOfStockException;
import com.loyaltyos.voucher.repository.VoucherDenominationMappingRepository;
import com.loyaltyos.voucher.repository.VoucherInventoryRepository;
import com.loyaltyos.voucher.support.VoucherDenominationSupport;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherDenominationMappingService {

    private static final String VOUCHER_TYPE = "VOUCHER";

    private final VoucherDenominationMappingRepository mappingRepository;
    private final VoucherInventoryRepository inventoryRepository;
    private final RewardCatalogService rewardCatalogService;

    public VoucherDenominationMappingService(
        VoucherDenominationMappingRepository mappingRepository,
        VoucherInventoryRepository inventoryRepository,
        RewardCatalogService rewardCatalogService
    ) {
        this.mappingRepository = Objects.requireNonNull(mappingRepository);
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.rewardCatalogService = Objects.requireNonNull(rewardCatalogService);
    }

    public boolean hasActiveMappings(String tenantId, String catalogRewardUid) {
        return mappingRepository.existsByTenantIdAndCatalogRewardUidAndActiveTrue(
            tenantId, catalogRewardUid.trim()
        );
    }

    public List<VoucherDenominationMapping> getActiveMappings(String tenantId, String catalogRewardUid) {
        return mappingRepository.findByTenantIdAndCatalogRewardUidAndActiveTrueOrderByPriorityAsc(
            tenantId, catalogRewardUid.trim()
        );
    }

    @Transactional
    public List<VoucherDenominationMapping> replaceMappings(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        List<DenominationMappingItemRequest> requests
    ) {
        String programme = normalizeProgramme(programmeUid);
        String catalogUid = catalogRewardUid.trim();
        validateCatalogVoucher(tenantId, programme, catalogUid);
        validateMappingRequests(requests);

        mappingRepository.deactivateAllForCatalog(tenantId, catalogUid);

        List<VoucherDenominationMapping> saved = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            DenominationMappingItemRequest req = requests.get(i);
            VoucherDenominationMapping mapping = new VoucherDenominationMapping();
            mapping.setMappingUid(UUID.randomUUID().toString());
            mapping.setTenantId(tenantId);
            mapping.setCatalogRewardUid(catalogUid);
            mapping.setPointsRequired(VoucherDenominationSupport.normalizeAmount(req.getPointsRequired()));
            mapping.setFaceValue(VoucherDenominationSupport.normalizeAmount(req.getFaceValue()));
            mapping.setCurrency(req.getCurrency().trim().toUpperCase(Locale.ROOT));
            mapping.setPriority(i + 1);
            mapping.setActive(true);
            mapping.setDescription(req.getDescription());
            mapping.setPartnerSku(req.getPartnerSku());
            saved.add(mappingRepository.save(mapping));
        }
        return saved;
    }

    public DenominationMappingsResponse getMappingsResponse(
        String tenantId,
        String programmeUid,
        String catalogRewardUid
    ) {
        String programme = normalizeProgramme(programmeUid);
        List<VoucherDenominationMapping> mappings = getActiveMappings(tenantId, catalogRewardUid);
        DenominationMappingsResponse response = new DenominationMappingsResponse();
        response.setCatalogRewardUid(catalogRewardUid);
        response.setMixedDenominationEnabled(!mappings.isEmpty());
        response.setMappings(mappings.stream()
            .map(m -> toDto(tenantId, programme, m))
            .collect(Collectors.toList()));
        return response;
    }

    public VoucherStockBreakdownDto stockBreakdown(String tenantId, String programmeUid, String catalogRewardUid) {
        String programme = normalizeProgramme(programmeUid);
        String catalogUid = catalogRewardUid.trim();
        VoucherStockBreakdownDto dto = new VoucherStockBreakdownDto();
        dto.setCatalogRewardUid(catalogUid);

        List<VoucherDenominationMapping> mappings = getActiveMappings(tenantId, catalogUid);
        if (mappings.isEmpty()) {
            long total = inventoryRepository.countAvailable(tenantId, programme, catalogUid);
            dto.setTotalAvailable(total);
            return dto;
        }

        Map<String, Long> byFace = new LinkedHashMap<>();
        long total = 0;
        for (VoucherDenominationMapping mapping : mappings) {
            long available = inventoryRepository.countAvailableByFaceValue(
                tenantId, programme, catalogUid, mapping.getFaceValue()
            );
            byFace.put(VoucherStockBreakdownDto.faceValueKey(mapping.getFaceValue()), available);
            total += available;
        }
        dto.setStockByFaceValue(byFace);
        dto.setTotalAvailable(total);
        return dto;
    }

    /**
     * Resolves tier by exact points (preferred) or exact face value.
     */
    public VoucherDenominationMapping resolveMapping(
        String tenantId,
        String catalogRewardUid,
        BigDecimal pointsToRedeem,
        BigDecimal faceValue
    ) {
        String catalogUid = catalogRewardUid.trim();
        if (pointsToRedeem != null) {
            List<VoucherDenominationMapping> mappings = getActiveMappings(tenantId, catalogUid);
            for (VoucherDenominationMapping mapping : mappings) {
                if (VoucherDenominationSupport.amountsEqual(mapping.getPointsRequired(), pointsToRedeem)) {
                    return mapping;
                }
            }
            throw new VoucherCatalogException(
                "No denomination mapping for " + pointsToRedeem.toPlainString() + " points on catalog " + catalogUid
            );
        }
        if (faceValue != null) {
            return mappingRepository
                .findByTenantIdAndCatalogRewardUidAndFaceValueAndActiveTrue(
                    tenantId, catalogUid, VoucherDenominationSupport.normalizeAmount(faceValue)
                )
                .orElseThrow(() -> new VoucherCatalogException(
                    "No denomination mapping for face value " + faceValue.toPlainString() + " on catalog " + catalogUid
                ));
        }
        throw new VoucherCatalogException(
            "pointsToRedeem or faceValue is required for multi-denomination catalog " + catalogUid
        );
    }

    public void assertFaceValueAllowed(
        String tenantId,
        String catalogRewardUid,
        BigDecimal faceValue
    ) {
        if (!hasActiveMappings(tenantId, catalogRewardUid)) {
            return;
        }
        BigDecimal normalized = VoucherDenominationSupport.normalizeAmount(faceValue);
        boolean allowed = getActiveMappings(tenantId, catalogRewardUid).stream()
            .anyMatch(m -> VoucherDenominationSupport.amountsEqual(m.getFaceValue(), normalized));
        if (!allowed) {
            throw new IllegalArgumentException(
                "Face value " + normalized.toPlainString()
                    + " is not configured in denomination mappings for catalog " + catalogRewardUid
            );
        }
    }

    public Map<BigDecimal, VoucherDenominationMapping> faceValueIndex(String tenantId, String catalogRewardUid) {
        Map<BigDecimal, VoucherDenominationMapping> index = new LinkedHashMap<>();
        for (VoucherDenominationMapping mapping : getActiveMappings(tenantId, catalogRewardUid)) {
            index.put(VoucherDenominationSupport.normalizeAmount(mapping.getFaceValue()), mapping);
        }
        return index;
    }

    public void assertStockForMapping(
        String tenantId,
        String programmeUid,
        VoucherDenominationMapping mapping
    ) {
        long available = inventoryRepository.countAvailableByFaceValue(
            tenantId,
            normalizeProgramme(programmeUid),
            mapping.getCatalogRewardUid(),
            mapping.getFaceValue()
        );
        if (available == 0) {
            throw new VoucherOutOfStockException(
                "No available vouchers for face value " + mapping.getFaceValue().toPlainString()
            );
        }
    }

    private DenominationMappingDto toDto(
        String tenantId,
        String programmeUid,
        VoucherDenominationMapping mapping
    ) {
        DenominationMappingDto dto = new DenominationMappingDto();
        dto.setMappingUid(mapping.getMappingUid());
        dto.setPointsRequired(mapping.getPointsRequired());
        dto.setFaceValue(mapping.getFaceValue());
        dto.setCurrency(mapping.getCurrency());
        dto.setDescription(mapping.getDescription());
        dto.setPartnerSku(mapping.getPartnerSku());
        dto.setPriority(mapping.getPriority());
        dto.setAvailable(inventoryRepository.countAvailableByFaceValue(
            tenantId,
            programmeUid,
            mapping.getCatalogRewardUid(),
            mapping.getFaceValue()
        ));
        return dto;
    }

    private void validateCatalogVoucher(String tenantId, String programmeUid, String catalogRewardUid) {
        RewardCatalogItem item = rewardCatalogService.findActiveItem(tenantId, programmeUid, catalogRewardUid)
            .orElseThrow(() -> new VoucherCatalogException("Catalog item not found or inactive: " + catalogRewardUid));
        if (!VOUCHER_TYPE.equalsIgnoreCase(item.rewardType())) {
            throw new VoucherCatalogException("Catalog item is not type VOUCHER: " + catalogRewardUid);
        }
    }

    private static void validateMappingRequests(List<DenominationMappingItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("At least one denomination mapping is required");
        }
        Set<BigDecimal> points = new HashSet<>();
        Set<BigDecimal> faces = new HashSet<>();
        String currency = null;
        for (DenominationMappingItemRequest req : requests) {
            if (req.getPointsRequired() == null || req.getPointsRequired().signum() <= 0) {
                throw new IllegalArgumentException("pointsRequired must be positive");
            }
            if (req.getFaceValue() == null || req.getFaceValue().signum() <= 0) {
                throw new IllegalArgumentException("faceValue must be positive");
            }
            if (!points.add(VoucherDenominationSupport.normalizeAmount(req.getPointsRequired()))) {
                throw new IllegalArgumentException("Duplicate pointsRequired in mappings");
            }
            if (!faces.add(VoucherDenominationSupport.normalizeAmount(req.getFaceValue()))) {
                throw new IllegalArgumentException("Duplicate faceValue in mappings");
            }
            String c = req.getCurrency().trim().toUpperCase(Locale.ROOT);
            if (c.length() != 3) {
                throw new IllegalArgumentException("currency must be a 3-letter ISO code");
            }
            try {
                Currency.getInstance(c);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid currency code: " + c);
            }
            if (currency == null) {
                currency = c;
            } else if (!currency.equals(c)) {
                throw new IllegalArgumentException("All denominations must use the same currency");
            }
        }
    }

    private static String normalizeProgramme(String programmeUid) {
        return programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
    }
}
