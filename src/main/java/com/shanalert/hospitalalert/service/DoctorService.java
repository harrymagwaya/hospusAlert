package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.DoctorUpdateDto;
import com.shanalert.hospitalalert.entity.Doctor;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.repository.DoctorRepository;
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
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final UserService userService;

    public DoctorService(
            DoctorRepository doctorRepository,
            HospitalRepository hospitalRepository,
            @Lazy UserService userService
    ) {
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.userService = userService;
    }

    /**
     * Fetches all users with DOCTOR role, then lazy-creates/syncs their Doctor profiles.
     * This solves the issue where doctorRepository.findAll() only returns already-created doctor profiles.
     */
    @Transactional
    public Page<Doctor> findAllPaged(Pageable pageable) {
        log.info(
                "Fetching paginated Doctor users: Page {}, Size {}",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return userService.findUsersByRole(UserRole.DOCTOR, pageable)
                .map(user -> findById(user.getId()));
    }

    /**
     * Gets a doctor profile by user ID.
     * If the Doctor profile does not exist yet, it creates it from the User record.
     */
    @Transactional
    public Doctor findById(UUID userId) {
        User masterUser = userService.getById(userId);

        if (masterUser.getRole() != UserRole.DOCTOR) {
            throw new IllegalStateException("User is not a doctor");
        }

        Doctor doctor = doctorRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing Doctor profile for user: {}", userId);

                    Doctor newDoctor = new Doctor();
                    newDoctor.setId(userId);

                    return newDoctor;
                });

        syncUserFieldsToDoctor(masterUser, doctor);

        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor updateDoctorProfile(UUID userId, DoctorUpdateDto dto, UUID actorId) {
        Doctor existing = findById(userId);

        if (dto.firstName() != null) existing.setFirstName(dto.firstName());
        if (dto.lastName() != null) existing.setLastName(dto.lastName());
        if (dto.phoneNumber() != null) existing.setPhoneNumber(dto.phoneNumber());
        if (dto.gender() != null) existing.setGender(dto.gender());

        if (dto.medicalLicenseNumber() != null) {
            existing.setMedicalLicenseNumber(dto.medicalLicenseNumber());
        }

        if (dto.specialization() != null) {
            existing.setSpecialization(dto.specialization());
        }

        if (dto.qualifications() != null) {
            existing.setQualifications(dto.qualifications());
        }

        if (dto.yearsOfExperience() != null) {
            existing.setYearsOfExperience(dto.yearsOfExperience());
        }

        if (dto.department() != null) {
            existing.setDepartment(dto.department());
        }

        if (dto.isAvailable() != null) {
            existing.setIsAvailable(dto.isAvailable());
        }

        if (dto.newHospitalId() != null) {
            Hospital hospital = hospitalRepository.findById(dto.newHospitalId())
                    .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

            if (existing.getHospitals() == null) {
                throw new IllegalStateException("Doctor hospitals list is not initialized");
            }

            if (!existing.getHospitals().contains(hospital)) {
                existing.getHospitals().add(hospital);
            }
        }

        existing.setUpdatedBy(actorId);
        existing.setUpdatedAt(LocalDateTime.now());

        return doctorRepository.save(existing);
    }

    /**
     * Links a doctor user to a hospital.
     * If doctor profile does not exist yet, it creates it.
     */
    @Transactional
    public void linkUserToHospital(UUID userId, UUID hospitalId) {
        User user = userService.getById(userId);

        if (user.getRole() != UserRole.DOCTOR) {
            throw new IllegalStateException("Only users with DOCTOR role can be linked as doctors");
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        Doctor doctorProfile = doctorRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Creating Doctor profile for user: {}", userId);

                    Doctor newDoctor = new Doctor();
                    newDoctor.setId(user.getId());

                    return newDoctor;
                });

        syncUserFieldsToDoctor(user, doctorProfile);

        if (doctorProfile.getHospitals() == null) {
            throw new IllegalStateException("Doctor hospitals list is not initialized");
        }

        if (doctorProfile.getHospitals().contains(hospital)) {
            throw new IllegalStateException("Doctor is already linked to this hospital");
        }

        doctorProfile.getHospitals().add(hospital);

        doctorRepository.save(doctorProfile);

        log.info(
                "Linked User {} as Doctor to Hospital {}",
                user.getEmail(),
                hospital.getName()
        );
    }

    @Transactional
    public void unlinkDoctorFromHospital(UUID doctorId, UUID hospitalId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor profile not found"));

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        if (doctor.getHospitals() == null || !doctor.getHospitals().contains(hospital)) {
            throw new IllegalStateException("Doctor is not linked to this hospital");
        }

        doctor.getHospitals().remove(hospital);

        doctor.setUpdatedAt(LocalDateTime.now());

        doctorRepository.save(doctor);

        log.info("Unlinked Doctor {} from Hospital {}", doctorId, hospitalId);
    }

    @Transactional
    public void removeDoctorProfile(UUID userId) {
        if (!doctorRepository.existsById(userId)) {
            throw new EntityNotFoundException("Cannot remove: Doctor profile does not exist");
        }

        doctorRepository.deleteById(userId);

        log.info("Removed Doctor profile for user {}", userId);
    }

    @Transactional
    public List<Doctor> findByHospitalId(UUID hospitalId) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new EntityNotFoundException("Hospital not found");
        }

        return doctorRepository.findByHospitalId(hospitalId)
                .stream()
                .map(doctor -> findById(doctor.getId()))
                .toList();
    }

    @Transactional
    public void deactivateProfile(UUID userId) {
        Doctor doctor = doctorRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor profile not found"));

        log.info(
                "Unlinking Doctor profile and clearing facility access for user: {}",
                userId
        );

        if (doctor.getHospitals() != null) {
            doctor.getHospitals().clear();
        }

        doctor.setUpdatedAt(LocalDateTime.now());

        doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public boolean belongsToHospital(UUID doctorId, UUID hospitalId) {
        if (doctorId == null) {
            throw new IllegalStateException("Doctor ID is required");
        }

        if (hospitalId == null) {
            throw new IllegalStateException("Hospital ID is required");
        }

        return doctorRepository.existsByIdAndHospitals_Id(doctorId, hospitalId);
    }

    @Transactional(readOnly = true)
    public void validateDoctorHospitalAccess(UUID doctorId, UUID hospitalId) {
        boolean belongsToHospital = belongsToHospital(doctorId, hospitalId);

        if (!belongsToHospital) {
            throw new IllegalStateException("Doctor does not belong to this hospital");
        }
    }

    private void syncUserFieldsToDoctor(User user, Doctor doctor) {
        doctor.setUsername(user.getUsername());
        doctor.setEmail(user.getEmail());
        doctor.setFirstName(user.getFirstName());
        doctor.setLastName(user.getLastName());
        doctor.setPhoneNumber(user.getPhoneNumber());
        doctor.setGender(user.getGender());
        doctor.setRole(user.getRole());
    }
}