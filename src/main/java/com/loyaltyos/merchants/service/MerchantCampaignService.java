package com.loyaltyos.merchants.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.campaigns.dto.CampaignUpsertRequest;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.service.CampaignService;
import com.loyaltyos.merchants.entity.MerchantApprovalRequest;
import com.loyaltyos.merchants.enums.MerchantApprovalRequestType;
import com.loyaltyos.merchants.enums.MerchantApprovalStatus;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.exception.MerchantAccessDeniedException;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantApprovalRequestRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantCampaignService {

    private static final String MERCHANT_FUNDED = "MERCHANT_FUNDED";

    private final CampaignService campaignService;
    private final CampaignRepository campaignRepository;
    private final MerchantRepository merchantRepository;
    private final MerchantApprovalRequestRepository approvalRequestRepository;
    private final ObjectMapper objectMapper;

    public MerchantCampaignService(
        CampaignService campaignService,
        CampaignRepository campaignRepository,
        MerchantRepository merchantRepository,
        MerchantApprovalRequestRepository approvalRequestRepository,
        ObjectMapper objectMapper
    ) {
        this.campaignService = Objects.requireNonNull(campaignService, "campaignService");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.approvalRequestRepository = Objects.requireNonNull(approvalRequestRepository, "approvalRequestRepository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional
    public CampaignResponse createMerchantCampaign(
        String tenantId,
        String merchantUid,
        CampaignUpsertRequest request,
        String actorEmail
    ) {
        assertActiveMerchant(tenantId, merchantUid);
        request.setMerchantId(merchantUid);
        request.setCampaignType(MERCHANT_FUNDED);
        CampaignResponse created = campaignService.create(tenantId, request, actorEmail);
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, created.getCampaignUid())
            .orElseThrow(() -> new CampaignNotFoundException(created.getCampaignUid()));
        campaign.setPendingMerchantApproval(true);
        campaignRepository.save(campaign);
        created.setPendingMerchantApproval(true);
        persistCampaignApprovalRequest(tenantId, merchantUid, created, actorEmail);
        return created;
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listMerchantCampaigns(String tenantId, String merchantUid) {
        assertActiveMerchant(tenantId, merchantUid);
        return campaignRepository.findByTenantIdAndMerchantIdOrderByCreatedAtDesc(tenantId, merchantUid)
            .stream()
            .map(c -> campaignService.get(tenantId, c.getCampaignUid()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listPendingApprovals(String tenantId) {
        return campaignRepository.findByTenantIdAndPendingMerchantApprovalTrueOrderByCreatedAtDesc(tenantId)
            .stream()
            .map(c -> campaignService.get(tenantId, c.getCampaignUid()))
            .toList();
    }

    @Transactional
    public CampaignResponse approveMerchantCampaign(String tenantId, String campaignUid, String actorEmail) {
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException(campaignUid));
        if (!campaign.isPendingMerchantApproval()) {
            throw new CampaignBadRequestException("Campaign is not pending merchant approval");
        }
        campaign.setPendingMerchantApproval(false);
        campaignRepository.save(campaign);
        resolveCampaignApproval(tenantId, campaignUid, MerchantApprovalStatus.APPROVED, actorEmail, null);
        return campaignService.activate(tenantId, campaignUid);
    }

    @Transactional
    public CampaignResponse rejectMerchantCampaign(String tenantId, String campaignUid, String actorEmail) {
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException(campaignUid));
        if (!campaign.isPendingMerchantApproval()) {
            throw new CampaignBadRequestException("Campaign is not pending merchant approval");
        }
        campaign.setPendingMerchantApproval(false);
        campaign.setStatus(CampaignStatus.ENDED);
        campaignRepository.save(campaign);
        resolveCampaignApproval(tenantId, campaignUid, MerchantApprovalStatus.REJECTED, actorEmail, "Rejected by tenant admin");
        return campaignService.get(tenantId, campaignUid);
    }

    private void persistCampaignApprovalRequest(
        String tenantId,
        String merchantUid,
        CampaignResponse campaign,
        String actorEmail
    ) {
        MerchantApprovalRequest row = new MerchantApprovalRequest();
        row.setRequestUid(UUID.randomUUID().toString());
        row.setTenantId(tenantId);
        row.setMerchantUid(merchantUid);
        row.setRequestType(MerchantApprovalRequestType.CAMPAIGN_APPROVAL);
        row.setRequestedBy(actorEmail != null ? actorEmail : merchantUid);
        row.setRequestedAt(Instant.now());
        row.setStatus(MerchantApprovalStatus.PENDING);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("campaignUid", campaign.getCampaignUid());
        payload.put("campaignName", campaign.getName());
        payload.put("programmeUid", campaign.getProgrammeUid());
        try {
            row.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize campaign approval payload");
        }
        approvalRequestRepository.save(row);
    }

    private void resolveCampaignApproval(
        String tenantId,
        String campaignUid,
        MerchantApprovalStatus status,
        String reviewer,
        String notes
    ) {
        approvalRequestRepository.findByTenantIdAndStatusOrderByRequestedAtDesc(tenantId, MerchantApprovalStatus.PENDING)
            .stream()
            .filter(req -> req.getRequestType() == MerchantApprovalRequestType.CAMPAIGN_APPROVAL)
            .filter(req -> campaignUid.equals(readCampaignUid(req.getPayloadJson())))
            .findFirst()
            .ifPresent(req -> {
                req.setStatus(status);
                req.setReviewedBy(reviewer);
                req.setReviewedAt(Instant.now());
                req.setReviewNotes(notes);
                approvalRequestRepository.save(req);
            });
    }

    private String readCampaignUid(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.readTree(payloadJson);
            JsonNode uid = node.get("campaignUid");
            return uid != null && !uid.isNull() ? uid.asText() : "";
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    private void assertActiveMerchant(String tenantId, String merchantUid) {
        var merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
        if (merchant.getOnboardingStage() != MerchantOnboardingStage.ACTIVE) {
            throw new MerchantAccessDeniedException("Merchant is not active");
        }
    }
}
