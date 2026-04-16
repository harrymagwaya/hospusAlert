package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.AuthResponse;
import com.shanalert.hospitalalert.dto.LoginRequest;
import com.shanalert.hospitalalert.model.HospusAPP;
import com.shanalert.hospitalalert.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login endpoint.
     * Expects a header 'X-App-Source' (e.g., ADMIN_APP, PATIENT_APP, HOSPITAL_APP)
     */
    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest loginRequest,
            @RequestHeader("X-App-Source") String appSource) {

        // Convert the header string to your HospusAPP enum
        HospusAPP app = HospusAPP.valueOf(appSource.toUpperCase());

        return authService.login(loginRequest, app);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        authService.logout(request);
    }
}