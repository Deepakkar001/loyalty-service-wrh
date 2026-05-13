package com.loyaltyos.onboarding.dto.request;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import jakarta.validation.constraints.NotNull;

public class UpdateIdentityRequest {

    @NotNull(message = "Identity mode is required")
    private IdentityMode identityMode;

    @NotNull(message = "Data residency region is required")
    private DataResidencyRegion dataResidencyRegion;

    public UpdateIdentityRequest() {}

    public IdentityMode getIdentityMode() {
        return identityMode;
    }

    public void setIdentityMode(IdentityMode identityMode) {
        this.identityMode = identityMode;
    }

    public DataResidencyRegion getDataResidencyRegion() {
        return dataResidencyRegion;
    }

    public void setDataResidencyRegion(DataResidencyRegion dataResidencyRegion) {
        this.dataResidencyRegion = dataResidencyRegion;
    }
}
