package com.loyaltyos.onboarding.dto.request;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.validation.IsoCountryCode;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterTenantRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 255, message = "Company name must be between 2 and 255 characters")
    private String companyName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, max = 100, message = "Password must be at least 12 characters")
    private String password;

    @NotBlank(message = "Business category is required")
    @Size(max = 64)
    private String businessCategory;

    @Size(max = 100, message = "Custom industry name must be at most 100 characters")
    private String customBusinessCategory;

    @Size(max = 255)
    private String legalBusinessName;

    @Size(max = 100)
    private String businessRegistrationNo;

    @Size(max = 100)
    private String subCategory;

    private String businessModel;

    private Integer numberOfLocations;

    @NotBlank(message = "Country code is required")
    @IsoCountryCode
    private String countryCode;

    @Size(max = 2000)
    private String headquartersAddress;

    @Size(max = 500)
    private String founderNames;

    private Integer yearFounded;

    private String annualRevenueRange;

    private Integer customerBaseSize;

    private String paymentMethodsAccepted;

    @NotNull(message = "Identity mode is required")
    private IdentityMode identityMode;

    @NotNull(message = "Data residency region is required")
    private DataResidencyRegion dataResidencyRegion;

    // Optional fields
    @Pattern(regexp = "https?://.*", message = "Website URL must start with http:// or https://")
    private String websiteUrl;

    private String timezone;

    // Contact information — at least one PRIMARY_ADMIN contact required
    @NotBlank(message = "Primary contact name is required")
    private String primaryContactName;

    @NotBlank(message = "Primary contact email is required")
    @Email(message = "Primary contact email must be valid")
    private String primaryContactEmail;

    private String primaryContactPhone;

    private String primaryContactDesignation;
}

