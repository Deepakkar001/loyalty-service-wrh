package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.config.AppUrlConfig;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AgreementNotificationMailer {

    private final PlatformEmailService emailService;
    private final AppUrlConfig appUrlConfig;

    public AgreementNotificationMailer(PlatformEmailService emailService, AppUrlConfig appUrlConfig) {
        this.emailService = Objects.requireNonNull(emailService, "emailService");
        this.appUrlConfig = Objects.requireNonNull(appUrlConfig, "appUrlConfig");
    }

    public boolean sendApprovalEmail(String tenantEmail, String companyName, String approvalNotes) {
        String dashboardUrl = appUrlConfig.getPortalUrl() + "/login";
        String notesBlock = formatOptionalApprovalNotes(approvalNotes);
        String safeCompany = companyName != null && !companyName.isBlank() ? companyName : "there";

        StringBuilder body = new StringBuilder();
        body.append("Congratulations, ").append(safeCompany).append("!\n\n");
        body.append("Your commercial agreement has been approved by our team. You can now proceed ");
        body.append("with configuring your loyalty programme.\n\n");
        if (!notesBlock.isEmpty()) {
            body.append("Approval notes from our admin:\n");
            body.append(notesBlock).append('\n');
        }
        body.append("Log in to your dashboard to get started:\n");
        body.append(dashboardUrl).append("\n\n");
        body.append("If you have any questions, reply to this email or contact support.\n\n");
        body.append("— The LoyaltyOS Team\n");

        return emailService.sendPlainText(
            tenantEmail,
            "Your LoyaltyOS agreement has been approved",
            body.toString(),
            null
        );
    }

    public boolean sendRejectionEmail(String tenantEmail, String companyName, String rejectionReason) {
        String onboardingUrl = appUrlConfig.getPortalUrl() + "/onboarding";
        String safeCompany = companyName != null && !companyName.isBlank() ? companyName : "there";
        String reason = rejectionReason != null && !rejectionReason.isBlank()
            ? rejectionReason.trim()
            : "No reason provided.";

        String body = """
Hello %s,

Unfortunately, your commercial agreement could not be approved at this time.

Reason:
%s

Please review the feedback above and resubmit your agreement:
%s

If you believe this is an error or need assistance, reply to this email.

— The LoyaltyOS Team
""".formatted(safeCompany, reason, onboardingUrl);

        return emailService.sendPlainText(
            tenantEmail,
            "Action required: your LoyaltyOS agreement needs revision",
            body,
            null
        );
    }

    private static String formatOptionalApprovalNotes(String approvalNotes) {
        if (approvalNotes == null) {
            return "";
        }
        String s = approvalNotes.trim();
        if (s.isEmpty()) {
            return "";
        }
        s = s.replace("\r\n", "\n").replace('\r', '\n');
        int maxChars = 2000;
        if (s.length() > maxChars) {
            s = s.substring(0, maxChars).trim() + "\n…";
        }
        return s + "\n";
    }
}
