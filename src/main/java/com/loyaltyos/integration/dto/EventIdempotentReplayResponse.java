package com.loyaltyos.integration.dto;

import java.time.Instant;

public class EventIdempotentReplayResponse {

    private String status = "IDEMPOTENT_REPLAY";
    private String eventId;
    private String message;
    private EventProcessingResponse originalResult;
    private Instant originalTimestamp;
    private boolean retryable;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public EventProcessingResponse getOriginalResult() { return originalResult; }
    public void setOriginalResult(EventProcessingResponse originalResult) { this.originalResult = originalResult; }
    public Instant getOriginalTimestamp() { return originalTimestamp; }
    public void setOriginalTimestamp(Instant originalTimestamp) { this.originalTimestamp = originalTimestamp; }
    public boolean isRetryable() { return retryable; }
    public void setRetryable(boolean retryable) { this.retryable = retryable; }
}
