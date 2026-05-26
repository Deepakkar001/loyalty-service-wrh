package com.loyaltyos.integration.controller;

import com.loyaltyos.integration.dto.EventIdempotentReplayResponse;
import com.loyaltyos.integration.dto.EventProcessingResponse;
import com.loyaltyos.integration.dto.EventStatusResponse;
import com.loyaltyos.integration.dto.ValidationResponse;
import com.loyaltyos.integration.dto.IntegrationParsedEvent;
import com.loyaltyos.integration.service.IntegrationEventPayloadResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.loyaltyos.integration.entity.ApiRequestAuditLog;
import com.loyaltyos.integration.security.ApiKeyPrincipal;
import com.loyaltyos.integration.service.IntegrationAuditService;
import com.loyaltyos.integration.service.IntegrationEventService;
import com.loyaltyos.integration.service.IntegrationMetricsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1/integration/{tenantId}")
@Tag(name = "Integration API", description = "Public API-key authenticated event ingestion")
@SuppressWarnings("null")
public class IntegrationApiController {

    private final IntegrationEventService integrationEventService;
    private final IntegrationEventPayloadResolver payloadResolver;
    private final IntegrationAuditService auditService;
    private final IntegrationMetricsService metricsService;

    public IntegrationApiController(
        IntegrationEventService integrationEventService,
        IntegrationEventPayloadResolver payloadResolver,
        IntegrationAuditService auditService,
        IntegrationMetricsService metricsService
    ) {
        this.integrationEventService = Objects.requireNonNull(integrationEventService, "integrationEventService");
        this.payloadResolver = Objects.requireNonNull(payloadResolver, "payloadResolver");
        this.auditService = Objects.requireNonNull(auditService, "auditService");
        this.metricsService = Objects.requireNonNull(metricsService, "metricsService");
    }

    @PostMapping("/events/process")
    public ResponseEntity<?> processEvent(
        @PathVariable String tenantId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @RequestBody JsonNode body,
        HttpServletRequest servletRequest
    ) {
        verifyTenant(auth, tenantId);
        IntegrationParsedEvent parsed = payloadResolver.parseAndValidate(tenantId, body);
        long start = System.currentTimeMillis();
        String bodyRaw = attributeBody(servletRequest);
        String payloadHash = IntegrationEventService.hashPayload(bodyRaw);
        String apiKeyUid = auth.keyUid();

        try {
            Object result = integrationEventService.processEvent(tenantId, parsed, apiKeyUid, bodyRaw, payloadHash);
            int processingMs = (int) (System.currentTimeMillis() - start);
            int httpStatus = 200;

            auditService.logApiRequest(
                tenantId, apiKeyUid, requestId(servletRequest), "POST", servletRequest.getRequestURI(),
                parsed.eventId(), parsed.customerId(), httpStatus, processingMs,
                null, null, payloadHash, clientIp(servletRequest), servletRequest.getHeader("User-Agent")
            );
            metricsService.recordRequest(tenantId, "events/process", httpStatus, processingMs);
            metricsService.recordEventProcessed(tenantId, true);

            return ResponseEntity.ok()
                .header("X-Request-ID", requestId(servletRequest))
                .header("X-Processing-Time", String.valueOf(processingMs))
                .body(result);
        } catch (RuntimeException ex) {
            int processingMs = (int) (System.currentTimeMillis() - start);
            metricsService.recordRequest(tenantId, "events/process", 400, processingMs);
            metricsService.recordEventProcessed(tenantId, false);
            throw ex;
        }
    }

    @PostMapping("/events/validate")
    public ResponseEntity<ValidationResponse> validateEvent(
        @PathVariable String tenantId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @RequestBody JsonNode body,
        HttpServletRequest servletRequest
    ) {
        verifyTenant(auth, tenantId);
        IntegrationParsedEvent parsed = payloadResolver.parseAndValidate(tenantId, body);
        long start = System.currentTimeMillis();
        ValidationResponse response = integrationEventService.validateEvent(tenantId, parsed);
        int processingMs = (int) (System.currentTimeMillis() - start);
        auditService.logApiRequest(
            tenantId, auth.keyUid(), requestId(servletRequest), "POST", servletRequest.getRequestURI(),
            parsed.eventId(), parsed.customerId(), 200, processingMs,
            null, null, IntegrationEventService.hashPayload(attributeBody(servletRequest)),
            clientIp(servletRequest), servletRequest.getHeader("User-Agent")
        );
        metricsService.recordRequest(tenantId, "events/validate", 200, processingMs);
        return ResponseEntity.ok()
            .header("X-Processing-Time", String.valueOf(processingMs))
            .body(response);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventStatusResponse> getEventStatus(
        @PathVariable String tenantId,
        @PathVariable String eventId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        HttpServletRequest servletRequest
    ) {
        verifyTenant(auth, tenantId);
        long start = System.currentTimeMillis();
        ResponseEntity<EventStatusResponse> result = integrationEventService.getEventStatus(tenantId, eventId)
            .map(r -> ResponseEntity.ok(r))
            .orElse(ResponseEntity.notFound().build());
        metricsService.recordRequest(tenantId, "events/status", result.getStatusCode().value(),
            System.currentTimeMillis() - start);
        return result;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<ApiRequestAuditLog>> getAuditLogs(
        @PathVariable String tenantId,
        @AuthenticationPrincipal ApiKeyPrincipal auth,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        HttpServletRequest servletRequest
    ) {
        verifyTenant(auth, tenantId);
        return ResponseEntity.ok(auditService.getAuditLogs(tenantId, PageRequest.of(page, size)));
    }

    private static void verifyTenant(ApiKeyPrincipal auth, String tenantId) {
        if (auth == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                "API key authentication required");
        }
        if (!tenantId.equals(auth.tenantId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Tenant mismatch: URL tenantId does not match API key tenant");
        }
    }

    private static String attributeBody(HttpServletRequest request) {
        Object body = request.getAttribute("integration.requestBody");
        return body != null ? String.valueOf(body) : "";
    }

    private static String requestId(HttpServletRequest request) {
        Object id = request.getAttribute("integration.requestId");
        return id != null ? String.valueOf(id) : java.util.UUID.randomUUID().toString();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
