package com.loyaltyos.merchants.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loyaltyos.campaigns.dto.CampaignEventSchemaUpsertRequest;
import com.loyaltyos.campaigns.dto.CampaignStatsResponse;
import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.campaigns.dto.CampaignUpsertRequest;
import com.loyaltyos.merchants.dto.MerchantCampaignAnalyticsResponse;
import com.loyaltyos.campaigns.entity.Campaign;
import com.loyaltyos.campaigns.enums.CampaignExecutionMode;
import com.loyaltyos.campaigns.enums.CampaignStatus;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.campaigns.repository.CampaignRepository;
import com.loyaltyos.campaigns.service.CampaignAnalyticsService;
import com.loyaltyos.campaigns.service.CampaignService;
import com.loyaltyos.merchants.entity.MerchantApprovalRequest;
import com.loyaltyos.merchants.enums.MerchantApprovalRequestType;
import com.loyaltyos.merchants.enums.MerchantApprovalStatus;
import com.loyaltyos.merchants.enums.MerchantOnboardingStage;
import com.loyaltyos.merchants.exception.MerchantAccessDeniedException;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.merchants.repository.MerchantApprovalRequestRepository;
import com.loyaltyos.merchants.repository.MerchantRepository;
import java.math.BigDecimal;
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
    private final MerchantValidationService merchantValidationService;
    private final CampaignAnalyticsService campaignAnalyticsService;
    private final ObjectMapper objectMapper;

    public MerchantCampaignService(
        CampaignService campaignService,
        CampaignRepository campaignRepository,
        MerchantRepository merchantRepository,
        MerchantApprovalRequestRepository approvalRequestRepository,
        MerchantValidationService merchantValidationService,
        CampaignAnalyticsService campaignAnalyticsService,
        ObjectMapper objectMapper
    ) {
        this.campaignService = Objects.requireNonNull(campaignService, "campaignService");
        this.campaignRepository = Objects.requireNonNull(campaignRepository, "campaignRepository");
        this.merchantRepository = Objects.requireNonNull(merchantRepository, "merchantRepository");
        this.approvalRequestRepository = Objects.requireNonNull(approvalRequestRepository, "approvalRequestRepository");
        this.merchantValidationService = Objects.requireNonNull(merchantValidationService, "merchantValidationService");
        this.campaignAnalyticsService = Objects.requireNonNull(campaignAnalyticsService, "campaignAnalyticsService");
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
        merchantValidationService.validateMerchantCampaignBudget(request.getBudgetTotal());
        request.setMerchantId(merchantUid);
        request.setCampaignType(MERCHANT_FUNDED);
        CampaignResponse created = campaignService.create(tenantId, request, actorEmail);
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, created.getCampaignUid())
            .orElseThrow(() -> new CampaignNotFoundException(created.getCampaignUid()));
        campaign.setPendingMerchantApproval(true);
        campaign.setExecutionMode(CampaignExecutionMode.LEGACY_OFFER);
        campaignRepository.save(campaign);
        created.setPendingMerchantApproval(true);
        persistCampaignApprovalRequest(tenantId, merchantUid, created, actorEmail);
        return created;
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listMerchantCampaigns(String tenantId, String merchantUid) {
        assertActiveMerchant(tenantId, merchantUid);
        return listCampaignsForMerchant(tenantId, merchantUid);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listCampaignsForTenantAdmin(String tenantId, String merchantUid) {
        getMerchantOrThrow(tenantId, merchantUid);
        return listCampaignsForMerchant(tenantId, merchantUid);
    }

    private List<CampaignResponse> listCampaignsForMerchant(String tenantId, String merchantUid) {
        return campaignRepository.findByTenantIdAndMerchantIdOrderByCreatedAtDesc(tenantId, merchantUid)
            .stream()
            .map(c -> campaignService.get(tenantId, c.getCampaignUid()))
            .toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse getMerchantCampaign(String tenantId, String merchantUid, String campaignUid) {
        assertActiveMerchant(tenantId, merchantUid);
        assertCampaignOwnership(tenantId, merchantUid, campaignUid);
        return campaignService.get(tenantId, campaignUid);
    }

    @Transactional
    public CampaignResponse updateMerchantCampaign(
        String tenantId,
        String merchantUid,
        String campaignUid,
        CampaignUpsertRequest request,
        String actorEmail
    ) {
        assertActiveMerchant(tenantId, merchantUid);
        assertCampaignOwnership(tenantId, merchantUid, campaignUid);
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException(campaignUid));
        if (!campaign.isPendingMerchantApproval()
            && campaign.getStatus() != CampaignStatus.DRAFT
            && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new CampaignBadRequestException(
                "Only draft, paused, or pending-approval campaigns can be edited");
        }
        request.setMerchantId(merchantUid);
        request.setCampaignType(MERCHANT_FUNDED);
        return campaignService.update(tenantId, campaignUid, request, actorEmail);
    }

    @Transactional
    public CampaignResponse pauseMerchantCampaign(String tenantId, String merchantUid, String campaignUid) {
        assertActiveMerchant(tenantId, merchantUid);
        assertCampaignOwnership(tenantId, merchantUid, campaignUid);
        return campaignService.pause(tenantId, campaignUid);
    }

    @Transactional
    public CampaignResponse endMerchantCampaign(String tenantId, String merchantUid, String campaignUid) {
        assertActiveMerchant(tenantId, merchantUid);
        assertCampaignOwnership(tenantId, merchantUid, campaignUid);
        return campaignService.end(tenantId, campaignUid);
    }

    @Transactional(readOnly = true)
    public List<com.loyaltyos.campaigns.dto.CampaignParticipationResponse> listMerchantCampaignParticipations(
        String tenantId,
        String merchantUid,
        String campaignUid,
        int limit
    ) {
        assertActiveMerchant(tenantId, merchantUid);
        assertCampaignOwnership(tenantId, merchantUid, campaignUid);
        return campaignAnalyticsService.listRecentParticipations(tenantId, campaignUid, limit);
    }

    private void getMerchantOrThrow(String tenantId, String merchantUid) {
        merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
    }

    @Transactional
    public CampaignResponse upsertMerchantCampaignEventSchema(
        String tenantId,
        String merchantUid,
        String campaignUid,
        CampaignEventSchemaUpsertRequest request
    ) {
        assertActiveMerchant(tenantId, merchantUid);
        assertCampaignOwnership(tenantId, merchantUid, campaignUid);
        return campaignService.upsertEventSchema(tenantId, campaignUid, request);
    }

    private void assertCampaignOwnership(String tenantId, String merchantUid, String campaignUid) {
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException(campaignUid));
        if (campaign.getMerchantId() == null || !merchantUid.equals(campaign.getMerchantId())) {
            throw new MerchantAccessDeniedException("Campaign does not belong to this merchant");
        }
    }

    @Transactional(readOnly = true)
    public CampaignStatsResponse getMerchantCampaignStats(
        String tenantId,
        String merchantUid,
        String campaignUid
    ) {
        assertActiveMerchant(tenantId, merchantUid);
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException(campaignUid));
        if (campaign.getMerchantId() == null || !merchantUid.equals(campaign.getMerchantId())) {
            throw new MerchantAccessDeniedException("Campaign does not belong to this merchant");
        }
        return campaignAnalyticsService.getCampaignStats(tenantId, campaignUid);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listPendingApprovals(String tenantId) {
        return campaignRepository.findByTenantIdAndPendingMerchantApprovalTrueOrderByCreatedAtDesc(tenantId)
            .stream()
            .map(c -> campaignService.get(tenantId, c.getCampaignUid()))
            .toList();
    }

    @Transactional
    public CampaignResponse resumeMerchantCampaign(String tenantId, String merchantUid, String campaignUid) {
        assertActiveMerchant(tenantId, merchantUid);
        assertCampaignOwnership(tenantId, merchantUid, campaignUid);
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException(campaignUid));
        if (campaign.isPendingMerchantApproval()) {
            throw new CampaignBadRequestException(
                "Campaign is pending tenant approval and cannot be resumed directly");
        }
        if (campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new CampaignBadRequestException(
                "Only paused campaigns can be resumed");
        }
        ensureMerchantFundedExecutionMode(campaign);
        campaignRepository.save(campaign);
        return campaignService.activate(tenantId, campaignUid);
    }

    @Transactional
    public CampaignResponse approveMerchantCampaign(String tenantId, String campaignUid, String actorEmail) {
        Campaign campaign = campaignRepository.findByTenantIdAndCampaignUid(tenantId, campaignUid)
            .orElseThrow(() -> new CampaignNotFoundException(campaignUid));
        if (!campaign.isPendingMerchantApproval()) {
            throw new CampaignBadRequestException("Campaign is not pending merchant approval");
        }
        ensureMerchantFundedExecutionMode(campaign);
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

    private void ensureMerchantFundedExecutionMode(Campaign campaign) {
        if (!MERCHANT_FUNDED.equals(campaign.getCampaignType())) {
            return;
        }
        campaign.setExecutionMode(CampaignExecutionMode.LEGACY_OFFER);
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

    @Transactional(readOnly = true)
    public MerchantCampaignAnalyticsResponse buildMerchantAnalytics(String tenantId, String merchantUid) {
        assertActiveMerchant(tenantId, merchantUid);
        List<Campaign> campaigns =
            campaignRepository.findByTenantIdAndMerchantIdOrderByCreatedAtDesc(tenantId, merchantUid);

        List<CampaignStatsResponse> stats = campaigns.stream()
            .map(c -> campaignAnalyticsService.getCampaignStats(tenantId, c.getCampaignUid()))
            .toList();

        long activeCampaigns = campaigns.stream()
            .filter(c -> c.getStatus() == CampaignStatus.ACTIVE)
            .count();

        long totalParticipations = stats.stream().mapToLong(CampaignStatsResponse::getTotalParticipations).sum();
        long totalUniqueCustomers = stats.stream().mapToLong(CampaignStatsResponse::getUniqueCustomersReached).sum();

        BigDecimal totalPointsIssued = stats.stream()
            .map(s -> s.getTotalPointsIssued() != null ? s.getTotalPointsIssued() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCashback = stats.stream()
            .map(s -> s.getTotalCashbackRecorded() != null ? s.getTotalCashbackRecorded() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAllocated = stats.stream()
            .map(s -> s.getBudgetTotal() != null ? s.getBudgetTotal() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalConsumed = stats.stream()
            .map(s -> s.getBudgetConsumed() != null ? s.getBudgetConsumed() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        double budgetPct = totalAllocated.compareTo(BigDecimal.ZERO) == 0 ? 0.0
            : totalConsumed.divide(totalAllocated, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).doubleValue();

        MerchantCampaignAnalyticsResponse response = new MerchantCampaignAnalyticsResponse();
        response.setTotalCampaigns(campaigns.size());
        response.setActiveCampaigns(activeCampaigns);
        response.setTotalParticipations(totalParticipations);
        response.setTotalUniqueCustomers(totalUniqueCustomers);
        response.setTotalPointsIssued(totalPointsIssued);
        response.setTotalCashbackRecorded(totalCashback);
        response.setTotalBudgetAllocated(totalAllocated);
        response.setTotalBudgetConsumed(totalConsumed);
        response.setBudgetConsumedPct(budgetPct);
        response.setCampaignStats(stats);
        return response;
    }

    private void assertActiveMerchant(String tenantId, String merchantUid) {
        var merchant = merchantRepository.findByTenantIdAndMerchantUid(tenantId, merchantUid)
            .orElseThrow(() -> new MerchantNotFoundException(merchantUid));
        if (merchant.getOnboardingStage() != MerchantOnboardingStage.ACTIVE) {
            throw new MerchantAccessDeniedException("Merchant is not active");
        }
    }
}
