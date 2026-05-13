package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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

    public UpdateProfileRequest() {}

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getLegalBusinessName() { return legalBusinessName; }
    public void setLegalBusinessName(String legalBusinessName) { this.legalBusinessName = legalBusinessName; }
    public String getBusinessRegistrationNo() { return businessRegistrationNo; }
    public void setBusinessRegistrationNo(String businessRegistrationNo) { this.businessRegistrationNo = businessRegistrationNo; }
    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }
    public String getCustomBusinessCategory() { return customBusinessCategory; }
    public void setCustomBusinessCategory(String customBusinessCategory) { this.customBusinessCategory = customBusinessCategory; }
    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }
    public String getBusinessModel() { return businessModel; }
    public void setBusinessModel(String businessModel) { this.businessModel = businessModel; }
    public Integer getNumberOfLocations() { return numberOfLocations; }
    public void setNumberOfLocations(Integer numberOfLocations) { this.numberOfLocations = numberOfLocations; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getHeadquartersAddress() { return headquartersAddress; }
    public void setHeadquartersAddress(String headquartersAddress) { this.headquartersAddress = headquartersAddress; }
    public String getFounderNames() { return founderNames; }
    public void setFounderNames(String founderNames) { this.founderNames = founderNames; }
    public Integer getYearFounded() { return yearFounded; }
    public void setYearFounded(Integer yearFounded) { this.yearFounded = yearFounded; }
    public String getAnnualRevenueRange() { return annualRevenueRange; }
    public void setAnnualRevenueRange(String annualRevenueRange) { this.annualRevenueRange = annualRevenueRange; }
    public Integer getCustomerBaseSize() { return customerBaseSize; }
    public void setCustomerBaseSize(Integer customerBaseSize) { this.customerBaseSize = customerBaseSize; }
    public String getPaymentMethodsAccepted() { return paymentMethodsAccepted; }
    public void setPaymentMethodsAccepted(String paymentMethodsAccepted) { this.paymentMethodsAccepted = paymentMethodsAccepted; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getPrimaryContactName() { return primaryContactName; }
    public void setPrimaryContactName(String primaryContactName) { this.primaryContactName = primaryContactName; }
    public String getPrimaryContactEmail() { return primaryContactEmail; }
    public void setPrimaryContactEmail(String primaryContactEmail) { this.primaryContactEmail = primaryContactEmail; }
    public String getPrimaryContactPhone() { return primaryContactPhone; }
    public void setPrimaryContactPhone(String primaryContactPhone) { this.primaryContactPhone = primaryContactPhone; }
    public String getPrimaryContactDesignation() { return primaryContactDesignation; }
    public void setPrimaryContactDesignation(String primaryContactDesignation) { this.primaryContactDesignation = primaryContactDesignation; }
}
