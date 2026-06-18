package com.loyaltyos.onboarding.dto;

import com.loyaltyos.merchants.dto.MerchantAuthResponse;
import java.util.List;

public class UnifiedSignInResponse {

    private SignInIdentityType identityType;
    private LoginResponse tenant;
    private MerchantAuthResponse merchant;
    private List<SignInOrganisationOption> organisations;

    public static UnifiedSignInResponse tenant(LoginResponse tenant) {
        UnifiedSignInResponse response = new UnifiedSignInResponse();
        response.identityType = SignInIdentityType.TENANT;
        response.tenant = tenant;
        return response;
    }

    public static UnifiedSignInResponse merchant(MerchantAuthResponse merchant) {
        UnifiedSignInResponse response = new UnifiedSignInResponse();
        response.identityType = SignInIdentityType.MERCHANT;
        response.merchant = merchant;
        return response;
    }

    public static UnifiedSignInResponse organisationSelection(List<SignInOrganisationOption> organisations) {
        UnifiedSignInResponse response = new UnifiedSignInResponse();
        response.identityType = SignInIdentityType.ORGANISATION_SELECTION_REQUIRED;
        response.organisations = organisations;
        return response;
    }

    public SignInIdentityType getIdentityType() { return identityType; }
    public void setIdentityType(SignInIdentityType identityType) { this.identityType = identityType; }

    public LoginResponse getTenant() { return tenant; }
    public void setTenant(LoginResponse tenant) { this.tenant = tenant; }

    public MerchantAuthResponse getMerchant() { return merchant; }
    public void setMerchant(MerchantAuthResponse merchant) { this.merchant = merchant; }

    public List<SignInOrganisationOption> getOrganisations() { return organisations; }
    public void setOrganisations(List<SignInOrganisationOption> organisations) {
        this.organisations = organisations;
    }
}
