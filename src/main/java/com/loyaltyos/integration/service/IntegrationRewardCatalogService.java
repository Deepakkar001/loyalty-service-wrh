package com.loyaltyos.integration.service;

import com.loyaltyos.integration.dto.IntegrationRewardCatalogResponse;
import com.loyaltyos.rewards.catalog.RewardCatalogItem;
import com.loyaltyos.rewards.catalog.RewardCatalogService;
import com.loyaltyos.rewards.catalog.RewardCatalogSnapshot;
import com.loyaltyos.rewards.catalog.RewardCatalogTypeDefinition;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class IntegrationRewardCatalogService {

    private final RewardCatalogService rewardCatalogService;

    public IntegrationRewardCatalogService(RewardCatalogService rewardCatalogService) {
        this.rewardCatalogService = Objects.requireNonNull(rewardCatalogService, "rewardCatalogService");
    }

    public IntegrationRewardCatalogResponse listCatalog(
        String tenantId,
        String programmeUid,
        boolean activeOnly
    ) {
        RewardCatalogSnapshot snapshot = rewardCatalogService.loadCatalog(tenantId, programmeUid);
        List<RewardCatalogItem> items = activeOnly
            ? rewardCatalogService.listActiveItems(tenantId, programmeUid)
            : snapshot.items();

        IntegrationRewardCatalogResponse out = new IntegrationRewardCatalogResponse();
        out.setTenantId(tenantId);
        out.setProgrammeUid(programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim());
        out.setVersion(snapshot.version());
        out.setRewardTypes(snapshot.rewardTypes().stream().map(this::toTypeDto).toList());
        out.setItems(items.stream().map(this::toItemDto).toList());
        return out;
    }

    private IntegrationRewardCatalogResponse.RewardTypeDto toTypeDto(RewardCatalogTypeDefinition t) {
        IntegrationRewardCatalogResponse.RewardTypeDto dto = new IntegrationRewardCatalogResponse.RewardTypeDto();
        dto.setTypeCode(t.typeCode());
        dto.setLabel(t.label());
        dto.setDescription(t.description());
        return dto;
    }

    private IntegrationRewardCatalogResponse.RewardItemDto toItemDto(RewardCatalogItem item) {
        IntegrationRewardCatalogResponse.RewardItemDto dto = new IntegrationRewardCatalogResponse.RewardItemDto();
        dto.setRewardUid(item.rewardUid());
        dto.setName(item.name());
        dto.setRewardType(item.rewardType());
        dto.setStatus(item.status());
        dto.setPointsCost(item.pointsCost());
        dto.setDisplayOrder(item.displayOrder());
        dto.setDescription(item.description());
        dto.setMetadata(item.metadata());
        return dto;
    }
}
