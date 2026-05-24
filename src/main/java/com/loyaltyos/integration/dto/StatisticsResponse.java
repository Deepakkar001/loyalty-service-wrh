package com.loyaltyos.integration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class StatisticsResponse {

    private TimeRange timeRange;
    private int totalRequests;
    private int successful;
    private int failed;
    private BigDecimal successRate;
    private Map<String, Integer> errorBreakdown = new HashMap<>();
    private LatencyStats latency;
    private int eventsProcessed;
    private BigDecimal pointsAwarded;
    private int customersImpacted;

    public static class TimeRange {
        private Instant from;
        private Instant to;

        public Instant getFrom() { return from; }
        public void setFrom(Instant from) { this.from = from; }
        public Instant getTo() { return to; }
        public void setTo(Instant to) { this.to = to; }
    }

    public static class LatencyStats {
        private int p50;
        private int p95;
        private int p99;
        private int avg;

        public int getP50() { return p50; }
        public void setP50(int p50) { this.p50 = p50; }
        public int getP95() { return p95; }
        public void setP95(int p95) { this.p95 = p95; }
        public int getP99() { return p99; }
        public void setP99(int p99) { this.p99 = p99; }
        public int getAvg() { return avg; }
        public void setAvg(int avg) { this.avg = avg; }
    }

    public TimeRange getTimeRange() { return timeRange; }
    public void setTimeRange(TimeRange timeRange) { this.timeRange = timeRange; }
    public int getTotalRequests() { return totalRequests; }
    public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
    public int getSuccessful() { return successful; }
    public void setSuccessful(int successful) { this.successful = successful; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public BigDecimal getSuccessRate() { return successRate; }
    public void setSuccessRate(BigDecimal successRate) { this.successRate = successRate; }
    public Map<String, Integer> getErrorBreakdown() { return errorBreakdown; }
    public void setErrorBreakdown(Map<String, Integer> errorBreakdown) { this.errorBreakdown = errorBreakdown; }
    public LatencyStats getLatency() { return latency; }
    public void setLatency(LatencyStats latency) { this.latency = latency; }
    public int getEventsProcessed() { return eventsProcessed; }
    public void setEventsProcessed(int eventsProcessed) { this.eventsProcessed = eventsProcessed; }
    public BigDecimal getPointsAwarded() { return pointsAwarded; }
    public void setPointsAwarded(BigDecimal pointsAwarded) { this.pointsAwarded = pointsAwarded; }
    public int getCustomersImpacted() { return customersImpacted; }
    public void setCustomersImpacted(int customersImpacted) { this.customersImpacted = customersImpacted; }
}
