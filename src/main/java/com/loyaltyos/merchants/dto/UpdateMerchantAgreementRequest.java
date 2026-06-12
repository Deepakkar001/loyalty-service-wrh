package com.loyaltyos.merchants.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateMerchantAgreementRequest {

    @NotBlank
    private String agreementDocumentUrl;

    @NotNull
    private Boolean agreementAccepted;

    public String getAgreementDocumentUrl() { return agreementDocumentUrl; }
    public void setAgreementDocumentUrl(String agreementDocumentUrl) { this.agreementDocumentUrl = agreementDocumentUrl; }

    public Boolean getAgreementAccepted() { return agreementAccepted; }
    public void setAgreementAccepted(Boolean agreementAccepted) { this.agreementAccepted = agreementAccepted; }
}
