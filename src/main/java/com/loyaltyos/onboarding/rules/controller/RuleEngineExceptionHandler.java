package com.loyaltyos.onboarding.rules.controller;

import com.loyaltyos.onboarding.rules.exception.RuleEngineBadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackageClasses = RuleEvaluationController.class)
public class RuleEngineExceptionHandler {

    @ExceptionHandler(RuleEngineBadRequestException.class)
    public ResponseEntity<Map<String, String>> badRequest(RuleEngineBadRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", "FORBIDDEN", "message", e.getMessage()));
    }
}
