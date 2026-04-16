package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.PatientUpdateDTO;
import com.shanalert.hospitalalert.entity.Patient;
import com.shanalert.hospitalalert.entity.User;
import com.shanalert.hospitalalert.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class PatientService {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AddressService addressService;

    @Autowired
    private UserService userService;

    @Transactional(readOnly = true)
    public Page<Patient> getAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable);
    }

    @Transactional
    public void deletePatientProfile(UUID userId) {
        Patient patient = patientRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Patient profile not found"));

        // We don't remove the row; we sync with the Master Identity's 'DELETED' status
        // and potentially clear sensitive emergency links
        patient.setAddress(null); // Optional: clear location for privacy on deletion
        patientRepository.save(patient);

        patientRepository.deleteById(userId);

        log.info("Patient profile deactivated for user: {}", userId);
    }

    @Transactional
    public Patient getPatientProfile(UUID userId) {
        // Fetch Master Identity
        User masterUser = userService.getById(userId);

        // Fetch existing or initialize a fresh profile
        Patient patient = patientRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("Lazy initializing Patient profile for user: {}", userId);
                    Patient newPatient = new Patient();
                    newPatient.setId(userId);
                    return newPatient;
                });

        // Always sync core fields to ensure they mirror the User table
        patient.setUsername(masterUser.getUsername());
        patient.setEmail(masterUser.getEmail());
        patient.setFirstName(masterUser.getFirstName());
        patient.setLastName(masterUser.getLastName());
        patient.setPhoneNumber(masterUser.getPhoneNumber());
        patient.setGender(masterUser.getGender());
        patient.setRole(masterUser.getRole());

        // Save and return
        return patientRepository.save(patient);
    }

    // 2. UPDATE: Fill in the Medical Data
    @Transactional
    public Patient updatePatientMedicalData(UUID userId, PatientUpdateDTO dto) {
        // We call getPatientProfile to ensure it exists and is synced first
        Patient patient = getPatientProfile(userId);

        if (dto.getDateOfBirth() != null) patient.setDateOfBirth(dto.getDateOfBirth());
        if (dto.getBloodGroup() != null) patient.setBloodGroup(dto.getBloodGroup());
        if (dto.getWeightKg() != null) patient.setWeightKg(dto.getWeightKg());
        if (dto.getHeightCm() != null) patient.setHeightCm(dto.getHeightCm());
        if (dto.getAllergies() != null) patient.setAllergies(dto.getAllergies());
        if (dto.getChronicConditions() != null) patient.setChronicConditions(dto.getChronicConditions());
        if (dto.getCurrentMedications() != null) patient.setCurrentMedications(dto.getCurrentMedications());

        if (dto.getNextOfKinName() != null) patient.setNextOfKinName(dto.getNextOfKinName());
        if (dto.getNextOfKinPhone() != null) patient.setNextOfKinPhone(dto.getNextOfKinPhone());
        if (dto.getNextOfKinRelationship() != null) {
            patient.setNextOfKinRelationship(dto.getNextOfKinRelationship());
        }
        if (dto.getInsuranceProvider() != null) patient.setInsuranceProvider(dto.getInsuranceProvider());
        if (dto.getInsurancePolicyNumber() != null) patient.setInsurancePolicyNumber(dto.getInsurancePolicyNumber());

        // Delegate address handling if provided
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


}
