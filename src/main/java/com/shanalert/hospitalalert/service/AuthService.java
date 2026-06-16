package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.config.UserPrincipal;
import com.shanalert.hospitalalert.dto.AuthResponse;
import com.shanalert.hospitalalert.dto.LoginRequest;
import com.shanalert.hospitalalert.dto.PasswordChangeRequest;
import com.shanalert.hospitalalert.dto.SecurityResetRequest;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.event.PasswordResetEvent;
import com.shanalert.hospitalalert.model.HospusAPP;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.repository.UserRepository;
import com.shanalert.hospitalalert.security.CustomUserDetailsService;
import com.shanalert.hospitalalert.security.OtpService;
import com.shanalert.hospitalalert.security.TokenProvider;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserService userService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private  ApplicationEventPublisher eventPublisher;

    @Autowired
    private OtpService otpService;



    public AuthResponse login(LoginRequest request, HospusAPP app) {
        // 1. Fetch the Principal (Your service returns UserPrincipal, not the Entity)
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(request.getEmail());

        // 2. Verify Password
        if (!passwordEncoder.matches(request.getPassword(), principal.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // 3. Check App Access (Using the getter from your UserPrincipal)
        validateAppAccess(principal.getUserRole(), app);

        // 4. Create Authentication for the TokenProvider
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );

        // 5. Generate Token
        String token = tokenProvider.generateToken(auth);

        // 6. Return Lean Response
        return new AuthResponse(
                token,
                principal.getId(), // Get ID from principal
                tokenProvider.getExpiration(token),
                principal.getUserRole() // Get Role from principal
        );
    }

    public void logout(HttpServletRequest request) {
        // 1. Clear the Spring Security Context
        SecurityContextHolder.clearContext();

        // 2. Extract the token (Optional: if you are implementing a blacklist)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("Token invalidated: {}", token);

            // Logic: Add 'token' to a Redis Blacklist with its remaining TTL
            // blacklistService.blacklistToken(token);
        }

        log.info("User successfully logged out and context cleared.");
    }



//
//    private UUID fetchUserProfileAndGetContext(User authUser) {
//        return switch (authUser.getRole()) {
//            case DOCTOR,  -> doctorRepository.findById(authUser.getId())
//                    .map(Doctor::getHospitalId)
//                    .orElseThrow(() -> new EntityNotFoundException("Doctor profile missing"));
//
//            case ADMIN -> hospitalAdminRepository.findById(authUser.getId())
//                    .map(h -> h.getHospital().getId())
//                    .orElseThrow(() -> new EntityNotFoundException("Hospital Admin profile missing"));
//
//            case PATIENT -> {
//                // Ensure Patient exists, even if they don't have a hospital context yet
//                if (!patientRepository.existsById(authUser.getId())) {
//                    throw new EntityNotFoundException("Patient profile missing");
//                }
//                yield null;
//            }
//
//            default -> null; // For roles like System Admin that don't need context
//        };
//    }


    private void validateAppAccess(UserRole role, HospusAPP appSource) {
        boolean isAuthorized = switch (appSource) {
            case PATIENT_APP -> role == UserRole.PATIENT;
            case HOSPITAL_APP -> (role == UserRole.DOCTOR || role == UserRole.NURSE || role == UserRole.ADMIN);
            case ADMIN_APP -> role == UserRole.ADMIN;
        };

        if (!isAuthorized) {
            log.warn("App Access Denied: {} tried to access {}", role, appSource.getValue());
            throw new AccessDeniedException("Access denied for " + appSource.getValue());
        }
    }




//    // --- LOGGED IN: CHANGE PASSWORD ---
//    @Transactional
//    public void changePassword(UUID userId, PasswordChangeRequest request) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
//            throw new IllegalArgumentException("Incorrect current password.");
//        }
//        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
//            throw new IllegalArgumentException("New passwords do not match.");
//        }
//
//        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//        userRepository.save(user);
//        log.info("Password changed successfully for user: {}", userId);
//    }

//    // --- FORGOT PASSWORD: STEP 1 (INITIATE) ---
//    @Transactional
//    public void initiateReset(String email) {
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        String otp = String.format("%06d", new Random().nextInt(999999));
//
//        user.setResetToken(passwordEncoder.encode(otp));
//        user.setTokenExpiry(LocalDateTime.now().plusMinutes(10));
//        userRepository.save(user);
//
//        eventPublisher.publishEvent(new PasswordResetEvent(email, otp));
//        log.info("Password reset OTP generated for: {}", email);
//    }
//
//    // --- FORGOT PASSWORD: STEP 2 (COMPLETE) ---
//    @Transactional
//    public void completeReset(SecurityResetRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        if (user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(LocalDateTime.now())) {
//            throw new IllegalArgumentException("OTP has expired.");
//        }
//        if (!passwordEncoder.matches(request.getOtpCode(), user.getResetToken())) {
//            throw new IllegalArgumentException("Invalid OTP code.");
//        }
//        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
//            throw new IllegalArgumentException("Passwords do not match.");
//        }
//
//        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
//        user.setResetToken(null);
//        user.setTokenExpiry(null);
//        userRepository.save(user);
//
//        log.info("Password reset successfully via OTP for: {}", user.getEmail());
//    }




    // --- FORGOT PASSWORD STEP 1: INITIATE (Stateless) ---

    @Transactional(readOnly = true)
    public void initiateReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Generate OTP based on current password hash (Stateless)
        String userSecret = user.getPassword() + user.getEmail();
        String otp = otpService.generateStatelessOtp(userSecret);

        // Publish event for asynchronous email delivery
        eventPublisher.publishEvent(new PasswordResetEvent(email, otp));
        log.info("Stateless OTP generated and event published for: {}", email);
    }

    // --- FORGOT PASSWORD STEP 2: VERIFY (Returns Boolean) ---

    @Transactional(readOnly = true)
    public boolean verifyOtp(String email, String otpCode) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    String userSecret = user.getPassword() + user.getEmail();
                    return otpService.isOtpValid(userSecret, otpCode);
                })
                .orElse(false);
    }

    // --- FORGOT PASSWORD STEP 3: COMPLETE ---

    @Transactional
    public void completeReset(SecurityResetRequest request) {
        // Double-check OTP validity before writing to DB
        if (!verifyOtp(request.getEmail(), request.getOtpCode())) {
            throw new IllegalArgumentException("Invalid or expired OTP code.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User no longer exists"));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user); // Password change kills the OTP mathematically
        log.info("Password successfully reset via OTP for: {}", request.getEmail());
    }

    // --- LOGGED IN: CHANGE PASSWORD ---

    @Transactional
    public void changePassword(UUID userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password updated for authenticated user: {}", userId);
    }
}
