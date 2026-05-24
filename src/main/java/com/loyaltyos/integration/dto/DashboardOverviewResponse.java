package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class DashboardOverviewResponse {

    private int totalActiveKeys;
    private int totalRequestsLast24h;
    private int successfulRequests;
    private int failedRequests;
    private BigDecimal errorRate;
    private int avgResponseTime;
    private int p99ResponseTime;
    private Instant lastRequestAt;

    public int getTotalActiveKeys() { return totalActiveKeys; }
    public void setTotalActiveKeys(int totalActiveKeys) { this.totalActiveKeys = totalActiveKeys; }
    public int getTotalRequestsLast24h() { return totalRequestsLast24h; }
    public void setTotalRequestsLast24h(int totalRequestsLast24h) { this.totalRequestsLast24h = totalRequestsLast24h; }
    public int getSuccessfulRequests() { return successfulRequests; }
    public void setSuccessfulRequests(int successfulRequests) { this.successfulRequests = successfulRequests; }
    public int getFailedRequests() { return failedRequests; }
    public void setFailedRequests(int failedRequests) { this.failedRequests = failedRequests; }
    public BigDecimal getErrorRate() { return errorRate; }
    public void setErrorRate(BigDecimal errorRate) { this.errorRate = errorRate; }
    public int getAvgResponseTime() { return avgResponseTime; }
    public void setAvgResponseTime(int avgResponseTime) { this.avgResponseTime = avgResponseTime; }
    public int getP99ResponseTime() { return p99ResponseTime; }
    public void setP99ResponseTime(int p99ResponseTime) { this.p99ResponseTime = p99ResponseTime; }
    public Instant getLastRequestAt() { return lastRequestAt; }
    public void setLastRequestAt(Instant lastRequestAt) { this.lastRequestAt = lastRequestAt; }
}
