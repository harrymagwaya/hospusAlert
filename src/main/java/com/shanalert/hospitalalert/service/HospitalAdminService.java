package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.HospitalAdminUpdateDto;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.entity.HospitalAdmin;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.repository.HospitalAdminRepository;
import com.shanalert.hospitalalert.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
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

    private final HospitalAdminRepository hospitalAdminRepository;
    private final HospitalRepository hospitalRepository;
    private final UserService userService;

    public HospitalAdminService(
            HospitalAdminRepository hospitalAdminRepository,
            HospitalRepository hospitalRepository,
            @Lazy UserService userService
    ) {
        this.hospitalAdminRepository = hospitalAdminRepository;
        this.hospitalRepository = hospitalRepository;
        this.userService = userService;
    }

    @Transactional
    public Page<HospitalAdmin> findAllPaged(Pageable pageable) {
        log.info(
                "Fetching paginated Hospital Admin users: Page {}, Size {}",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return userService.findUsersByRole(UserRole.HOSPITAL_ADMIN, pageable)
                .map(user -> getOrCreateProfile(user.getId()));
    }

    @Transactional
    public HospitalAdmin getOrCreateProfile(UUID userId) {
        User masterUser = userService.getById(userId);

        if (masterUser.getRole() != UserRole.HOSPITAL_ADMIN) {
            throw new IllegalStateException("User is not a hospital admin");
        }

        HospitalAdmin hospitalAdmin = hospitalAdminRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing HospitalAdmin profile for user: {}", userId);

                    HospitalAdmin newAdmin = new HospitalAdmin();
                    newAdmin.setId(userId);

                    return newAdmin;
                });

        syncUserFields(hospitalAdmin, masterUser);

        return hospitalAdminRepository.save(hospitalAdmin);
    }

    @Transactional
    public HospitalAdmin updateAdminProfile(UUID userId, HospitalAdminUpdateDto dto, UUID actorId) {
        HospitalAdmin existing = getOrCreateProfile(userId);

        if (dto.firstName() != null) existing.setFirstName(dto.firstName());
        if (dto.lastName() != null) existing.setLastName(dto.lastName());
        if (dto.phoneNumber() != null) existing.setPhoneNumber(dto.phoneNumber());
        if (dto.gender() != null) existing.setGender(dto.gender());

        if (dto.hospitalId() != null) {
            Hospital newHospital = hospitalRepository.findById(dto.hospitalId())
                    .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

            existing.setHospital(newHospital);
        }

        existing.setUpdatedBy(actorId);
        existing.setUpdatedAt(LocalDateTime.now());

        return hospitalAdminRepository.save(existing);
    }

    @Transactional
    public void linkUserToHospital(UUID userId, UUID hospitalId) {
        User user = userService.getById(userId);

        if (user.getRole() != UserRole.HOSPITAL_ADMIN) {
            throw new IllegalStateException("Only users with HOSPITAL_ADMIN role can be linked as hospital admins");
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        HospitalAdmin adminProfile = hospitalAdminRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Creating HospitalAdmin profile for user: {}", userId);

                    HospitalAdmin newAdmin = new HospitalAdmin();
                    newAdmin.setId(user.getId());

                    return newAdmin;
                });

        syncUserFields(adminProfile, user);
        adminProfile.setHospital(hospital);

        hospitalAdminRepository.save(adminProfile);

        log.info("Linked User {} to Hospital {}", user.getEmail(), hospital.getName());
    }

    @Transactional
    public void removeAdminProfile(UUID userId) {
        if (!hospitalAdminRepository.existsById(userId)) {
            throw new EntityNotFoundException("Cannot remove: Hospital Admin profile does not exist");
        }

        hospitalAdminRepository.deleteById(userId);

        log.info("Removed HospitalAdmin profile for user {}", userId);
    }

    @Transactional(readOnly = true)
    public List<HospitalAdmin> findByHospitalId(UUID hospitalId) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new EntityNotFoundException("Hospital not found");
        }

        return hospitalAdminRepository.findByHospitalId(hospitalId);
    }

    @Transactional
    public void deactivateProfile(UUID userId) {
        HospitalAdmin admin = hospitalAdminRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital Admin profile not found"));

        log.info("Unlinking HospitalAdmin profile and clearing hospital access for user: {}", userId);

        admin.setHospital(null);
        admin.setUpdatedAt(LocalDateTime.now());

        hospitalAdminRepository.save(admin);
    }

    @Transactional(readOnly = true)
    public boolean belongsToHospital(UUID adminId, UUID hospitalId) {
        if (adminId == null) {
            throw new IllegalStateException("Hospital Admin ID is required");
        }

        if (hospitalId == null) {
            throw new IllegalStateException("Hospital ID is required");
        }

        return hospitalAdminRepository.existsByIdAndHospital_Id(adminId, hospitalId);
    }

    @Transactional(readOnly = true)
    public void validateHospitalAdminAccess(UUID adminId, UUID hospitalId) {
        boolean belongsToHospital = belongsToHospital(adminId, hospitalId);

        if (!belongsToHospital) {
            throw new IllegalStateException("Hospital admin does not belong to this hospital");
        }
    }

    private void syncUserFields(HospitalAdmin hospitalAdmin, User user) {
        hospitalAdmin.setUsername(user.getUsername());
        hospitalAdmin.setEmail(user.getEmail());
        hospitalAdmin.setFirstName(user.getFirstName());
        hospitalAdmin.setLastName(user.getLastName());
        hospitalAdmin.setPhoneNumber(user.getPhoneNumber());
        hospitalAdmin.setGender(user.getGender());
        hospitalAdmin.setRole(user.getRole());
    }
}