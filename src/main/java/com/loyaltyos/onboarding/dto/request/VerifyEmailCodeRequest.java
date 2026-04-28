package com.loyaltyos.onboarding.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailCodeRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Verification code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Verification code must be 6 digits")
    private String code;

    /**
     * Optional: allow setting a new password at the time of OTP verification.
     * This supports recovery if the user mistyped password during initial signup.
     */
    @Size(min = 12, max = 100, message = "Password must be at least 12 characters")
    private String newPassword;
}

