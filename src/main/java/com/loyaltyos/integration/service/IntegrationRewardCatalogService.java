package com.loyaltyos.integration.service;



import com.loyaltyos.integration.dto.IntegrationRewardCatalogResponse;

import com.loyaltyos.rewards.catalog.RewardCatalogItem;

import com.loyaltyos.onboarding.service.ProgrammeService;

import com.loyaltyos.rewards.catalog.RewardCatalogService;

import com.loyaltyos.rewards.catalog.RewardCatalogSnapshot;

import com.loyaltyos.rewards.catalog.RewardCatalogTypeDefinition;

import com.loyaltyos.voucher.service.VoucherInventoryService;

import java.util.List;

import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;

import org.springframework.stereotype.Service;



@Service

public class IntegrationRewardCatalogService {



    private static final String VOUCHER_TYPE = "VOUCHER";



    private final ProgrammeService programmeService;

    private final RewardCatalogService rewardCatalogService;

    private final ObjectProvider<VoucherInventoryService> voucherInventoryService;



    public IntegrationRewardCatalogService(

        ProgrammeService programmeService,

        RewardCatalogService rewardCatalogService,

        ObjectProvider<VoucherInventoryService> voucherInventoryService

    ) {

        this.programmeService = Objects.requireNonNull(programmeService, "programmeService");

        this.rewardCatalogService = Objects.requireNonNull(rewardCatalogService, "rewardCatalogService");

        this.voucherInventoryService = Objects.requireNonNull(voucherInventoryService);

    }



    public IntegrationRewardCatalogResponse listCatalog(

        String tenantId,

        String programmeUid,

        boolean activeOnly

    ) {

        programmeService.assertProgrammeActiveForIntegration(tenantId, programmeUid);

        String programme = programmeUid == null || programmeUid.isBlank() ? "default" : programmeUid.trim();

        RewardCatalogSnapshot snapshot = rewardCatalogService.loadCatalog(tenantId, programme);

        List<RewardCatalogItem> items = activeOnly

            ? rewardCatalogService.listActiveItems(tenantId, programme)

            : snapshot.items();



        IntegrationRewardCatalogResponse out = new IntegrationRewardCatalogResponse();

        out.setTenantId(tenantId);

        out.setProgrammeUid(programme);

        out.setVersion(snapshot.version());

        out.setRewardTypes(snapshot.rewardTypes().stream().map(this::toTypeDto).toList());

        out.setItems(items.stream().map(item -> toItemDto(tenantId, programme, item)).toList());

        return out;

    }



    private IntegrationRewardCatalogResponse.RewardTypeDto toTypeDto(RewardCatalogTypeDefinition t) {

        IntegrationRewardCatalogResponse.RewardTypeDto dto = new IntegrationRewardCatalogResponse.RewardTypeDto();

        dto.setTypeCode(t.typeCode());

        dto.setLabel(t.label());

        dto.setDescription(t.description());

        return dto;

    }



    private IntegrationRewardCatalogResponse.RewardItemDto toItemDto(

        String tenantId,

        String programmeUid,

        RewardCatalogItem item

    ) {

        IntegrationRewardCatalogResponse.RewardItemDto dto = new IntegrationRewardCatalogResponse.RewardItemDto();

        dto.setRewardUid(item.rewardUid());

        dto.setName(item.name());

        dto.setRewardType(item.rewardType());

        dto.setStatus(item.status());

        dto.setPointsCost(item.pointsCost());

        dto.setDisplayOrder(item.displayOrder());

        dto.setDescription(item.description());

        dto.setMetadata(item.metadata());

        if (VOUCHER_TYPE.equalsIgnoreCase(item.rewardType())) {

            voucherInventoryService.ifAvailable(service ->

                dto.setAvailableCount(service.countAvailable(tenantId, programmeUid, item.rewardUid()))

            );

        }

        return dto;

    }

}

