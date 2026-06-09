package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.entity.BedAdmission;
import com.shanalert.hospitalalert.model.AdmissionStatus;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.repository.BedAdmissionRepository;
import com.shanalert.hospitalalert.repository.BedRepository;
import com.shanalert.hospitalalert.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedAdmissionService {

    private final BedRepository bedRepository;
    private final BedAdmissionRepository admissionRepository;
    private final PatientRepository patientRepository;

    private final HospitalAdminService hospitalAdminService;
    private final DoctorService doctorService;


    @Transactional
    public BedAdmission reserveBed(UUID hospitalId, BedType type, UUID patientId) {

        if (!patientRepository.existsById(patientId)) {
            throw new EntityNotFoundException("Patient not found");
        }

        Bed availableBed = bedRepository.findLockedAvailableBed(hospitalId, type).orElseThrow(() -> new IllegalStateException("No beds available"));

        if (availableBed.getHospital() == null || !availableBed.getHospital().getId().equals(hospitalId)) {
            throw new IllegalStateException("Bed does not belong to hospital");
        }

        BedAdmission admission = new BedAdmission();
        admission.setBed(availableBed);
        admission.setPatientId(patientId);
        admission.setHospitalId(hospitalId);
        admission.setStatus(AdmissionStatus.RESERVED);
        admission.setReservedAt(LocalDateTime.now());

        availableBed.setStatus(BedStatus.RESERVED);

        bedRepository.save(availableBed);

        BedAdmission savedAdmission = admissionRepository.save(admission);

        log.info("Reserved bed {} for patient {} at hospital {}", availableBed.getBedNumber(), patientId, hospitalId);

        return savedAdmission;
    }

    @Transactional
    public BedAdmission markAsArrived(UUID admissionId, UUID actorId) {

        BedAdmission admission = admissionRepository.findById(admissionId).orElseThrow(() -> new EntityNotFoundException("Admission not found"));

        if (admission.getStatus() != AdmissionStatus.RESERVED) {
            throw new IllegalStateException("Only reserved admissions can be marked as arrived");
        }

        admission.setStatus(AdmissionStatus.ARRIVED);
        admission.setUpdatedBy(actorId);

        BedAdmission savedAdmission = admissionRepository.save(admission);

        log.info("Admission {} marked as ARRIVED by {}", admissionId, actorId);

        return savedAdmission;
    }

    @Transactional
    public BedAdmission confirmArrival(UUID admissionId, UUID actorId) {

        BedAdmission admission = admissionRepository.findById(admissionId).orElseThrow(() -> new EntityNotFoundException("Admission not found"));

        if (admission.getStatus() != AdmissionStatus.ARRIVED) {
            throw new IllegalStateException("Patient must arrive before admission");
        }

        admission.setStatus(AdmissionStatus.ADMITTED);
        admission.setAdmittedAt(LocalDateTime.now());
        admission.setUpdatedBy(actorId);

        Bed bed = admission.getBed();

        if (bed == null) {
            throw new IllegalStateException("Admission has no bed linked");
        }

        bed.setStatus(BedStatus.OCCUPIED);

        bedRepository.save(bed);

        BedAdmission savedAdmission = admissionRepository.save(admission);

        log.info("Admission {} confirmed. Bed {} is now OCCUPIED", admissionId, bed.getBedNumber());

        return savedAdmission;
    }

    @Transactional
    public BedAdmission discharge(UUID admissionId, UUID actorId) {

        BedAdmission admission = admissionRepository.findById(admissionId).orElseThrow(() -> new EntityNotFoundException("Admission not found"));

        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new IllegalStateException("Only admitted patients can be discharged");
        }

        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setDischargedAt(LocalDateTime.now());
        admission.setUpdatedBy(actorId);

        Bed bed = admission.getBed();

        if (bed == null) {
            throw new IllegalStateException("Admission has no bed linked");
        }

        bed.setStatus(BedStatus.AVAILABLE);

        bedRepository.save(bed);

        BedAdmission savedAdmission = admissionRepository.save(admission);

        log.info("Admission {} discharged. Bed {} is now AVAILABLE", admissionId, bed.getBedNumber());

        return savedAdmission;
    }

    @Transactional(readOnly = true)
    public List<UUID> getCurrentPatients(UUID hospitalId, UserRole role, UUID actorId) {

        validateCurrentPatientAccess(hospitalId, role, actorId);

        if (role == UserRole.ADMIN) {
            log.info("Admin {} fetching all admitted patients", actorId);

            return admissionRepository.findAllAdmittedPatients(AdmissionStatus.ADMITTED);
        }

        log.info("{} {} fetching admitted patients for hospital {}", role, actorId, hospitalId);

        return admissionRepository.findByHospitalIdAndStatus(hospitalId, AdmissionStatus.ADMITTED).stream().map(BedAdmission::getPatientId).toList();
    }

    @Transactional(readOnly = true)
    public Optional<BedAdmission> getCurrentOccupant(UUID bedId) {
        return admissionRepository.findTopByBedIdAndStatusOrderByAdmittedAtDesc(bedId, AdmissionStatus.ADMITTED);
    }

    private void validateCurrentPatientAccess(UUID hospitalId, UserRole role, UUID actorId) {

        if (actorId == null) {
            throw new IllegalStateException("Actor ID is required");
        }

        if (role == null) {
            throw new IllegalStateException("User role is required");
        }

        if (role == UserRole.ADMIN) {
            return;
        }

        if (hospitalId == null) {
            throw new IllegalStateException("Hospital ID is required for hospital-scoped users");
        }

        switch (role) {
            case HOSPITAL_ADMIN -> hospitalAdminService.validateHospitalAdminAccess(actorId, hospitalId);
            case DOCTOR -> doctorService.validateDoctorHospitalAccess(actorId, hospitalId);
            default -> throw new IllegalStateException("You are not allowed to view admitted patients");
        }
    }
}