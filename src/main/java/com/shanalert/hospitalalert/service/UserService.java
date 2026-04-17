package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.UserRequest;
import com.shanalert.hospitalalert.dto.UserResponse;
import com.shanalert.hospitalalert.dto.UserUpdateDTO;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.model.UserStatus;
import com.shanalert.hospitalalert.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
@Slf4j
@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HospitalAdminService hospitalAdminService;

    // Helper method to convert Entity -> DTO (DRY Principle)
    private UserResponse mapToResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getUserStatus(),
                "Success"
        );
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(UserStatus status, Pageable pageable) {
        Page<User> users;
        if (status != null) {
            users = userRepository.findByUserStatus(status, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        // Use .map() to convert each user in the page to a DTO
        return users.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapToResponse(user);
    }

    @Transactional
    public UserResponse registerUser(UserRequest request) {
        log.info("Registering user with role: {}", request.getUserRole());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .role(request.getUserRole())
                .userStatus(UserStatus.ACTIVE)
                .build();

        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UserUpdateDTO updateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (updateDto.getFirstName() != null) user.setFirstName(updateDto.getFirstName());
        if (updateDto.getLastName() != null) user.setLastName(updateDto.getLastName());
        if (updateDto.getPhoneNumber() != null) user.setPhoneNumber(updateDto.getPhoneNumber());
        if (updateDto.getGender() != null) user.setGender(updateDto.getGender());
        if (updateDto.getUserStatus() != null) user.setUserStatus(updateDto.getUserStatus());

        log.info("Updating profile for user: {}", userId);
        return mapToResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        user.setUserStatus(UserStatus.DELETED);
        userRepository.save(user);

        if (user.getRole() == UserRole.ADMIN) {
            hospitalAdminService.deactivateProfile(userId);
        }

        log.info("User {} and associated profiles deactivated.", userId);
    }
}