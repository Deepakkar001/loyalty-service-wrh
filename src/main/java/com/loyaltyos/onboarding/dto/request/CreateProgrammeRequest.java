package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public class CreateProgrammeRequest {
    @NotBlank
    @Size(min = 2, max = 255)
    private String name;

    public CreateProgrammeRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

