package com.loyaltyos.integration.service;

import com.loyaltyos.integration.entity.ApiRequestAuditLog;
import com.loyaltyos.integration.entity.CredentialAccessLog;
import com.loyaltyos.integration.enums.CredentialAccessType;
import com.loyaltyos.integration.repository.ApiRequestAuditLogRepository;
import com.loyaltyos.integration.repository.CredentialAccessLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class IntegrationAuditService {

    private final ApiRequestAuditLogRepository apiRequestAuditLogRepository;
    private final CredentialAccessLogRepository credentialAccessLogRepository;

    public IntegrationAuditService(
        ApiRequestAuditLogRepository apiRequestAuditLogRepository,
        CredentialAccessLogRepository credentialAccessLogRepository
    ) {
        this.apiRequestAuditLogRepository = Objects.requireNonNull(apiRequestAuditLogRepository, "apiRequestAuditLogRepository");
        this.credentialAccessLogRepository = Objects.requireNonNull(credentialAccessLogRepository, "credentialAccessLogRepository");
    }

    @Transactional
    public void logApiRequest(
        String tenantId,
        String apiKeyUid,
        String requestId,
        String httpMethod,
        String requestPath,
        String eventId,
        String customerId,
        int httpStatus,
        int processingTimeMs,
        String errorCode,
        String errorMessage,
        String requestPayloadHash,
        String ipAddress,
        String userAgent
    ) {
        ApiRequestAuditLog row = new ApiRequestAuditLog();
        row.setTenantId(tenantId);
        row.setApiKeyUid(apiKeyUid);
        row.setRequestId(requestId != null && !requestId.isBlank() ? requestId : UUID.randomUUID().toString());
        row.setHttpMethod(httpMethod);
        row.setRequestPath(requestPath);
        row.setEventId(eventId);
        row.setCustomerId(customerId);
        row.setHttpStatus(httpStatus);
        row.setProcessingTimeMs(processingTimeMs);
        row.setErrorCode(errorCode);
        row.setErrorMessage(errorMessage);
        row.setRequestPayloadHash(requestPayloadHash);
        row.setIpAddress(ipAddress != null ? ipAddress : "0.0.0.0");
        row.setUserAgent(userAgent);
        apiRequestAuditLogRepository.save(row);
    }

    @Transactional
    public void logCredentialAccess(
        String tenantId,
        String apiKeyUid,
        CredentialAccessType accessType,
        String userId,
        String ipAddress,
        String reason
    ) {
        CredentialAccessLog row = new CredentialAccessLog();
        row.setTenantId(tenantId);
        row.setApiKeyUid(apiKeyUid);
        row.setAccessType(accessType);
        row.setUserId(userId);
        row.setIpAddress(ipAddress != null ? ipAddress : "0.0.0.0");
        row.setReason(reason);
        credentialAccessLogRepository.save(row);
    }

    @Transactional(readOnly = true)
    public Page<ApiRequestAuditLog> getAuditLogs(String tenantId, Pageable pageable) {
        return apiRequestAuditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
    }

    @Transactional(readOnly = true)
    public List<ApiRequestAuditLog> getRecentAuditLogs(String tenantId) {
        return apiRequestAuditLogRepository.findTop10ByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}
