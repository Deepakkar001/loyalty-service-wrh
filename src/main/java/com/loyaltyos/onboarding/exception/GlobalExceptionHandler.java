package com.loyaltyos.onboarding.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.loyaltyos.access.exception.ModuleNotEntitledException;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignConflictException;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
import com.loyaltyos.integration.exception.IntegrationApiException;
import com.loyaltyos.referrals.exception.ReferralException;
import com.loyaltyos.support.exception.SupportException;
import com.loyaltyos.merchants.exception.InvalidMerchantStateTransitionException;
import com.loyaltyos.merchants.exception.MerchantAccessDeniedException;
import com.loyaltyos.merchants.exception.MerchantNotActiveException;
import com.loyaltyos.merchants.exception.MerchantNotFoundException;
import com.loyaltyos.voucher.exception.VoucherCatalogException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTenantNotFound(
            TenantNotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage(), request);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerified(
            EmailNotVerifiedException ex, WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", ex.getMessage(), request);
    }

    @ExceptionHandler(ModuleNotEntitledException.class)
    public ResponseEntity<ErrorResponse> handleModuleNotEntitled(
            ModuleNotEntitledException ex, WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "MODULE_NOT_ENTITLED", ex.getMessage(), request);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            RuntimeException ex, WebRequest request) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "You do not have permission to perform this action.";
        }
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", message, request);
    }

    @ExceptionHandler(DuplicateTenantException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateTenant(
            DuplicateTenantException ex, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_TENANT", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(
            InvalidStatusTransitionException ex, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "INVALID_STATUS_TRANSITION", ex.getMessage(), request);
    }

    @ExceptionHandler(ProgrammeArchiveBlockedException.class)
    public ResponseEntity<ErrorResponse> handleProgrammeArchiveBlocked(
            ProgrammeArchiveBlockedException ex, WebRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(
            Instant.now(),
            HttpStatus.CONFLICT.value(),
            "PROGRAMME_ARCHIVE_BLOCKED",
            ex.getMessage(),
            request.getDescription(false),
            extractTraceId(request),
            ex.getReasons()
        ));
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(
            InvalidStateException ex, WebRequest request) {
        String msg = ex.getMessage();
        if (ex.getCurrentStatus() != null || ex.getRequiredStatus() != null) {
            msg = (msg == null ? "Invalid state" : msg)
                + " (currentStatus=" + ex.getCurrentStatus()
                + ", requiredStatus=" + ex.getRequiredStatus() + ")";
        }
        return buildResponse(HttpStatus.CONFLICT, "INVALID_STATE", msg, request);
    }

    // --- Kafka (disabled): reinstate when KafkaProvisioningException is thrown again ---
    // @ExceptionHandler(KafkaProvisioningException.class)
    // public ResponseEntity<ErrorResponse> handleKafkaProvisioning(
    //         KafkaProvisioningException ex, WebRequest request) {
    //     log.error("Kafka provisioning failed: {}", ex.getMessage(), ex);
    //     return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "KAFKA_PROVISIONING_FAILED",
    //             "Infrastructure provisioning failed. Please try again.", request);
    // }

    @ExceptionHandler(InvalidVerificationCodeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVerificationCode(
            InvalidVerificationCodeException ex, WebRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_VERIFICATION_CODE", ex.getMessage(), request);
    }

    @ExceptionHandler(VerificationRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleVerificationRateLimit(
            VerificationRateLimitException ex, WebRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "VERIFICATION_RATE_LIMITED", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_STATE", ex.getMessage(), request);
    }

    /**
     * Map programmatic argument errors to a clean 400 with the actual message preserved
     * (instead of letting them fall through to the catch-all 500 with "An unexpected error
     * occurred"). Common sources: "Unknown business category: X", missing required reason, etc.
     */
    @ExceptionHandler(ProgrammeInactiveException.class)
    public ResponseEntity<ErrorResponse> handleProgrammeInactive(
            ProgrammeInactiveException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "PROGRAMME_INACTIVE", ex.getMessage(), request);
    }

    @ExceptionHandler(VoucherCatalogException.class)
    public ResponseEntity<ErrorResponse> handleVoucherCatalog(
            VoucherCatalogException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", ex.getMessage(), request);
    }

    @ExceptionHandler(MerchantNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMerchantNotFound(
            MerchantNotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "MERCHANT_NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(MerchantNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleMerchantNotActive(
            MerchantNotActiveException ex, WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "MERCHANT_NOT_ACTIVE", ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidMerchantStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleMerchantStateTransition(
            InvalidMerchantStateTransitionException ex, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "INVALID_MERCHANT_STATE_TRANSITION", ex.getMessage(), request);
    }

    @ExceptionHandler(MerchantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleMerchantAccessDenied(
            MerchantAccessDeniedException ex, WebRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "MERCHANT_ACCESS_DENIED", ex.getMessage(), request);
    }

    /**
     * Safety net when {@link IntegrationApiException} is thrown outside
     * {@link com.loyaltyos.integration.exception.IntegrationExceptionHandler} scope.
     */
    @ExceptionHandler(IntegrationApiException.class)
    public ResponseEntity<ErrorResponse> handleIntegrationApi(IntegrationApiException ex, WebRequest request) {
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("Integration API error [{}]: {}", ex.getErrorCode(), ex.getMessage(), ex);
        } else {
            log.info("Integration API client error [{}]: {}", ex.getErrorCode(), ex.getMessage());
        }
        return buildResponse(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ReferralException.class)
    public ResponseEntity<ErrorResponse> handleReferral(ReferralException ex, WebRequest request) {
        HttpStatus status = referralHttpStatus(ex.getCode());
        return buildResponse(status, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(SupportException.class)
    public ResponseEntity<ErrorResponse> handleSupport(SupportException ex, WebRequest request) {
        HttpStatus status = supportHttpStatus(ex.getCode());
        return buildResponse(status, ex.getCode(), ex.getMessage(), request);
    }

    private static HttpStatus supportHttpStatus(String code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (code) {
            case "CASE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "RATE_LIMIT" -> HttpStatus.TOO_MANY_REQUESTS;
            case "INVALID_STATUS_TRANSITION" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private static HttpStatus referralHttpStatus(String code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        return switch (code) {
            case "REFERRAL_PROGRAMME_NOT_FOUND", "REFERRAL_NOT_FOUND", "REFERRAL_CODE_NOT_FOUND" ->
                HttpStatus.NOT_FOUND;
            case "REFERRAL_FRAUD" -> HttpStatus.FORBIDDEN;
            case "REFERRAL_CAP_EXCEEDED", "REFERRAL_POINTS_BUDGET_EXCEEDED", "REFEREE_NOT_NEW" ->
                HttpStatus.CONFLICT;
            case "CODE_GENERATION_FAILED" -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        String msg = ex.getMessage();
        if (msg != null && msg.startsWith("Failed to evaluate expression")) {
            String detail = resolveSecurityExpressionFailure(ex);
            return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED", detail, request);
        }
        boolean looksLikeNotFound = msg != null
                && (msg.toLowerCase().startsWith("unknown ")
                        || msg.toLowerCase().contains(" not found"));
        if (looksLikeNotFound) {
            return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", msg, request);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg, request);
    }

    private static String resolveSecurityExpressionFailure(IllegalArgumentException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            String causeMsg = cause.getMessage();
            if (causeMsg != null && !causeMsg.isBlank()
                && !causeMsg.startsWith("Failed to evaluate expression")) {
                return causeMsg;
            }
            cause = cause.getCause();
        }
        return "You do not have permission to perform this action.";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            fieldErrors.put(fieldName, error.getDefaultMessage());
        });
        ErrorResponse response = new ErrorResponse(
            Instant.now(), HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "VALIDATION_FAILED", "Request validation failed",
            request.getDescription(false), extractTraceId(request), fieldErrors
        );
        return ResponseEntity.unprocessableEntity().body(response);
    }

    @ExceptionHandler(ProgrammeConfigValidationException.class)
    public ResponseEntity<ErrorResponse> handleProgrammeConfigValidation(
            ProgrammeConfigValidationException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            Instant.now(), HttpStatus.UNPROCESSABLE_ENTITY.value(),
            "VALIDATION_FAILED", ex.getMessage(),
            request.getDescription(false), extractTraceId(request),
            ex.getFieldErrors()
        );
        return ResponseEntity.unprocessableEntity().body(response);
    }

    @ExceptionHandler(CampaignBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleCampaignBadRequest(
            CampaignBadRequestException ex, WebRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(CampaignNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCampaignNotFound(
            CampaignNotFoundException ex, WebRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler(CampaignConflictException.class)
    public ResponseEntity<ErrorResponse> handleCampaignConflict(
            CampaignConflictException ex, WebRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableJson(
            HttpMessageNotReadableException ex, WebRequest request) {
        // Common cause: invalid enum value in JSON (e.g., identityMode typo)
        log.warn("Malformed JSON request: {}", ex.getMessage());

        String message = "Malformed JSON request body";
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            // Keep it short but useful for Swagger callers
            message = cause.getMessage();
            if (message.length() > 500) {
                message = message.substring(0, 500);
            }
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String error, String message, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            Instant.now(), status.value(), error, message,
            request.getDescription(false), extractTraceId(request), null
        );
        return ResponseEntity.status(status).body(response);
    }

    private String extractTraceId(WebRequest request) {
        if (!(request instanceof ServletWebRequest servletWebRequest)) {
            return null;
        }

        var http = servletWebRequest.getRequest();
        String b3 = http.getHeader("X-B3-TraceId");
        if (b3 != null && !b3.isBlank()) return b3;

        // W3C traceparent: version-traceId-spanId-flags
        String traceparent = http.getHeader("traceparent");
        if (traceparent != null && !traceparent.isBlank()) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 2 && !parts[1].isBlank()) return parts[1];
        }

        return null;
    }

    public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String traceId,
        Map<String, String> fieldErrors
    ) {}
}

