package com.loyaltyos.onboarding.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import com.loyaltyos.campaigns.exception.CampaignBadRequestException;
import com.loyaltyos.campaigns.exception.CampaignConflictException;
import com.loyaltyos.campaigns.exception.CampaignNotFoundException;
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
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        String msg = ex.getMessage();
        boolean looksLikeNotFound = msg != null
                && (msg.toLowerCase().startsWith("unknown ")
                        || msg.toLowerCase().contains(" not found"));
        if (looksLikeNotFound) {
            return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", msg, request);
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", msg, request);
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

