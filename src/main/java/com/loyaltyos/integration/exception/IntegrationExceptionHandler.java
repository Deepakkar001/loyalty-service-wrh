package com.loyaltyos.integration.exception;

import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.integration.dto.EventProcessingErrorResponse;
import com.loyaltyos.rewards.exception.RewardInsufficientBalanceException;
import com.loyaltyos.rewards.exception.RewardRedemptionLimitExceededException;
import com.loyaltyos.rewards.exception.RewardRedemptionValidationException;
import com.loyaltyos.onboarding.exception.InvalidStateException;
import com.loyaltyos.onboarding.exception.ProgrammeInactiveException;
import com.loyaltyos.onboarding.exception.ProgrammeConfigValidationException;
import com.loyaltyos.onboarding.exception.InvalidStatusTransitionException;
import com.loyaltyos.onboarding.exception.TenantNotFoundException;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import com.loyaltyos.voucher.exception.VoucherOutOfStockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice(basePackages = {
    "com.loyaltyos.integration.controller",
    "com.loyaltyos.voucher.controller"
})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IntegrationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(IntegrationExceptionHandler.class);

    @ExceptionHandler(IntegrationApiException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleIntegration(
        IntegrationApiException ex,
        HttpServletRequest request
    ) {
        return buildError(
            ex.getHttpStatus(),
            ex.getErrorCode(),
            ex.getMessage(),
            ex.isRetryable(),
            ex.getDetails()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fe ->
            fields.put(fe.getField(), fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid")
        );
        EventProcessingErrorResponse body = new EventProcessingErrorResponse();
        body.setErrorCode("VALIDATION_FAILED");
        body.setErrorMessage("Request validation failed");
        body.setDetails(fields);
        body.setRetryable(false);
        body.setTimestamp(Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), false, null);
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleTenantNotFound(TenantNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", ex.getMessage(), false, null);
    }

    @ExceptionHandler(ProgrammeInactiveException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleProgrammeInactive(ProgrammeInactiveException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("programmeUid", ex.getProgrammeUid());
        if (ex.getStatus() != null) {
            details.put("status", ex.getStatus().name());
        }
        HttpStatus status = ex.getStatus() == null ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        String code = ex.getStatus() == null ? "PROGRAMME_NOT_FOUND" : "PROGRAMME_INACTIVE";
        return buildError(status, code, ex.getMessage(), false, details);
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleInvalidState(InvalidStateException ex) {
        String msg = ex.getMessage();
        if (ex.getCurrentStatus() != null || ex.getRequiredStatus() != null) {
            msg = (msg == null ? "Invalid tenant state" : msg)
                + " (currentStatus=" + ex.getCurrentStatus()
                + ", requiredStatus=" + ex.getRequiredStatus() + ")";
        }
        return buildError(HttpStatus.CONFLICT, "INVALID_STATE", msg, false, null);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleInvalidTransition(InvalidStatusTransitionException ex) {
        return buildError(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", ex.getMessage(), false, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleIllegalState(IllegalStateException ex) {
        return buildError(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage(), false, null);
    }

    @ExceptionHandler(RewardRedemptionValidationException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleRedemptionValidation(
        RewardRedemptionValidationException ex
    ) {
        return buildError(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            ex.getMessage(),
            false,
            ex.getFieldErrors().isEmpty() ? null : ex.getFieldErrors()
        );
    }

    @ExceptionHandler(RewardRedemptionLimitExceededException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleRedemptionLimitExceeded(
        RewardRedemptionLimitExceededException ex
    ) {
        return buildError(
            HttpStatus.BAD_REQUEST,
            "REDEMPTION_LIMIT_EXCEEDED",
            ex.getMessage(),
            false,
            ex.getFieldErrors().isEmpty() ? null : ex.getFieldErrors()
        );
    }

    @ExceptionHandler(RewardInsufficientBalanceException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleInsufficientBalance(
        RewardInsufficientBalanceException ex
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("currentBalance", ex.getCurrentBalance());
        details.put("pointsRequested", ex.getPointsRequested());
        return buildError(
            HttpStatus.BAD_REQUEST,
            "INSUFFICIENT_BALANCE",
            ex.getMessage(),
            false,
            details
        );
    }

    @ExceptionHandler(VoucherOutOfStockException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleVoucherOutOfStock(VoucherOutOfStockException ex) {
        return buildError(HttpStatus.CONFLICT, "OUT_OF_STOCK", ex.getMessage(), false, null);
    }

    @ExceptionHandler(VoucherCatalogException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleVoucherCatalog(VoucherCatalogException ex) {
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage(), false, null);
    }

    @ExceptionHandler(CampaignBadRequestException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleCampaignBadRequest(CampaignBadRequestException ex) {
        return buildError(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            ex.getMessage(),
            false,
            ex.getFieldErrors().isEmpty() ? null : ex.getFieldErrors()
        );
    }

    @ExceptionHandler(ProgrammeConfigValidationException.class)
    public ResponseEntity<EventProcessingErrorResponse> handleProgrammeValidation(ProgrammeConfigValidationException ex) {
        return buildError(
            HttpStatus.BAD_REQUEST,
            "VALIDATION_FAILED",
            ex.getMessage(),
            false,
            ex.getFieldErrors()
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, InvalidDataAccessApiUsageException.class})
    public ResponseEntity<EventProcessingErrorResponse> handleIllegalArgument(RuntimeException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("-parameters")) {
            message = message + " Rebuild the backend with Gradle (./gradlew clean bootRun) or enable "
                + "'Store information about method parameters' in the Java compiler settings, then restart.";
        }
        return buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, false, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EventProcessingErrorResponse> handleGeneric(Exception ex) {
        log.error("Integration dashboard/API error: {}", ex.getMessage(), ex);
        String message = "An unexpected error occurred processing your request";
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            message = message + ": " + ex.getMessage();
        }
        return buildError(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            message,
            true,
            null
        );
    }

    private static ResponseEntity<EventProcessingErrorResponse> buildError(
        HttpStatus status,
        String code,
        String message,
        boolean retryable,
        Object details
    ) {
        EventProcessingErrorResponse body = new EventProcessingErrorResponse();
        body.setErrorCode(code);
        body.setErrorMessage(message);
        body.setRetryable(retryable);
        body.setDetails(details);
        body.setTimestamp(Instant.now());
        if (status.value() >= 500) {
            body.setErrorId("err_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
            body.setRetryAfterSeconds(60);
        }
        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            body.setRetryAfterSeconds(60);
        }
        return ResponseEntity.status(status).body(body);
    }
}
