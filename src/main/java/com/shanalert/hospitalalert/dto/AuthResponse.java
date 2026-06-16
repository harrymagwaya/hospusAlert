package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;


@Builder
public class AuthResponse {

    @Getter
    private final String token;
    @Getter
    private final UUID userId;
    @Getter
    private final long expiresIn;
    @Getter
    private final UserRole role;


    public AuthResponse(String token, UUID userId,long exp , UserRole role) {
        this.token = token;
        this.userId = userId;
        this.expiresIn = exp;
        this.role = role;
    }
}

