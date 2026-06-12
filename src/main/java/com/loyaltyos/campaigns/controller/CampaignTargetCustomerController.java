package com.loyaltyos.campaigns.controller;

import com.loyaltyos.campaigns.config.CampaignProperties;
import com.loyaltyos.campaigns.dto.CampaignTargetCustomerPageResponse;
import com.loyaltyos.campaigns.dto.CampaignTargetUploadResponse;
import com.loyaltyos.campaigns.dto.CampaignTargetUploadSpecResponse;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignConflictException;
import com.loyaltyos.campaigns.service.CampaignTargetCustomerService;
import com.loyaltyos.campaigns.service.CampaignTargetCustomerUploadService;
import com.loyaltyos.campaigns.support.CampaignTargetUploadSpec;
import com.loyaltyos.onboarding.security.TenantJwt;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/campaigns/admin/campaigns")
public class CampaignTargetCustomerController {

    private final CampaignTargetCustomerUploadService uploadService;
    private final CampaignTargetCustomerService targetCustomerService;
    private final CampaignProperties campaignProperties;

    public CampaignTargetCustomerController(
        CampaignTargetCustomerUploadService uploadService,
        CampaignTargetCustomerService targetCustomerService,
        CampaignProperties campaignProperties
    ) {
        this.uploadService = Objects.requireNonNull(uploadService, "uploadService");
        this.targetCustomerService = Objects.requireNonNull(targetCustomerService, "targetCustomerService");
        this.campaignProperties = Objects.requireNonNull(campaignProperties, "campaignProperties");
    }

    @GetMapping("/target-customers/upload-spec")
    public ResponseEntity<CampaignTargetUploadSpecResponse> uploadSpec() {
        return ResponseEntity.ok(CampaignTargetUploadSpec.build(campaignProperties));
    }

    @PostMapping("/{campaignUid}/target-customers/upload")
    @PreAuthorize("hasPermission('campaigns.edit')")
    public ResponseEntity<CampaignTargetUploadResponse> upload(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid,
        @RequestParam("file") MultipartFile file
    ) {
        String tenantId = requireTenant(jwt);
        try {
            CampaignTargetUploadResponse body = uploadService.uploadCsv(tenantId, campaignUid, file);
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            CampaignTargetUploadResponse err = new CampaignTargetUploadResponse();
            err.setStatus("FAILED");
            err.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (CampaignBadRequestException | CampaignConflictException e) {
            CampaignTargetUploadResponse err = new CampaignTargetUploadResponse();
            err.setStatus("FAILED");
            err.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(err);
        }
    }

    @GetMapping("/{campaignUid}/target-customers")
    public ResponseEntity<CampaignTargetCustomerPageResponse> list(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "50") int size,
        @RequestParam(value = "search", required = false) String search
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(targetCustomerService.list(tenantId, campaignUid, page, size, search));
    }

    @DeleteMapping("/{campaignUid}/target-customers/{customerId}")
    @PreAuthorize("hasPermission('campaigns.edit')")
    public ResponseEntity<Void> remove(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid,
        @PathVariable("customerId") String customerId
    ) {
        String tenantId = requireTenant(jwt);
        targetCustomerService.remove(tenantId, campaignUid, customerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{campaignUid}/target-customers/uploads")
    public ResponseEntity<List<CampaignTargetUploadResponse>> listUploads(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("campaignUid") String campaignUid
    ) {
        String tenantId = requireTenant(jwt);
        return ResponseEntity.ok(targetCustomerService.listUploads(tenantId, campaignUid));
    }

    private static String requireTenant(Jwt jwt) {
        String tenantId = TenantJwt.tenantId(jwt);
        if (tenantId == null || tenantId.isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("Missing tenant");
        }
        return tenantId;
    }
}
