package com.shanalert.hospitalalert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:no-reply@hospusapp.com}")
    private String fromEmail;

    @Async
    public void sendResetOtp(String toEmail, String otp) {
        log.info("Preparing password reset email for: {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("HospusAPP: Password Reset Code");
        message.setText(
                "Hello,\n\n" +
                        "You requested a password reset for your HospusAPP account.\n\n" +
                        "Your 6-digit verification code is: " + otp + "\n\n" +
                        "This code will expire in 10 minutes.\n\n" +
                        "If you did not request this, please secure your account immediately.\n\n" +
                        "Stay safe,\n" +
                        "HospusAPP Security Team"
        );

        try {
            mailSender.send(message);
            log.info("Reset OTP successfully sent to: {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send reset OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordChangeAlert(String toEmail) {
        log.info("Preparing password change alert email for: {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("HospusAPP: Security Alert - Password Changed");
        message.setText(
                "Hello,\n\n" +
                        "The password for your HospusAPP account has been successfully updated.\n\n" +
                        "If you did not make this change, please contact our support team immediately.\n\n" +
                        "Stay safe,\n" +
                        "HospusAPP Security Team"
        );

        try {
            mailSender.send(message);
            log.info("Password change alert successfully sent to: {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send password change alert to {}: {}", toEmail, e.getMessage());
        }
    }
}