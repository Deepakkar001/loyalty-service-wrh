package com.loyaltyos.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateProgrammeRequest {

    @NotBlank
    @Size(min = 2, max = 255)
    private String name;

    public UpdateProgrammeRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
