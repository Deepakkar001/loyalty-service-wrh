package com.loyaltyos.referrals.dto;

import com.loyaltyos.rewards.dto.RewardIssueCommandDto;

public class ReferralIssuanceLine {

    private String customerId;
    private RewardIssueCommandDto command;

    public ReferralIssuanceLine() {}

    public ReferralIssuanceLine(String customerId, RewardIssueCommandDto command) {
        this.customerId = customerId;
        this.command = command;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public RewardIssueCommandDto getCommand() {
        return command;
    }

    public void setCommand(RewardIssueCommandDto command) {
        this.command = command;
    }
}
