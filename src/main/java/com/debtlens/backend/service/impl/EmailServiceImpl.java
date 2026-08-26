package com.debtlens.backend.service.impl;

import com.debtlens.backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.invitation.frontend-base-url:http://localhost:5173/invitation/accept}")
    private String frontendBaseUrl;

    @Value("${spring.mail.username:no-reply@debtlens.com}")
    private String fromEmail;

    public EmailServiceImpl(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendInvitationEmail(
            String toEmail,
            String githubUsername,
            String companyName,
            String repositoryName,
            String invitationToken,
            LocalDateTime expiresAt
    ) {
        String invitationUrl = frontendBaseUrl + "?token=" + invitationToken;
        String formattedExpiry = expiresAt != null
                ? expiresAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                : "7 days";

        log.info("Sending invitation email to [{}] for repo [{}] at company [{}] with token [{}]",
                toEmail, repositoryName, companyName, invitationToken);

        String htmlContent = buildHtmlEmailContent(githubUsername, companyName, repositoryName, invitationUrl, formattedExpiry);

        if (mailSender == null) {
            log.warn("JavaMailSender bean is not configured. Invitation Link: {}", invitationUrl);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "no-reply@debtlens.com";
            helper.setFrom(sender, "DebtLens");
            helper.setTo(toEmail);
            helper.setSubject("Invitation to join " + companyName + " on DebtLens (" + repositoryName + ")");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Invitation email successfully delivered to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send invitation email to {} via SMTP. (Invitation Link: {}) Error: {}",
                    toEmail, invitationUrl, e.getMessage());
            // Log fallback link so developer/admin can still copy and test it
        }
    }

    private String buildHtmlEmailContent(
            String githubUsername,
            String companyName,
            String repositoryName,
            String invitationUrl,
            String formattedExpiry
    ) {
        String greetingName = (githubUsername != null && !githubUsername.isBlank()) ? "@" + githubUsername : "Contributor";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>DebtLens Invitation</title>
                </head>
                <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f5f7; margin: 0; padding: 30px 15px;">
                    <table align="center" border="0" cellpadding="0" cellspacing="0" width="100%%" style="max-width: 600px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08); border: 1px solid #e2e8f0;">
                        <!-- Header Banner -->
                        <tr>
                            <td style="background: linear-gradient(135deg, #4361ee 0%%, #7c3aed 100%%); padding: 36px 30px; text-align: center;">
                                <h1 style="color: #ffffff; margin: 0; font-size: 26px; font-weight: 800; letter-spacing: -0.5px;">DebtLens</h1>
                                <p style="color: #e0e7ff; margin: 6px 0 0 0; font-size: 14px;">Technical Debt & Code Intelligence Platform</p>
                            </td>
                        </tr>
                        <!-- Body Content -->
                        <tr>
                            <td style="padding: 36px 30px;">
                                <h2 style="color: #1e293b; font-size: 20px; font-weight: 700; margin-top: 0;">You're Invited!</h2>
                                <p style="color: #475569; font-size: 15px; line-height: 1.6; margin: 12px 0 20px 0;">
                                    Hello <strong style="color: #1e293b;">%s</strong>,
                                </p>
                                <p style="color: #475569; font-size: 15px; line-height: 1.6; margin: 0 0 24px 0;">
                                    You have been invited by the Super Admin of <strong style="color: #4361ee;">%s</strong> to collaborate and view technical debt analytics for the repository:
                                </p>

                                <!-- Repository Box -->
                                <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-left: 4px solid #4361ee; border-radius: 8px; padding: 16px 20px; margin-bottom: 28px;">
                                    <div style="font-size: 11px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px;">Target Repository</div>
                                    <div style="font-size: 16px; font-weight: 700; color: #0f172a; margin-top: 4px;">%s</div>
                                </div>

                                <!-- CTA Button -->
                                <div style="text-align: center; margin: 32px 0;">
                                    <a href="%s" style="background-color: #4361ee; color: #ffffff; text-decoration: none; font-size: 15px; font-weight: 600; padding: 14px 32px; border-radius: 10px; display: inline-block; box-shadow: 0 4px 12px rgba(67, 97, 238, 0.35);">
                                        Accept Invitation & Join
                                    </a>
                                </div>

                                <p style="color: #64748b; font-size: 13px; line-height: 1.5; margin: 24px 0 0 0;">
                                    Or copy and paste this link into your browser:<br>
                                    <a href="%s" style="color: #4361ee; word-break: break-all; font-size: 12px;">%s</a>
                                </p>

                                <div style="margin-top: 24px; padding-top: 16px; border-top: 1px solid #f1f5f9; color: #94a3b8; font-size: 12px;">
                                    ⏰ This invitation link expires on <strong>%s</strong>.
                                </div>
                            </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                            <td style="background-color: #f8fafc; padding: 20px 30px; text-align: center; border-top: 1px solid #e2e8f0;">
                                <p style="color: #94a3b8; font-size: 12px; margin: 0;">
                                    © %d DebtLens. All rights reserved. If you weren't expecting this email, you can safely ignore it.
                                </p>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                greetingName,
                companyName,
                repositoryName,
                invitationUrl,
                invitationUrl,
                invitationUrl,
                formattedExpiry,
                java.time.Year.now().getValue()
        );
    }
}
