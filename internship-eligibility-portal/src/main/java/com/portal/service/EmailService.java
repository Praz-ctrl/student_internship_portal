package com.portal.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendNotificationEmail(String toEmail, String subject, String messageText) {
        if (toEmail == null || toEmail.isBlank()) return;

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            helper.setTo(toEmail);
            helper.setSubject("[Portal] " + subject);
            helper.setFrom("noreply@placementportal.edu");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f8fafc; margin: 0; padding: 20px; color: #1e293b; }
                    .card { background-color: #ffffff; border-radius: 8px; max-width: 580px; margin: 0 auto; padding: 32px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05); }
                    .header { font-size: 20px; font-weight: 700; color: #1e3a8a; margin-bottom: 16px; border-bottom: 2px solid #3b82f6; padding-bottom: 8px; }
                    .content { font-size: 15px; line-height: 1.6; color: #334155; margin-bottom: 24px; }
                    .footer { font-size: 12px; color: #94a3b8; text-align: center; border-top: 1px solid #f1f5f9; padding-top: 16px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <div class="header">%s</div>
                    <div class="content"><p>%s</p></div>
                    <div class="footer">
                      <p>Internship Management & Eligibility Portal &bull; Automated Notification</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(subject, messageText);

            helper.setText(htmlContent, true);
            mailSender.send(mimeMessage);
            log.info("Email notification sent successfully to {}", toEmail);
        } catch (Exception e) {
            log.warn("Could not dispatch email notification to {}: {} (SMTP may be unconfigured in dev/demo mode)", toEmail, e.getMessage());
        }
    }
}
