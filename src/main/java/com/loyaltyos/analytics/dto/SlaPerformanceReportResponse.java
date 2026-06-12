package com.loyaltyos.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SlaPerformanceReportResponse {

    private String programmeUid;
    private String fromDate;
    private String toDate;
    private String reportDefinition;
    private String overallSlaStatus;

    private long totalApiRequests;
    private BigDecimal overallApiSuccessRatePct;
    private Integer overallApiP99LatencyMs;
    private long totalIssuanceAttempts;
    private BigDecimal issuanceSuccessRatePct;
    private Integer issuanceP99LatencyMs;
    private long totalEventProcessingAttempts;
    private BigDecimal eventProcessingSuccessRatePct;
    private Integer eventProcessingP99LatencyMs;

    private BigDecimal priorPeriodApiSuccessRatePct;
    private BigDecimal periodOverPeriodApiSuccessChangePct;

    private List<SlaComponentMetricRow> components = new ArrayList<>();
    private List<SlaEndpointMetricRow> endpointBreakdown = new ArrayList<>();
    private List<SlaDailyTrendRow> dailyTrend = new ArrayList<>();

    public String getProgrammeUid() { return programmeUid; }
    public void setProgrammeUid(String programmeUid) { this.programmeUid = programmeUid; }
    public String getFromDate() { return fromDate; }
    public void setFromDate(String fromDate) { this.fromDate = fromDate; }
    public String getToDate() { return toDate; }
    public void setToDate(String toDate) { this.toDate = toDate; }
    public String getReportDefinition() { return reportDefinition; }
    public void setReportDefinition(String reportDefinition) { this.reportDefinition = reportDefinition; }
    public String getOverallSlaStatus() { return overallSlaStatus; }
    public void setOverallSlaStatus(String overallSlaStatus) { this.overallSlaStatus = overallSlaStatus; }
    public long getTotalApiRequests() { return totalApiRequests; }
    public void setTotalApiRequests(long totalApiRequests) { this.totalApiRequests = totalApiRequests; }
    public BigDecimal getOverallApiSuccessRatePct() { return overallApiSuccessRatePct; }
    public void setOverallApiSuccessRatePct(BigDecimal overallApiSuccessRatePct) { this.overallApiSuccessRatePct = overallApiSuccessRatePct; }
    public Integer getOverallApiP99LatencyMs() { return overallApiP99LatencyMs; }
    public void setOverallApiP99LatencyMs(Integer overallApiP99LatencyMs) { this.overallApiP99LatencyMs = overallApiP99LatencyMs; }
    public long getTotalIssuanceAttempts() { return totalIssuanceAttempts; }
    public void setTotalIssuanceAttempts(long totalIssuanceAttempts) { this.totalIssuanceAttempts = totalIssuanceAttempts; }
    public BigDecimal getIssuanceSuccessRatePct() { return issuanceSuccessRatePct; }
    public void setIssuanceSuccessRatePct(BigDecimal issuanceSuccessRatePct) { this.issuanceSuccessRatePct = issuanceSuccessRatePct; }
    public Integer getIssuanceP99LatencyMs() { return issuanceP99LatencyMs; }
    public void setIssuanceP99LatencyMs(Integer issuanceP99LatencyMs) { this.issuanceP99LatencyMs = issuanceP99LatencyMs; }
    public long getTotalEventProcessingAttempts() { return totalEventProcessingAttempts; }
    public void setTotalEventProcessingAttempts(long totalEventProcessingAttempts) { this.totalEventProcessingAttempts = totalEventProcessingAttempts; }
    public BigDecimal getEventProcessingSuccessRatePct() { return eventProcessingSuccessRatePct; }
    public void setEventProcessingSuccessRatePct(BigDecimal eventProcessingSuccessRatePct) { this.eventProcessingSuccessRatePct = eventProcessingSuccessRatePct; }
    public Integer getEventProcessingP99LatencyMs() { return eventProcessingP99LatencyMs; }
    public void setEventProcessingP99LatencyMs(Integer eventProcessingP99LatencyMs) { this.eventProcessingP99LatencyMs = eventProcessingP99LatencyMs; }
    public BigDecimal getPriorPeriodApiSuccessRatePct() { return priorPeriodApiSuccessRatePct; }
    public void setPriorPeriodApiSuccessRatePct(BigDecimal priorPeriodApiSuccessRatePct) { this.priorPeriodApiSuccessRatePct = priorPeriodApiSuccessRatePct; }
    public BigDecimal getPeriodOverPeriodApiSuccessChangePct() { return periodOverPeriodApiSuccessChangePct; }
    public void setPeriodOverPeriodApiSuccessChangePct(BigDecimal periodOverPeriodApiSuccessChangePct) { this.periodOverPeriodApiSuccessChangePct = periodOverPeriodApiSuccessChangePct; }
    public List<SlaComponentMetricRow> getComponents() { return components; }
    public void setComponents(List<SlaComponentMetricRow> components) { this.components = components; }
    public List<SlaEndpointMetricRow> getEndpointBreakdown() { return endpointBreakdown; }
    public void setEndpointBreakdown(List<SlaEndpointMetricRow> endpointBreakdown) { this.endpointBreakdown = endpointBreakdown; }
    public List<SlaDailyTrendRow> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(List<SlaDailyTrendRow> dailyTrend) { this.dailyTrend = dailyTrend; }
}
