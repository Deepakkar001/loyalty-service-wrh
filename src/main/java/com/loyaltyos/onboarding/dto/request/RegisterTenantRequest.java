package com.loyaltyos.onboarding.dto.request;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import com.loyaltyos.onboarding.validation.IsoCountryCode;
import jakarta.validation.constraints.*;

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

    public RegisterTenantRequest() {}

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getBusinessCategory() { return businessCategory; }
    public void setBusinessCategory(String businessCategory) { this.businessCategory = businessCategory; }
    public String getCustomBusinessCategory() { return customBusinessCategory; }
    public void setCustomBusinessCategory(String customBusinessCategory) { this.customBusinessCategory = customBusinessCategory; }
    public String getLegalBusinessName() { return legalBusinessName; }
    public void setLegalBusinessName(String legalBusinessName) { this.legalBusinessName = legalBusinessName; }
    public String getBusinessRegistrationNo() { return businessRegistrationNo; }
    public void setBusinessRegistrationNo(String businessRegistrationNo) { this.businessRegistrationNo = businessRegistrationNo; }
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
    public IdentityMode getIdentityMode() { return identityMode; }
    public void setIdentityMode(IdentityMode identityMode) { this.identityMode = identityMode; }
    public DataResidencyRegion getDataResidencyRegion() { return dataResidencyRegion; }
    public void setDataResidencyRegion(DataResidencyRegion dataResidencyRegion) { this.dataResidencyRegion = dataResidencyRegion; }
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

