package com.loyaltyos.onboarding.dto.request;

import com.loyaltyos.onboarding.domain.enums.DataResidencyRegion;
import com.loyaltyos.onboarding.domain.enums.IdentityMode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateIdentityRequest {

    @NotNull(message = "Identity mode is required")
    private IdentityMode identityMode;

    @NotNull(message = "Data residency region is required")
    private DataResidencyRegion dataResidencyRegion;
}
