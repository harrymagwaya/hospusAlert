package com.shanalert.hospitalalert.service;


import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.entity.HospitalAdmin;
import com.shanalert.hospitalalert.repository.HospitalAdminRepository;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class HospitalAdminService {

    @Autowired
    private HospitalAdminRepository hospitalAdminRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private UserService userService;


    @Transactional(readOnly = true)
    public Page<HospitalAdmin> findAllPaged(Pageable pageable) {
        log.info("Fetching paginated Hospital Admins: Page {}, Size {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return hospitalAdminRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public HospitalAdmin findById(UUID userId) {
        return hospitalAdminRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital Admin profile not found"));
    }


    @Transactional
    public HospitalAdmin updateAdminProfile(UUID userId, HospitalAdmin updateData, UUID newHospitalId) {
        HospitalAdmin existing = findById(userId);

        // Update identity fields if provided
        if (updateData.getFirstName() != null) existing.setFirstName(updateData.getFirstName());
        if (updateData.getLastName() != null) existing.setLastName(updateData.getLastName());
        if (updateData.getPhoneNumber() != null) existing.setPhoneNumber(updateData.getPhoneNumber());
        if (updateData.getGender() != null) existing.setGender(updateData.getGender());

        // Update the "Top Up" (The Hospital Link)
        if (newHospitalId != null) {
            Hospital newHospital = hospitalRepository.findById(newHospitalId)
                    .orElseThrow(() -> new EntityNotFoundException("New Hospital not found"));
            existing.setHospital(newHospital);
        }

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
}
