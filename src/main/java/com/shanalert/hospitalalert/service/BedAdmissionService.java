package com.shanalert.hospitalalert.service;


import com.shanalert.hospitalalert.dto.BedOccupancyResponse;
import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.HospitalPatientResponse;
import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.entity.BedAdmission;
import com.shanalert.hospitalalert.mapper.BedMapper;
import com.shanalert.hospitalalert.model.AdmissionStatus;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.repository.BedAdmissionRepository;
import com.shanalert.hospitalalert.repository.BedRepository;
import com.shanalert.hospitalalert.repository.PatientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BedAdmissionService {

    @Autowired
    BedRepository bedRepository;

    @Autowired
    BedAdmissionRepository admissionRepository;

    @Autowired
    private PatientRepository  patientRepository;

    @Autowired
    BedMapper bedMapper;


    @Transactional
    public BedAdmission reserveBed(UUID hospitalId, BedType type, UUID patientId) {

        // 1. Validate patient exists
        if (!patientRepository.existsById(patientId)) {
            throw new EntityNotFoundException("Patient not found");
        }

        // 2. LOCK bed (prevents double booking)
        Bed availableBed = bedRepository
                .findLockedAvailableBed(hospitalId, type)
                .orElseThrow(() -> new RuntimeException("No beds available"));

        // 3. Safety check
        if (!availableBed.getHospitalId().equals(hospitalId)) {
            throw new IllegalStateException("Bed does not belong to hospital");
        }

        // 4. Create admission
        BedAdmission admission = new BedAdmission();
        admission.setBed(availableBed);
        admission.setPatientId(patientId);
        admission.setHospitalId(hospitalId);
        admission.setStatus(AdmissionStatus.RESERVED);
        admission.setReservedAt(LocalDateTime.now());

        BedAdmission saved = admissionRepository.save(admission);

        // 5. Update bed state
        availableBed.setStatus(BedStatus.RESERVED);
        bedRepository.save(availableBed);

        return saved;
    }

//    @Transactional
//    public void confirmArrival(UUID admissionId, UUID actorId) {
//
//        BedAdmission admission = admissionRepository.findById(admissionId)
//                .orElseThrow(() -> new EntityNotFoundException("Admission not found"));
//
//        // 🔥 UPDATED CONDITION
//        if (admission.getStatus() != AdmissionStatus.ARRIVED) {
//            throw new IllegalStateException("Patient must arrive before admission");
//        }
//
//        admission.setStatus(AdmissionStatus.ADMITTED);
//        admission.setAdmittedAt(LocalDateTime.now());
//        admission.setUpdatedBy(actorId);
//
//        Bed bed = admission.getBed();
//        bed.setStatus(BedStatus.OCCUPIED);
//
//        admissionRepository.save(admission);
//        bedRepository.save(bed);
//    }

//    @Transactional
//    public void discharge(UUID admissionId, UUID actorId) {
//
//        BedAdmission admission = admissionRepository.findById(admissionId)
//                .orElseThrow(() -> new EntityNotFoundException("Admission not found"));
//
//        // 1. Validate correct state
//        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
//            throw new IllegalStateException("Only admitted patients can be discharged");
//        }
//
//        // 2. Update admission
//        admission.setStatus(AdmissionStatus.DISCHARGED);
//        admission.setDischargedAt(LocalDateTime.now());
//        admission.setUpdatedBy(actorId);
//
//        // 3. Free bed
//        Bed bed = admission.getBed();
//        bed.setStatus(BedStatus.AVAILABLE);
//
//        admissionRepository.save(admission);
//        bedRepository.save(bed);
//
//    }


    public List<UUID> getCurrentPatients(UUID hospitalId, UserRole role) {

        // 1. Admin sees everything
        if (role == UserRole.ADMIN || role == UserRole.HOSPITAL_ADMIN || role == UserRole.DOCTOR ||  role == UserRole.NURSE) {
            return admissionRepository.findAllAdmittedPatients(AdmissionStatus.ADMITTED);
        }

        // 2. Hospital-scoped users
        return admissionRepository
                .findByHospitalIdAndStatus(hospitalId, AdmissionStatus.ADMITTED)
                .stream()
                .map(BedAdmission::getPatientId)
                .toList();
    }

    public Optional<BedAdmission> getCurrentOccupant(UUID bedId) {

        return admissionRepository
                .findTopByBedIdAndStatusOrderByAdmittedAtDesc(
                        bedId,
                        AdmissionStatus.ADMITTED
                );
    }

//    @Transactional
//    public void markAsArrived(UUID admissionId, UUID actorId) {
//
//        BedAdmission admission = admissionRepository.findById(admissionId)
//                .orElseThrow(() -> new EntityNotFoundException("Admission not found"));
//
//        if (admission.getStatus() != AdmissionStatus.RESERVED) {
//            throw new IllegalStateException("Only reserved admissions can be marked as arrived");
//        }
//
//        admission.setStatus(AdmissionStatus.ARRIVED);
//        admission.setUpdatedBy(actorId);
//
//        admissionRepository.save(admission);
//    }


//    @Transactional(readOnly = true)
//    public List<BedOccupancyResponse> getBedOccupancy(UUID hospitalId) {
//
//        // 1. Get all beds in hospital
//        List<Bed> beds = bedRepository.findAllByHospitalId(hospitalId);
//
//        List<UUID> bedIds = beds.stream()
//                .map(Bed::getId)
//                .toList();
//
//        // 2. Get active admissions
//        List<BedAdmission> admissions =
//                admissionRepository.findActiveAdmissionsForBeds(bedIds);
//
//        // 3. Map: bedId → admission
//        Map<UUID, BedAdmission> admissionMap = admissions.stream()
//                .collect(Collectors.toMap(
//                        ba -> ba.getBed().getId(),
//                        ba -> ba
//                ));
//
//        // 4. Build response
//        return beds.stream()
//                .map(bed -> {
//
//                    BedAdmission admission = admissionMap.get(bed.getId());
//
//                    UUID patientId = null;
//                    String patientName = null;
//                    String admissionStatus = null;
//
//                    if (admission != null) {
//                        patientId = admission.getPatientId();
//                        admissionStatus = admission.getStatus().name();
//
//                        // OPTIONAL: fetch patient
//                        // (optimize later with join)
//                        // Example:
//                        // Patient p = patientRepo.findById(patientId).orElse(null);
//                        // patientName = p != null ? p.getFirstName() + " " + p.getLastName() : null;
//                    }
//
//                    return new BedOccupancyResponse(
//                            bed.getId(),
//                            bed.getBedNumber(),
//                            bed.getBedType().name(),
//                            bed.getStatus().name(),
//                            patientId,
//                            patientName,
//                            admissionStatus
//                    );
//                })
//                .toList();
//    }


    // ... other methods ...

    @Transactional
    public BedAdmission confirmArrival(UUID admissionId, UUID actorId) {

        BedAdmission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new EntityNotFoundException("Admission not found"));

        if (admission.getStatus() != AdmissionStatus.ARRIVED) {
            throw new IllegalStateException("Patient must arrive before admission");
        }

        admission.setStatus(AdmissionStatus.ADMITTED);
        admission.setAdmittedAt(LocalDateTime.now());
        admission.setUpdatedBy(actorId);

        Bed bed = admission.getBed();
        bed.setStatus(BedStatus.OCCUPIED);

        bedRepository.save(bed);
        return admissionRepository.save(admission); // Returning the updated entity
    }

    @Transactional
    public BedAdmission discharge(UUID admissionId, UUID actorId) {

        BedAdmission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new EntityNotFoundException("Admission not found"));

        if (admission.getStatus() != AdmissionStatus.ADMITTED) {
            throw new IllegalStateException("Only admitted patients can be discharged");
        }

        admission.setStatus(AdmissionStatus.DISCHARGED);
        admission.setDischargedAt(LocalDateTime.now());
        admission.setUpdatedBy(actorId);

        Bed bed = admission.getBed();
        bed.setStatus(BedStatus.AVAILABLE);

        bedRepository.save(bed);
        return admissionRepository.save(admission); // Returning the updated entity
    }

    @Transactional
    public BedAdmission markAsArrived(UUID admissionId, UUID actorId) {

        BedAdmission admission = admissionRepository.findById(admissionId)
                .orElseThrow(() -> new EntityNotFoundException("Admission not found"));

        if (admission.getStatus() != AdmissionStatus.RESERVED) {
            throw new IllegalStateException("Only reserved admissions can be marked as arrived");
        }

        admission.setStatus(AdmissionStatus.ARRIVED);
        admission.setUpdatedBy(actorId);

        return admissionRepository.save(admission); // Returning the updated entity
    }
}
