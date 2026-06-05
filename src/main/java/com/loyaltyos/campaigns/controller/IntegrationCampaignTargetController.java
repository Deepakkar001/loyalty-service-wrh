package com.loyaltyos.campaigns.controller;

import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.dto.CampaignResponse;
import com.loyaltyos.campaigns.dto.CampaignTargetCustomerPageResponse;
import com.loyaltyos.campaigns.dto.CampaignTargetCustomersBulkRequest;
import com.loyaltyos.campaigns.dto.CampaignTargetUploadResponse;
import com.loyaltyos.campaigns.dto.CampaignTargetUploadSpecResponse;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignConflictException;
import com.loyaltyos.campaigns.service.CampaignService;
import com.loyaltyos.campaigns.service.CampaignTargetCustomerService;
import com.loyaltyos.campaigns.service.CampaignTargetCustomerUploadService;
import com.loyaltyos.campaigns.support.CampaignTargetUploadSpec;
import com.loyaltyos.integration.security.ApiKeyPrincipal;
import com.loyaltyos.integration.service.IntegrationAuditService;
import com.loyaltyos.integration.service.IntegrationEventService;
import com.loyaltyos.integration.service.IntegrationMetricsService;
import com.loyaltyos.integration.support.IntegrationAuthSupport;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Public integration APIs for targeted campaign audience management (API key + HMAC).
 * Campaign create/activate remains portal/JWT admin; tenants sync customer lists here.
 */
@RestController
@RequestMapping("/api/v1/integration/{tenantId}/campaigns")
@Tag(name = "Integration Campaigns", description = "Targeted campaign customer lists for tenant applications")
public class IntegrationCampaignTargetController {

    private final CampaignTargetCustomerUploadService uploadService;
    private final CampaignTargetCustomerService targetCustomerService;
    private final CampaignService campaignService;
    private final CampaignProperties campaignProperties;
    private final IntegrationAuditService auditService;
    private final IntegrationMetricsService metricsService;

    public IntegrationCampaignTargetController(
        CampaignTargetCustomerUploadService uploadService,
        CampaignTargetCustomerService targetCustomerService,
        CampaignService campaignService,
        CampaignProperties campaignProperties,
        IntegrationAuditService auditService,
        IntegrationMetricsService metricsService
    ) {
        this.uploadService = Objects.requireNonNull(uploadService, "uploadService");
        this.targetCustomerService = Objects.requireNonNull(targetCustomerService, "targetCustomerService");
        this.campaignService = Objects.requireNonNull(campaignService, "campaignService");
        this.campaignProperties = Objects.requireNonNull(campaignProperties, "campaignProperties");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService");
    }

    @GetMapping("/target-customers/upload-spec")
    public ResponseEntity<CampaignTargetUploadSpecResponse> uploadSpec(
        @PathVariable String tenantId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        logGet(tenantId, auth, servletRequest, 200, "campaigns/target-customers/upload-spec");
        return ResponseEntity.ok(CampaignTargetUploadSpec.build(campaignProperties));
    }

    @GetMapping("/{campaignUid}/audience")
    public ResponseEntity<CampaignResponse> audienceSummary(
        @PathVariable String tenantId,
        @PathVariable String campaignUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        CampaignResponse body = campaignService.get(tenantId, campaignUid);
        logGet(tenantId, auth, servletRequest, 200, "campaigns/audience");
        return ResponseEntity.ok(body);
    }

    @PostMapping(
        value = "/{campaignUid}/target-customers/upload",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CampaignTargetUploadResponse> uploadCsv(
        @PathVariable String tenantId,
        @PathVariable String campaignUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @RequestParam("file") MultipartFile file,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        String actor = integrationActor(auth);
        try {
            CampaignTargetUploadResponse body = uploadService.uploadCsv(
                tenantId,
                campaignUid,
                file,
                actor
            );
            int status = "FAILED".equals(body.getStatus()) ? 400 : 200;
            logWrite(tenantId, auth, servletRequest, "POST", status, "campaigns/target-customers/upload");
            return status == 200 ? ResponseEntity.ok(body) : ResponseEntity.badRequest().body(body);
        } catch (CampaignBadRequestException | CampaignConflictException e) {
            CampaignTargetUploadResponse err = failedUpload(e.getMessage());
            logWrite(tenantId, auth, servletRequest, "POST", 400, "campaigns/target-customers/upload");
            return ResponseEntity.badRequest().body(err);
        }
    }

    @PostMapping("/{campaignUid}/target-customers/bulk")
    public ResponseEntity<CampaignTargetUploadResponse> bulkAdd(
        @PathVariable String tenantId,
        @PathVariable String campaignUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @Valid @RequestBody CampaignTargetCustomersBulkRequest request,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        String actor = integrationActor(auth);
        try {
            CampaignTargetUploadResponse body = uploadService.uploadCustomerIds(
                tenantId,
                campaignUid,
                request.getCustomerIds(),
                actor
            );
            int status = "FAILED".equals(body.getStatus()) ? 400 : 200;
            logWrite(tenantId, auth, servletRequest, "POST", status, "campaigns/target-customers/bulk");
            return status == 200 ? ResponseEntity.ok(body) : ResponseEntity.badRequest().body(body);
        } catch (CampaignBadRequestException | CampaignConflictException e) {
            CampaignTargetUploadResponse err = failedUpload(e.getMessage());
            logWrite(tenantId, auth, servletRequest, "POST", 400, "campaigns/target-customers/bulk");
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/{campaignUid}/target-customers")
    public ResponseEntity<CampaignTargetCustomerPageResponse> list(
        @PathVariable String tenantId,
        @PathVariable String campaignUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "search", required = false) String search,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        CampaignTargetCustomerPageResponse body = targetCustomerService.list(
            tenantId,
            campaignUid,
            page,
            size,
            search
        );
        logGet(tenantId, auth, servletRequest, 200, "campaigns/target-customers/list");
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{campaignUid}/target-customers/{customerId}")
    public ResponseEntity<Void> remove(
        @PathVariable String tenantId,
        @PathVariable String campaignUid,
        @PathVariable String customerId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        targetCustomerService.remove(tenantId, campaignUid, customerId);
        logWrite(tenantId, auth, servletRequest, "DELETE", 204, "campaigns/target-customers/remove");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{campaignUid}/target-customers/uploads")
    public ResponseEntity<List<CampaignTargetUploadResponse>> listUploads(
        @PathVariable String tenantId,
        @PathVariable String campaignUid,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        IntegrationAuthSupport.verifyTenant(auth, tenantId);
        List<CampaignTargetUploadResponse> body = targetCustomerService.listUploads(tenantId, campaignUid);
        logGet(tenantId, auth, servletRequest, 200, "campaigns/target-customers/uploads");
        return ResponseEntity.ok(body);
    }

    private static String integrationActor(ApiKeyPrincipal auth) {
        return "integration:" + auth.keyUid();
    }

    private static CampaignTargetUploadResponse failedUpload(String message) {
        CampaignTargetUploadResponse err = new CampaignTargetUploadResponse();
        err.setStatus("FAILED");
        err.setErrorMessage(message);
        return err;
    }

    private void logGet(
        String tenantId,
        ApiKeyPrincipal auth,
        HttpServletRequest servletRequest,
        int status,
        String metricKey
    ) {
        auditService.logApiRequest(
            tenantId,
            auth.keyUid(),
            IntegrationAuthSupport.requestId(servletRequest),
            "GET",
            servletRequest.getRequestURI(),
            null,
            null,
            status,
            0,
            null,
            null,
            IntegrationEventService.hashPayload(""),
            IntegrationAuthSupport.clientIp(servletRequest),
            servletRequest.getHeader("User-Agent")
        );
        metricsService.recordRequest(tenantId, metricKey, status, 0);
    }

    private void logWrite(
        String tenantId,
        ApiKeyPrincipal auth,
        HttpServletRequest servletRequest,
        String method,
        int status,
        String metricKey
    ) {
        String bodyRaw = IntegrationAuthSupport.attributeBody(servletRequest);
        auditService.logApiRequest(
            tenantId,
            auth.keyUid(),
            IntegrationAuthSupport.requestId(servletRequest),
            method,
            servletRequest.getRequestURI(),
            null,
            null,
            status,
            0,
            null,
            null,
            IntegrationEventService.hashPayload(bodyRaw),
            IntegrationAuthSupport.clientIp(servletRequest),
            servletRequest.getHeader("User-Agent")
        );
        metricsService.recordRequest(tenantId, metricKey, status, 0);
    }
}
