package com.loyaltyos.campaigns.dto;

import java.util.ArrayList;
import java.util.List;

public class CampaignPerformanceReportResponse {

    private CampaignPerformanceSummary summary = new CampaignPerformanceSummary();
    private List<CampaignParticipationTrendRow> dailyParticipations = new ArrayList<>();
    private List<CampaignPerformanceRow> campaigns = new ArrayList<>();

    public CampaignPerformanceSummary getSummary() { return summary; }
    public void setSummary(CampaignPerformanceSummary summary) { this.summary = summary; }
    public List<CampaignParticipationTrendRow> getDailyParticipations() { return dailyParticipations; }
    public void setDailyParticipations(List<CampaignParticipationTrendRow> dailyParticipations) { this.dailyParticipations = dailyParticipations; }
    public List<CampaignPerformanceRow> getCampaigns() { return campaigns; }
    public void setCampaigns(List<CampaignPerformanceRow> campaigns) { this.campaigns = campaigns; }
}
