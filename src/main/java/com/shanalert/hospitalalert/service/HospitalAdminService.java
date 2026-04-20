package com.shanalert.hospitalalert.service;


import com.shanalert.hospitalalert.dto.HospitalAdminUpdateDto;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.entity.HospitalAdmin;
import com.shanalert.hospitalalert.entity.Patient;
import com.shanalert.hospitalalert.repository.HospitalAdminRepository;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class HospitalAdminService {

    @Autowired
    private HospitalAdminRepository hospitalAdminRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Lazy
    @Autowired
    private UserService userService;


    @Transactional(readOnly = true)
    public Page<HospitalAdmin> findAllPaged(Pageable pageable) {
        log.info("Fetching paginated Hospital Admins: Page {}, Size {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return hospitalAdminRepository.findAll(pageable);
    }

    @Transactional
    public HospitalAdmin findById(UUID userId) {
        // Fetch Master Identity
        User masterUser = userService.getById(userId);

        // Fetch existing or initialize a fresh profile
        HospitalAdmin hospitalAdmin = hospitalAdminRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing Patient profile for user: {}", userId);
                    HospitalAdmin newAdmin = new HospitalAdmin();
                    newAdmin.setId(userId);
                    return newAdmin;
                });

        // Always sync core fields to ensure they mirror the User table
        hospitalAdmin.setUsername(masterUser.getUsername());
        hospitalAdmin.setEmail(masterUser.getEmail());
        hospitalAdmin.setFirstName(masterUser.getFirstName());
        hospitalAdmin.setLastName(masterUser.getLastName());
        hospitalAdmin.setPhoneNumber(masterUser.getPhoneNumber());
        hospitalAdmin.setGender(masterUser.getGender());
        hospitalAdmin.setRole(masterUser.getRole());

        // Save and return
        return hospitalAdminRepository.save(hospitalAdmin);
    }


    @Transactional
    public HospitalAdmin updateAdminProfile(UUID userId, HospitalAdminUpdateDto dto, UUID actorId) {
        // findById already contains your JIT logic if you want it
        HospitalAdmin existing = findById(userId);

        // Partial updates from DTO
        if (dto.firstName() != null) existing.setFirstName(dto.firstName());
        if (dto.lastName() != null) existing.setLastName(dto.lastName());
        if (dto.phoneNumber() != null) existing.setPhoneNumber(dto.phoneNumber());
        if (dto.gender() != null) existing.setGender(dto.gender());

        // Update Hospital Link if provided
        if (dto.hospitalId() != null) {
            Hospital newHospital = hospitalRepository.findById(dto.hospitalId())
                    .orElseThrow(() -> new EntityNotFoundException("New Hospital not found"));
            existing.setHospital(newHospital);
        }

        existing.setUpdatedBy(actorId);
        existing.setUpdatedAt(LocalDateTime.now());

        return hospitalAdminRepository.save(existing);
    }

    @Transactional
    public void linkUserToHospital(UUID userId, UUID hospitalId) {
        // 1. Verify existence of the primary entities
        User user = userService.getById(userId);

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        // 2. Check if this User is already an Admin elsewhere
        if (hospitalAdminRepository.existsById(userId)) {
            throw new IllegalStateException("User is already assigned as a Hospital Admin");
        }

        // 3. Create the Extension
        HospitalAdmin adminProfile = new HospitalAdmin();
        adminProfile.setId(user.getId()); // ID match

        // 4. Mirror all User fields (The Extension)
        adminProfile.setUsername(user.getUsername());
        adminProfile.setEmail(user.getEmail());
        adminProfile.setFirstName(user.getFirstName());
        adminProfile.setLastName(user.getLastName());
        adminProfile.setPhoneNumber(user.getPhoneNumber());
        adminProfile.setGender(user.getGender());
        adminProfile.setRole(user.getRole());

        // 5. The Top Up
        adminProfile.setHospital(hospital);

        hospitalAdminRepository.save(adminProfile);
        log.info("Linked User {} to Hospital {}", user.getEmail(), hospital.getName());
    }

    @Transactional
    public void removeAdminProfile(UUID userId) {
        if (!hospitalAdminRepository.existsById(userId)) {
            throw new EntityNotFoundException("Cannot remove: Profile does not exist");
        }
        hospitalAdminRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public List<HospitalAdmin> findByHospitalId(UUID hospitalId){
        return hospitalAdminRepository.findByHospitalId(hospitalId);
    }

    // Inside HospitalAdminService.java

    @Transactional
    public void deactivateProfile(UUID userId) {
        hospitalAdminRepository.findById(userId).ifPresent(admin -> {
            log.info("Unlinking HospitalAdmin profile and clearing facility access for user: {}", userId);

            // 1. Remove the link to the hospital
            admin.setHospital(null);

            hospitalAdminRepository.save(admin);
        });
    }
}
