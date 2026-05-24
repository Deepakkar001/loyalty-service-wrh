package com.loyaltyos.integration.dto;

import java.time.Instant;

public class EventProcessingErrorResponse {

    private String status = "ERROR";
    private String errorCode;
    private String errorMessage;
    private Object details;
    private boolean retryable;
    private Integer retryAfterSeconds;
    private String errorId;
    private Instant timestamp = Instant.now();

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }
    public Integer getRetryAfterSeconds() { return retryAfterSeconds; }
    public void setRetryAfterSeconds(Integer retryAfterSeconds) { this.retryAfterSeconds = retryAfterSeconds; }
    public String getErrorId() { return errorId; }
    public void setErrorId(String errorId) { this.errorId = errorId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
