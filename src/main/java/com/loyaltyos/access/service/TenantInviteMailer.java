package com.loyaltyos.access.service;

import com.loyaltyos.onboarding.config.AppUrlConfig;
import com.loyaltyos.onboarding.service.PlatformEmailService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TenantInviteMailer {

    private final PlatformEmailService emailService;
    private final AppUrlConfig appUrlConfig;

    public TenantInviteMailer(PlatformEmailService emailService, AppUrlConfig appUrlConfig) {
        this.emailService = Objects.requireNonNull(emailService, "emailService");
        this.appUrlConfig = Objects.requireNonNull(appUrlConfig, "appUrlConfig");
    }

    /**
     * Sends a temporary password and login instructions for a new team member.
     *
     * @return true when SMTP accepted the message; false when email was skipped or only logged
     */
    public boolean sendTempPasswordInviteEmail(String toEmail, String temporaryPassword) {
        String loginUrl = appUrlConfig.getPortalUrl() + "/login";
        String body = """
You have been invited to join your organisation on LoyaltyOS.

Sign in with your email address and this temporary password:

Email: %s
Temporary password: %s

Sign in here: %s

You will be asked to choose a new password immediately after your first sign-in.

If you were not expecting this invitation, you can ignore this email.
""".formatted(toEmail, temporaryPassword, loginUrl);

        return emailService.sendPlainText(
            toEmail,
            "Your LoyaltyOS team invite",
            body,
            "Login URL: " + loginUrl
        );
    }

    /**
     * Legacy accept-link invite (kept for backward compatibility with pending INVITED users).
     *
     * @return true when SMTP accepted the message; false when email was skipped or only logged
     */
    public boolean sendInviteEmail(String toEmail, String inviteToken) {
        String acceptUrl = buildAcceptUrl(toEmail, inviteToken);
        String body = """
You have been invited to join your organisation on LoyaltyOS.

Open the link below to set your password and activate your account:

%s

This link expires in 7 days.

If you were not expecting this invitation, you can ignore this email.
""".formatted(acceptUrl);

        return emailService.sendPlainText(
            toEmail,
            "You have been invited to LoyaltyOS",
            body,
            "Invite accept URL: " + acceptUrl
        );
    }

    public String buildAcceptUrl(String toEmail, String inviteToken) {
        return appUrlConfig.getPortalUrl()
            + "/accept-invite?email="
            + URLEncoder.encode(toEmail, StandardCharsets.UTF_8)
            + "&token="
            + URLEncoder.encode(inviteToken, StandardCharsets.UTF_8);
    }
}
