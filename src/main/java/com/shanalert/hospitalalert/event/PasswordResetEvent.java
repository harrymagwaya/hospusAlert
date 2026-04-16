package com.shanalert.hospitalalert.event;

public record PasswordResetEvent(String email, String otpCode) {}