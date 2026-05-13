package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.config.AppUrlConfig;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AgreementNotificationMailer {

    private static final Logger log = LoggerFactory.getLogger(AgreementNotificationMailer.class);

    private final JavaMailSender mailSender;
    private final AppUrlConfig appUrlConfig;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public AgreementNotificationMailer(JavaMailSender mailSender, AppUrlConfig appUrlConfig) {
        this.mailSender = Objects.requireNonNull(mailSender, "mailSender");
        this.appUrlConfig = Objects.requireNonNull(appUrlConfig, "appUrlConfig");
    }

    public void sendApprovalEmail(String tenantEmail, String companyName, String approvalNotes) {
        String dashboardUrl = appUrlConfig.getBaseUrl() + "/dashboard";

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("spring.mail.username not set; skipping approval email to {}", tenantEmail);
            return;
        }

        String notesBlock = formatOptionalApprovalNotes(approvalNotes);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(tenantEmail);
        message.setFrom(fromAddress);
        message.setSubject("Your LoyaltyOS agreement has been approved");
        message.setText("""
Congratulations, %s!

Your commercial agreement has been approved by our team. You can now proceed \
with configuring your loyalty programme.

%s
%s
Log in to your dashboard to get started:
%s

If you have any questions, reply to this email or contact support.

— The LoyaltyOS Team
""".formatted(
            companyName,
            notesBlock.isEmpty() ? "" : "Approval notes from our admin:",
            notesBlock,
            dashboardUrl
        ));

        try {
            mailSender.send(message);
            log.info("Approval notification sent to {}", tenantEmail);
        } catch (MailException ex) {
            log.error("Failed to send approval email to {}", tenantEmail, ex);
        }
    }

    public void sendRejectionEmail(String tenantEmail, String companyName, String rejectionReason) {
        String onboardingUrl = appUrlConfig.getBaseUrl() + "/onboarding";

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("spring.mail.username not set; skipping rejection email to {}", tenantEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(tenantEmail);
        message.setFrom(fromAddress);
        message.setSubject("Action required: your LoyaltyOS agreement needs revision");
        message.setText("""
Hello %s,

Unfortunately, your commercial agreement could not be approved at this time.

Reason:
%s

Please review the feedback above and resubmit your agreement:
%s

If you believe this is an error or need assistance, reply to this email.

— The LoyaltyOS Team
""".formatted(companyName, rejectionReason, onboardingUrl));

        try {
            mailSender.send(message);
            log.info("Rejection notification sent to {}", tenantEmail);
        } catch (MailException ex) {
            log.error("Failed to send rejection email to {}", tenantEmail, ex);
        }
    }

    private static String formatOptionalApprovalNotes(String approvalNotes) {
        if (approvalNotes == null) {
            return "";
        }
        String s = approvalNotes.trim();
        if (s.isEmpty()) {
            return "";
        }
        // Body-only: normalize line endings and bound size to keep emails readable.
        s = s.replace("\r\n", "\n").replace('\r', '\n');
        int maxChars = 2000;
        if (s.length() > maxChars) {
            s = s.substring(0, maxChars).trim() + "\n…";
        }
        return s + "\n";
    }
}
