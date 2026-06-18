package com.loyaltyos.merchants.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateMerchantContactRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String contactEmail;

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
}
