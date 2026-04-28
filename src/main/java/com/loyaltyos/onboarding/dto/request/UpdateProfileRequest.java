package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 255)
    private String companyName;

    @Size(max = 255)
    private String legalBusinessName;

    @Size(max = 100)
    private String businessRegistrationNo;

    @NotBlank(message = "Business category is required")
    @Size(max = 64)
    private String businessCategory;

    @Size(max = 100)
    private String customBusinessCategory;

    @Size(max = 100)
    private String subCategory;

    private String businessModel;

    private Integer numberOfLocations;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 2)
    private String countryCode;

    @Size(max = 2000)
    private String headquartersAddress;

    @Size(max = 500)
    private String founderNames;

    private Integer yearFounded;

    private String annualRevenueRange;

    private Integer customerBaseSize;

    private String paymentMethodsAccepted;

    @Size(max = 500)
    private String websiteUrl;

    @Size(max = 100)
    private String timezone;

    @NotBlank(message = "Contact name is required")
    @Size(max = 255)
    private String primaryContactName;

    @NotBlank(message = "Contact email is required")
    @Size(max = 255)
    private String primaryContactEmail;

    @Size(max = 30)
    private String primaryContactPhone;

    @Size(max = 255)
    private String primaryContactDesignation;
}
