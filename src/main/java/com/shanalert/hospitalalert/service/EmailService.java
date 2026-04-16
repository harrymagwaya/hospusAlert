package com.shanalert.hospitalalert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Sends a plain-text OTP code for password resets.
     * @param toEmail Recipient address
     * @param otp The 6-digit code
     */
    @Async
    public void sendResetOtp(String toEmail, String otp) {
        log.info("Preparing password reset email for: {}", toEmail);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("support@hospusapp.com"); // Ensure this matches your SMTP config
        message.setTo(toEmail);
        message.setSubject("HospusAPP: Password Reset Code");
        message.setText(String.format(
                "Hello,\n\n" +
                        "You requested a password reset for your HospusAPP account.\n" +
                        "Your 6-digit verification code is: %s\n\n" +
                        "This code will expire in 10 minutes. If you did not request this, " +
                        "please secure your account immediately.\n\n" +
                        "Stay safe,\n" +
                        "HospusAPP Security Team", otp));

        try {
            mailSender.send(message);
            log.info("Reset OTP successfully sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    /**
     * Sends a security alert after a successful password change.
     */
    @Async
    public void sendPasswordChangeAlert(String toEmail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("security@hospusapp.com");
        message.setTo(toEmail);
        message.setSubject("HospusAPP: Security Alert - Password Changed");
        message.setText("The password for your account has been successfully updated.\n\n" +
                "If you did not make this change, please contact our support " +
                "team immediately to lock your account.");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send security alert to {}: {}", toEmail, e.getMessage());
        }
    }
}