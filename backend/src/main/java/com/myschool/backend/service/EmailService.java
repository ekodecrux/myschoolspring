package com.myschool.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${email.from:}")
    private String emailFrom;

    @Async
    public void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailFrom);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            logger.info("Email sent successfully to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name, String password, String role, String schoolName) {
        String subject = "Welcome to " + schoolName + " - Your Account Details";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #1976d2; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                    .credentials { background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0; border: 1px solid #ddd; }
                    .password { font-size: 18px; font-weight: bold; color: #1976d2; background: #e3f2fd; padding: 10px; border-radius: 4px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to %s</h1>
                    </div>
                    <div class="content">
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Your account has been created successfully as a <strong>%s</strong>.</p>
                        <div class="credentials">
                            <h3>Your Login Credentials:</h3>
                            <p><strong>Email:</strong> %s</p>
                            <p><strong>Temporary Password:</strong></p>
                            <p class="password">%s</p>
                        </div>
                        <p>&#9888; <strong>Important:</strong> Please change your password after your first login for security purposes.</p>
                        <p>If you have any questions, please contact your administrator.</p>
                        <div class="footer">
                            <p>This is an automated message from MySchool. Please do not reply to this email.</p>
                            <p>&copy; 2024 MySchool - Solutions Beyond School</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(schoolName, name, role, toEmail, password);

        sendEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendSelfRegistrationWelcomeEmail(String toEmail, String name, String role, String schoolName) {
        String subject = "Welcome to " + schoolName + "! Registration Successful";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4CAF50; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                    .info { background: #fff; padding: 20px; border-radius: 8px; margin: 20px 0; border: 1px solid #ddd; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Welcome to %s!</h1>
                    </div>
                    <div class="content">
                        <p>Dear <strong>%s</strong>,</p>
                        <p>Thank you for registering with us! Your account has been created successfully as a <strong>%s</strong>.</p>
                        <div class="info">
                            <h3>Your Account Details:</h3>
                            <p><strong>Email:</strong> %s</p>
                            <p><strong>Role:</strong> %s</p>
                        </div>
                        <p>You can now log in to access all the educational resources available on our platform.</p>
                        <p><a href="https://portal.myschoolct.com">Login Now</a></p>
                        <div class="footer">
                            <p>Thank you for choosing MySchool!</p>
                            <p>&copy; 2024 MySchool - Solutions Beyond School</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(schoolName, name, role, toEmail, role);

        sendEmail(toEmail, subject, htmlContent);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String name, String resetCode) {
        String subject = "MySchool - Password Reset Request";
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #f44336; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }
                    .code { font-size: 32px; font-weight: bold; color: #f44336; background: #ffebee; padding: 15px; border-radius: 8px; text-align: center; letter-spacing: 8px; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset</h1>
                    </div>
                    <div class="content">
                        <p>Dear <strong>%s</strong>,</p>
                        <p>We received a request to reset your password. Use the code below to reset your password:</p>
                        <p class="code">%s</p>
                        <p>This code will expire in <strong>15 minutes</strong>.</p>
                        <p>If you didn't request this password reset, please ignore this email or contact support if you have concerns.</p>
                        <div class="footer">
                            <p>&copy; 2024 MySchool - Solutions Beyond School</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(name, resetCode);

        sendEmail(toEmail, subject, htmlContent);
    }
}
