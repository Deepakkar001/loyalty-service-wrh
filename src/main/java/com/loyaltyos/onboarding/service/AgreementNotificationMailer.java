package com.loyaltyos.onboarding.service;

import com.loyaltyos.onboarding.config.AppUrlConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementNotificationMailer {

    private final JavaMailSender mailSender;
    private final AppUrlConfig appUrlConfig;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendApprovalEmail(String tenantEmail, String companyName) {
        String dashboardUrl = appUrlConfig.getBaseUrl() + "/dashboard";

        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("spring.mail.username not set; skipping approval email to {}", tenantEmail);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(tenantEmail);
        message.setFrom(fromAddress);
        message.setSubject("Your LoyaltyOS agreement has been approved");
        message.setText("""
Congratulations, %s!

Your commercial agreement has been approved by our team. You can now proceed \
with configuring your loyalty programme.

Log in to your dashboard to get started:
%s

If you have any questions, reply to this email or contact support.

— The LoyaltyOS Team
""".formatted(companyName, dashboardUrl));

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
}
