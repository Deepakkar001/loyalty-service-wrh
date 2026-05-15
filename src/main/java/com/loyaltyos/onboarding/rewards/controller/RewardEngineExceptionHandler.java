package com.loyaltyos.onboarding.rewards.controller;

import com.loyaltyos.onboarding.rewards.exception.RewardCreditAlreadyReversedException;
import com.loyaltyos.onboarding.rewards.exception.RewardIssuanceAuditWriteException;
import com.loyaltyos.onboarding.rewards.exception.RewardIssuanceValidationException;
import com.loyaltyos.onboarding.rewards.exception.RewardLedgerNotFoundException;
import com.loyaltyos.onboarding.rewards.exception.RewardPartialIdempotencyException;
import com.loyaltyos.onboarding.rewards.exception.RewardReversalValidationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = RewardIssuanceController.class)
public class RewardEngineExceptionHandler {

    @ExceptionHandler(RewardIssuanceAuditWriteException.class)
    public ResponseEntity<Map<String, String>> auditWriteFailed(RewardIssuanceAuditWriteException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "ISSUANCE_AUDIT_FAILED", "message", e.getMessage()));
    }

    @ExceptionHandler(RewardIssuanceValidationException.class)
    public ResponseEntity<Map<String, String>> badRequest(RewardIssuanceValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(RewardPartialIdempotencyException.class)
    public ResponseEntity<Map<String, String>> conflict(RewardPartialIdempotencyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "PARTIAL_IDEMPOTENCY", "message", e.getMessage()));
    }

    @ExceptionHandler(RewardCreditAlreadyReversedException.class)
    public ResponseEntity<Map<String, String>> alreadyReversed(RewardCreditAlreadyReversedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", "ALREADY_REVERSED", "message", e.getMessage()));
    }

    @ExceptionHandler(RewardReversalValidationException.class)
    public ResponseEntity<Map<String, String>> reversalBadRequest(RewardReversalValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(RewardLedgerNotFoundException.class)
    public ResponseEntity<Map<String, String>> ledgerNotFound(RewardLedgerNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "FORBIDDEN", "message", e.getMessage()));
    }
}
