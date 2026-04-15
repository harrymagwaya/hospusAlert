package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.UserRequest;
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


    private User getById(UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user;
    }

    private User getByEmail(UUID userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return user;
    }

    @Transactional
    public void createAdmin(UserRequest request) {
        // We create the user. The 'hospitalId' relationship is managed
        // outside of the User entity (e.g., in a logs or future profile table)
        saveUser(request, UserRole.ADMIN);
        log.info("Admin created.");
    }

    @Transactional
    public void createHospitalAdmin(UserRequest request, UUID hospitalId) {
        // We create the user. The 'hospitalId' relationship is managed
        // outside of the User entity (e.g., in a logs or future profile table)
        saveUser(request, UserRole.HOSPITAL_ADMIN);
        log.info("Admin created. Access for Hospital {} must be mapped in the Access table.", hospitalId);
    }

    // 2. HOSPITAL ADMIN: Provisions a Doctor
    @Transactional
    public void addDoctor(UserRequest request, UUID hospitalId) {
        saveUser(request, UserRole.DOCTOR);
        log.info("Doctor created for facility {}. Relationship held in staff mapping.", hospitalId);
    }

    // 3. PUBLIC: Patient Registration
    @Transactional
    public void registerPatient(UserRequest request) {
        saveUser(request, UserRole.PATIENT);
    }

    // INTERNAL HELPER: Purely saves Auth data
    private void saveUser(UserRequest request, UserRole role) {
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .userStatus(UserStatus.ACTIVE)
                .build();

        userRepository.save(user);
    }


}
