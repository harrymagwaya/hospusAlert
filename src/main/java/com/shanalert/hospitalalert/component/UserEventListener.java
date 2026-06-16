package com.shanalert.hospitalalert.component;

import com.shanalert.hospitalalert.event.PasswordResetEvent;
import com.shanalert.hospitalalert.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final EmailService emailService;

    @Async
    @EventListener
    public void handlePasswordReset(PasswordResetEvent event) {
        // This method executes in a separate thread
        emailService.sendResetOtp(event.email(), event.otpCode());
    }
}