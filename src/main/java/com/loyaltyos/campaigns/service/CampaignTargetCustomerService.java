package com.loyaltyos.campaigns.service;

import com.loyaltyos.campaigns.dto.CampaignTargetCustomerPageResponse;
import com.loyaltyos.campaigns.dto.CampaignTargetCustomerResponse;
import com.loyaltyos.campaigns.dto.CampaignTargetUploadResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.entity.CampaignTargetCustomer;
import com.loyaltyos.campaigns.entity.CampaignTargetUpload;
import com.loyaltyos.campaigns.enums.CustomerScope;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.repository.CampaignTargetCustomerRepository;
import com.loyaltyos.campaigns.repository.CampaignTargetUploadRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampaignTargetCustomerService {

    private final CampaignRepository campaignRepository;
    private final CampaignTargetCustomerRepository targetCustomerRepository;
    private final CampaignTargetUploadRepository uploadRepository;

    public CampaignTargetCustomerService(
        CampaignRepository campaignRepository,
        CampaignTargetCustomerRepository targetCustomerRepository,
        CampaignTargetUploadRepository uploadRepository
    ) {
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.targetCustomerRepository = Objects.requireNonNull(targetCustomerRepository, "targetCustomerRepository");
        this.uploadRepository = Objects.requireNonNull(uploadRepository, "uploadRepository");
    }

    @Transactional(readOnly = true)
    public CampaignTargetCustomerPageResponse list(
        String tenantId,
        String campaignUid,
        int page,
        int size,
        String search
    ) {
        assertCampaignExists(tenantId, campaignUid);
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<CampaignTargetCustomer> result = targetCustomerRepository.search(
            tenantId,
            campaignUid,
            search,
            pageable
        );

        CampaignTargetCustomerPageResponse out = new CampaignTargetCustomerPageResponse();
        out.setPage(safePage);
        out.setSize(safeSize);
        out.setTotalElements(result.getTotalElements());
        out.setTotalPages(result.getTotalPages());
        List<CampaignTargetCustomerResponse> rows = new ArrayList<>();
        for (CampaignTargetCustomer c : result.getContent()) {
            CampaignTargetCustomerResponse row = new CampaignTargetCustomerResponse();
            row.setCustomerId(c.getCustomerId());
            row.setAddedAt(c.getAddedAt());
            row.setAddedBy(c.getAddedBy());
            rows.add(row);
        }
        out.setContent(rows);
        return out;
    }

    @Transactional
    public void remove(String tenantId, String campaignUid, String customerId) {
        Campaign campaign = loadCampaign(tenantId, campaignUid);
        targetCustomerRepository.deleteByTenantIdAndCampaignUidAndCustomerId(
            tenantId,
            campaignUid,
            customerId.trim()
        );
        long count = targetCustomerRepository.countByTenantIdAndCampaignUid(tenantId, campaignUid);
        campaign.setCustomerCount(Math.toIntExact(Math.min(count, Integer.MAX_VALUE)));
        if (count == 0 && campaign.getCustomerScope() == CustomerScope.TARGETED) {
            campaign.setCustomerScope(CustomerScope.TARGETED);
        }
        campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public List<CampaignTargetUploadResponse> listUploads(String tenantId, String campaignUid) {
        assertCampaignExists(tenantId, campaignUid);
        List<CampaignTargetUploadResponse> out = new ArrayList<>();
        for (CampaignTargetUpload u : uploadRepository.findByTenantIdAndCampaignUidOrderByUploadedAtDesc(
            tenantId,
            campaignUid
        )) {
            CampaignTargetUploadResponse row = new CampaignTargetUploadResponse();
            row.setUploadUid(u.getUploadUid());
            row.setStatus(u.getStatus().name());
            row.setTotalRowsUploaded(u.getTotalRowsUploaded());
            row.setImportedCount(u.getImportedCount());
            row.setDuplicateCount(u.getDuplicateCount());
            row.setErrorCount(u.getErrorCount());
            row.setTenantId(u.getTenantId());
            row.setUploadedBy(u.getTenantId());
            row.setUploadedAt(u.getUploadedAt());
            row.setCompletedAt(u.getCompletedAt());
            out.add(row);
        }
        return out;
    }

    private void assertCampaignExists(String tenantId, String campaignUid) {
        if (!campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid).isPresent()) {
            throw new CampaignNotFoundException("Campaign not found: " + campaignUid);
        }
    }

    private Campaign loadCampaign(String tenantId, String campaignUid) {
        return campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException("Campaign not found: " + campaignUid));
    }
}
