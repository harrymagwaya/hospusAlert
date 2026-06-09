package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.PatientUpdateDTO;
import com.shanalert.hospitalalert.entity.Patient;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final AddressService addressService;
    private final UserService userService;

    public PatientService(
            PatientRepository patientRepository,
            AddressService addressService,
            @Lazy UserService userService
    ) {
        this.patientRepository = patientRepository;
        this.addressService = addressService;
        this.userService = userService;
    }

    /**
     * Fetches all users with PATIENT role, then lazy-creates/syncs their Patient profiles.
     */
    @Transactional
    public Page<Patient> getAllPatients(Pageable pageable) {
        log.info(
                "Fetching paginated Patient users: Page {}, Size {}",
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return userService.findUsersByRole(UserRole.PATIENT, pageable)
                .map(user -> getPatientProfile(user.getId()));
    }

    /**
     * Gets a patient profile by user ID.
     * If the Patient profile does not exist yet, it creates it from the User record.
     */
    @Transactional
    public Patient getPatientProfile(UUID userId) {
        User masterUser = userService.getById(userId);

        if (masterUser.getRole() != UserRole.PATIENT) {
            throw new IllegalStateException("User is not a patient");
        }

        Patient patient = patientRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing Patient profile for user: {}", userId);

                    Patient newPatient = new Patient();
                    newPatient.setId(userId);

                    return newPatient;
                });

        syncUserFieldsToPatient(masterUser, patient);

        return patientRepository.save(patient);
    }

    @Transactional
    public Patient updatePatientMedicalData(UUID userId, PatientUpdateDTO dto) {
        Patient patient = getPatientProfile(userId);

        if (dto.getDateOfBirth() != null) {
            patient.setDateOfBirth(dto.getDateOfBirth());
        }

        if (dto.getBloodGroup() != null) {
            patient.setBloodGroup(dto.getBloodGroup());
        }

        if (dto.getWeightKg() != null) {
            patient.setWeightKg(dto.getWeightKg());
        }

        if (dto.getHeightCm() != null) {
            patient.setHeightCm(dto.getHeightCm());
        }

        if (dto.getAllergies() != null) {
            patient.setAllergies(dto.getAllergies());
        }

        if (dto.getChronicConditions() != null) {
            patient.setChronicConditions(dto.getChronicConditions());
        }

        if (dto.getCurrentMedications() != null) {
            patient.setCurrentMedications(dto.getCurrentMedications());
        }

        if (dto.getNextOfKinName() != null) {
            patient.setNextOfKinName(dto.getNextOfKinName());
        }

        if (dto.getNextOfKinPhone() != null) {
            patient.setNextOfKinPhone(dto.getNextOfKinPhone());
        }

        if (dto.getNextOfKinRelationship() != null) {
            patient.setNextOfKinRelationship(dto.getNextOfKinRelationship());
        }

        if (dto.getInsuranceProvider() != null) {
            patient.setInsuranceProvider(dto.getInsuranceProvider());
        }

        if (dto.getInsurancePolicyNumber() != null) {
            patient.setInsurancePolicyNumber(dto.getInsurancePolicyNumber());
        }

        if (dto.getAddress() != null) {
            if (patient.getAddress() == null) {
                patient.setAddress(addressService.saveAddress(dto.getAddress()));
            } else {
                addressService.updateAddress(patient.getAddress().getId(), dto.getAddress());
            }
        }

        log.info("Medical profile updated for patient: {}", userId);

        return patientRepository.save(patient);
    }

    @Transactional
    public void deletePatientProfile(UUID userId) {
        if (!patientRepository.existsById(userId)) {
            throw new EntityNotFoundException("Patient profile not found");
        }

        patientRepository.deleteById(userId);

        log.info("Patient profile deleted for user: {}", userId);
    }

    @Transactional(readOnly = true)
    public boolean existsById(UUID patientId) {
        return patientRepository.existsById(patientId);
    }

    private void syncUserFieldsToPatient(User user, Patient patient) {
        patient.setUsername(user.getUsername());
        patient.setEmail(user.getEmail());
        patient.setFirstName(user.getFirstName());
        patient.setLastName(user.getLastName());
        patient.setPhoneNumber(user.getPhoneNumber());
        patient.setGender(user.getGender());
        patient.setRole(user.getRole());
    }
}