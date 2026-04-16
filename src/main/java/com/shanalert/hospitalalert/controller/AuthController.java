package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.AuthResponse;
import com.shanalert.hospitalalert.dto.LoginRequest;
import com.shanalert.hospitalalert.dto.SecurityResetRequest;
import com.shanalert.hospitalalert.model.HospusAPP;
import com.shanalert.hospitalalert.service.AuthService;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request, @RequestParam HospusAPP app) {
        return authService.login(request, app);
    }

    @PostMapping("/forgot-password")
    public void initiateReset(@RequestParam String email) {
        authService.initiateReset(email);
    }

    @PostMapping("/verify-otp")
    public boolean verifyOtp(@RequestParam String email, @RequestParam String otp) {
        return authService.verifyOtp(email, otp);
    }

    @PostMapping("/reset-password")
    public void completeReset(@RequestBody SecurityResetRequest request) {
        authService.completeReset(request);
    }
}