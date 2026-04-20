package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.EmergencyAlertRequest;
import com.shanalert.hospitalalert.dto.EmergencyAlertResponse;
import com.shanalert.hospitalalert.entity.BedAdmission;
import com.shanalert.hospitalalert.entity.EmergencyAlert;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.mapper.EmergencyAlertMapper;
import com.shanalert.hospitalalert.model.AlertStatus;
import com.shanalert.hospitalalert.repository.EmergencyAlertRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class EmergencyAlertService {

    @Autowired
    private HospitalService hospitalService;

    @Autowired
    private EmergencyAlertMapper alertMapper;

    @Autowired
    private LocationService locationService;

    @Autowired
    private BedService bedService;

    @Autowired
    private EmergencyAlertRepository alertRepository;

    @Autowired
    private BedAdmissionService bedAdmissionService;



    @Transactional
    public EmergencyAlertResponse triggerAlert(EmergencyAlertRequest request) {

        log.info("Emergency Alert triggered for Patient: {} -> Hospital: {}",
                request.patientId(), request.hospitalId());

        // 1. Get Hospital & Location Details
        Hospital hospital = hospitalService.getById(request.hospitalId());

        // Ensure the hospital has coordinates set
        if (hospital.getAddress() == null || hospital.getAddress().getLatitude() == null) {
            throw new IllegalStateException("Hospital address or coordinates are missing.");
        }
//         1. Logic for OSRM ETA calculation (omitted for brevity)
        Integer eta = null;

        try {
            eta = locationService.getEstimatedMinutes(request.patientLat(), request.patientLng(), hospital.getAddress().getLatitude(),
                    hospital.getAddress().getLongitude() );
        }catch (Exception e) {
            log.error("OSRM Service unreachable, proceeding without ETA");
        }

        // 2. Initialize the Alert
        EmergencyAlert alert = new EmergencyAlert();
        alert.setPatientId(request.patientId());
        alert.setHospitalId(request.hospitalId());
        alert.setEstimatedArrivalTimeMinutes(eta);
        alert.setRequestedBedType(request.requestedBedType());
        alert.setPatientNotes(request.patientNotes());

        String bedNumber = null;
        String statusMessage;

        try {
            BedAdmission reservedBed = bedAdmissionService.reserveBed(
                    request.hospitalId(),
                    request.requestedBedType(),
                    request.patientId()
            );

            alert.setAdmissionId(reservedBed.getId());
            alert.setStatus(AlertStatus.ON_THE_WAY); // Using your new status!

            bedNumber = reservedBed.getBed().getBedNumber();
            statusMessage = "Ambulance dispatched. Bed " + bedNumber + " is ON THE WAY.";

        } catch (Exception e) {
            log.error("Failed to reserve bed during alert trigger: {}", e.getMessage());
            alert.setStatus(AlertStatus.FAILED);
            statusMessage = "Alert logged, but no beds were found. Redirecting to backup.";
        }

        EmergencyAlert savedAlert = alertRepository.save(alert);
        return alertMapper.toDto(savedAlert, bedNumber, statusMessage);
    }


    @Transactional
    public EmergencyAlertResponse markPatientAsArrived(UUID alertId, UUID actorId) {
        log.info("Processing arrival for Alert ID: {}", alertId);

        // 1. Fetch and Validate the Alert
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        // 2. Update Alert Status
        alert.setStatus(AlertStatus.ARRIVED);

        // 3. Update Admission & Get Bed Info
        String bedNumber = "Unassigned"; // Default for alerts with no reserved bed
        String statusMessage = "Patient has arrived at the facility.";

        if (alert.getAdmissionId() != null) {
            // We let the Admission Service handle the logic and return the object
            // This avoids making redundant queries in the Alert Service
            BedAdmission admission = bedAdmissionService.markAsArrived(alert.getAdmissionId(), actorId);

            bedNumber = admission.getBed().getBedNumber();
            statusMessage = "Patient arrived. Proceed to Bed: " + bedNumber;
        } else {
            log.warn("Alert {} has no admission linked", alertId);
            statusMessage = "Arrival logged. Warning: No bed was reserved for this patient.";
        }

        // 4. Save the Alert (Auditable fields update here)
        EmergencyAlert savedAlert = alertRepository.save(alert);

        // 5. Return the DTO using your mapper
        return alertMapper.toDto(savedAlert, bedNumber, statusMessage);
    }


    @Transactional
    public EmergencyAlertResponse checkInToBed(UUID alertId, UUID actorId) {

        log.info("Initiating Bed Check-in for Alert ID: {}", alertId);

        // 1. Fetch and Validate Alert
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        if (alert.getAdmissionId() == null) {
            throw new IllegalStateException("No admission linked to this alert");
        }

        if (alert.getStatus() != AlertStatus.ARRIVED) {
            throw new IllegalStateException("Patient must arrive before check-in");
        }

        // 2. Confirm admission (Capturing the returned BedAdmission object)
        // This method now updates Admission to ADMITTED and Bed to OCCUPIED
        BedAdmission admission = bedAdmissionService.confirmArrival(alert.getAdmissionId(), actorId);

        // 3. Update Alert state
        alert.setStatus(AlertStatus.OCCUPIED);
        EmergencyAlert savedAlert = alertRepository.save(alert);

        log.info("Check-in complete. Alert {} is now OCCUPIED", alertId);

        // 4. Return the mapped DTO
        // We pull the bed number directly from the admission graph
        String bedLabel = admission.getBed().getBedNumber();

        return alertMapper.toDto(
                savedAlert,
                bedLabel,
                "Patient successfully checked into Bed " + bedLabel
        );
    }

}
