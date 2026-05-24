package com.loyaltyos.integration.exception;

import org.springframework.http.HttpStatus;

public class IntegrationApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;
    private final boolean retryable;
    private final Object details;

    public IntegrationApiException(
        HttpStatus httpStatus,
        String errorCode,
        String message,
        boolean retryable
    ) {
        this(httpStatus, errorCode, message, retryable, null);
    }

    public IntegrationApiException(
        HttpStatus httpStatus,
        String errorCode,
        String message,
        boolean retryable,
        Object details
    ) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.details = details;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Object getDetails() {
        return details;
    }
}
