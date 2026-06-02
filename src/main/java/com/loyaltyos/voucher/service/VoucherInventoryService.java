package com.loyaltyos.voucher.service;

import com.loyaltyos.voucher.config.VoucherProperties;
import com.loyaltyos.voucher.dto.VoucherStockDto;
import com.loyaltyos.voucher.repository.VoucherInventoryRepository;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class VoucherInventoryService {

    private final VoucherInventoryRepository inventoryRepository;
    private final VoucherProperties properties;

    public VoucherInventoryService(
        VoucherInventoryRepository inventoryRepository,
        VoucherProperties properties
    ) {
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.properties = Objects.requireNonNull(properties);
    }

    public long countAvailable(String tenantId, String programmeUid, String catalogRewardUid) {
        String programme = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
        return inventoryRepository.countAvailable(tenantId, programme, catalogRewardUid.trim());
    }

    public long countAvailableByFaceValue(
        String tenantId,
        String programmeUid,
        String catalogRewardUid,
        BigDecimal faceValue
    ) {
        String programme = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();
        return inventoryRepository.countAvailableByFaceValue(
            tenantId, programme, catalogRewardUid.trim(), faceValue
        );
    }

    public VoucherStockDto stock(String tenantId, String programmeUid, String catalogRewardUid) {
        long available = countAvailable(tenantId, programmeUid, catalogRewardUid);
        VoucherStockDto dto = new VoucherStockDto();
        dto.setCatalogRewardUid(catalogRewardUid);
        dto.setAvailable(available);
        dto.setLowStockThreshold(properties.getLowStockThreshold());
        dto.setLowStock(available < properties.getLowStockThreshold());
        return dto;
    }
}
