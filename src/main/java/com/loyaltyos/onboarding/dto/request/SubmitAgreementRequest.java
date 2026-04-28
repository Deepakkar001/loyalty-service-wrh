package com.loyaltyos.onboarding.dto.request;

import com.loyaltyos.onboarding.domain.enums.SettlementFrequency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SubmitAgreementRequest {

    @NotBlank(message = "Terms version is required")
    @Size(max = 20, message = "Terms version must be at most 20 characters")
    private String termsVersion;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    @NotNull(message = "Revenue share % is required")
    @DecimalMin(value = "0.0", message = "Revenue share % must be at least 0")
    @DecimalMax(value = "100.0", message = "Revenue share % must be at most 100")
    private BigDecimal revenueSharePct;

    @NotNull(message = "Settlement frequency is required")
    private SettlementFrequency settlementFrequency;

    @NotBlank(message = "Points currency is required")
    @Size(max = 10)
    private String pointsCurrency;

    private Integer expectedDailyTxnVolume;

    @Size(max = 255)
    private String billingContactName;

    @Size(max = 2000)
    private String billingAddress;

    @Size(max = 30)
    private String paymentMethod;

    private Integer contractDurationMonths;

    private Boolean autoRenewal;

    @NotBlank(message = "Signed by name is required")
    @Size(min = 2, max = 255, message = "Signed by name must be between 2 and 255 characters")
    private String signedByName;

    @NotBlank(message = "Signed by email is required")
    @Email(message = "Signed by email must be a valid email address")
    private String signedByEmail;

    @Size(max = 255, message = "Designation must be at most 255 characters")
    private String signedByDesignation;
}

