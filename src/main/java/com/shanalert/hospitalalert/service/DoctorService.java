package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.DoctorUpdateDto;
import com.shanalert.hospitalalert.entity.Doctor;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.repository.DoctorRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Lazy
    @Autowired
    private UserService userService;

    @Transactional(readOnly = true)
    public Page<Doctor> findAllPaged(Pageable pageable) {
        log.info("Fetching paginated Doctors: Page {}, Size {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return doctorRepository.findAll(pageable);
    }

    @Transactional
    public Doctor findById(UUID userId) {
        // 1. Fetch Master Identity from User table
        User masterUser = userService.getById(userId);

        // 2. Fetch existing or initialize a fresh profile (JIT Provisioning)
        Doctor doctor = doctorRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing Doctor profile for user: {}", userId);
                    Doctor newDoctor = new Doctor();
                    newDoctor.setId(userId);
                    return newDoctor;
                });

        // 3. Sync core fields to ensure they mirror the User table
        syncUserFieldsToDoctor(masterUser, doctor);

        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor updateDoctorProfile(UUID userId, DoctorUpdateDto dto, UUID actorId) {
        Doctor existing = findById(userId); // Uses your JIT logic to fetch/sync

        // 1. Update Identity Fields
        if (dto.firstName() != null) existing.setFirstName(dto.firstName());
        if (dto.lastName() != null) existing.setLastName(dto.lastName());
        if (dto.phoneNumber() != null) existing.setPhoneNumber(dto.phoneNumber());
        if (dto.gender() != null) existing.setGender(dto.gender());

        // 2. Update Professional Fields
        if (dto.medicalLicenseNumber() != null) existing.setMedicalLicenseNumber(dto.medicalLicenseNumber());
        if (dto.specialization() != null) existing.setSpecialization(dto.specialization());
        if (dto.qualifications() != null) existing.setQualifications(dto.qualifications());
        if (dto.yearsOfExperience() != null) existing.setYearsOfExperience(dto.yearsOfExperience());
        if (dto.department() != null) existing.setDepartment(dto.department());
        if (dto.isAvailable() != null) existing.setIsAvailable(dto.isAvailable());

        // 3. Update Hospital Associations (ManyToMany)
        if (dto.newHospitalId() != null) {
            Hospital hospital = hospitalRepository.findById(dto.newHospitalId())
                    .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

            if (!existing.getHospitals().contains(hospital)) {
                existing.getHospitals().add(hospital);
            }
        }

        existing.setUpdatedBy(actorId);
        existing.setUpdatedAt(LocalDateTime.now());

        return doctorRepository.save(existing);
    }

    @Transactional
    public void linkUserToHospital(UUID userId, UUID hospitalId) {
        User user = userService.getById(userId);
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        if (doctorRepository.existsById(userId)) {
            throw new IllegalStateException("User is already assigned as a Doctor");
        }

        Doctor doctorProfile = new Doctor();
        doctorProfile.setId(user.getId());
        syncUserFieldsToDoctor(user, doctorProfile);

        // Initialize the collection and add the first hospital
        // Ensure your Doctor entity has: private List<Hospital> hospitals = new ArrayList<>();
        doctorProfile.getHospitals().add(hospital);

        doctorRepository.save(doctorProfile);
        log.info("Linked User {} as Doctor to Hospital {}", user.getEmail(), hospital.getName());
    }

    @Transactional
    public void removeDoctorProfile(UUID userId) {
        if (!doctorRepository.existsById(userId)) {
            throw new EntityNotFoundException("Cannot remove: Doctor profile does not exist");
        }
        doctorRepository.deleteById(userId);
    }

    /**
     * Finds doctors by hospital ID and ensures their profiles are
     * synced with the Master User data.
     */
    @Transactional
    public List<Doctor> findByHospitalId(UUID hospitalId) {
        List<Doctor> doctors = doctorRepository.findByHospitalId(hospitalId);

        return doctors.stream()
                .map(doctor -> findById(doctor.getId())) // Re-uses JIT logic to sync fields
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivateProfile(UUID userId) {
        doctorRepository.findById(userId).ifPresent(doctor -> {
            log.info("Unlinking Doctor profile and clearing facility access for user: {}", userId);
            doctor.getHospitals().clear(); // Removes all hospital associations
            doctorRepository.save(doctor);
        });
    }

    /**
     * Helper to keep Doctor and User data in sync
     */
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