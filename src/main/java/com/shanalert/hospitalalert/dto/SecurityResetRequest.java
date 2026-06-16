package com.shanalert.hospitalalert.dto;

import lombok.Data;

@Data
public class SecurityResetRequest {
    private String email;
    private String otpCode;
    private String newPassword;
    private String confirmPassword;
}