package com.loyaltyos.onboarding.dto.request;

public class ApproveAgreementRequest {
    private String approvalNotes;

    public ApproveAgreementRequest() {}

    public String getApprovalNotes() {
        return approvalNotes;
    }

    public void setApprovalNotes(String approvalNotes) {
        this.approvalNotes = approvalNotes;
    }
}
