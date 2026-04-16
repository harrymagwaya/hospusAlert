package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.UserRequest;
import com.shanalert.hospitalalert.dto.UserUpdateDTO;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.model.UserStatus;
import com.shanalert.hospitalalert.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private HospitalService hospitalService;

    @Autowired
    private HospitalAdminService hospitalAdminService;


    public User getById(UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user;
    }

    private User getByEmail(UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user;
    }

    // SINGLE METHOD for all registrations
    @Transactional
    public User registerUser(UserRequest request, UserRole role) {
        log.info("Registering user with role: {}", role);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .role(role)
                .userStatus(UserStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(UUID userId, UserUpdateDTO updateDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Apply updates only if the values are present in the DTO
        if (updateDto.getFirstName() != null) user.setFirstName(updateDto.getFirstName());
        if (updateDto.getLastName() != null) user.setLastName(updateDto.getLastName());
        if (updateDto.getPhoneNumber() != null) user.setPhoneNumber(updateDto.getPhoneNumber());
        if (updateDto.getGender() != null) user.setGender(updateDto.getGender());
        if (updateDto.getUserStatus() != null) user.setUserStatus(updateDto.getUserStatus());

        log.info("Updating profile for user: {}", userId);
        return userRepository.save(user);
    }


}
