package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.EmergencyAlertRequest;
import com.shanalert.hospitalalert.dto.EmergencyAlertResponse;
import com.shanalert.hospitalalert.entity.EmergencyAlert;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.mapper.EmergencyAlertMapper;
import com.shanalert.hospitalalert.model.AlertStatus;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.model.EmergencyType;
import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.repository.EmergencyAlertRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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


//
//    @Transactional
//    public EmergencyAlertResponse triggerAlert(EmergencyAlertRequest request) {
//        log.info("Emergency Alert triggered for Patient: {} -> Hospital: {}",
//                request.patientId(), request.hospitalId());
//
//        // 1. Get Hospital & Location Details
//        Hospital hospital = hospitalService.findById(request.hospitalId())
//                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));
//
//        // Ensure the hospital has coordinates set
//        if (hospital.getAddress() == null || hospital.getAddress().getLatitude() == null) {
//            throw new IllegalStateException("Hospital address or coordinates are missing.");
//        }
//
//        // 2. Calculate real ETA via OSRM (LocationService)
//        Integer eta = locationService.getEstimatedMinutes(
//                request.patientLat(),
//                request.patientLng(),
//                hospital.getAddress().getLatitude(),
//                hospital.getAddress().getLongitude()
//        );
//
//        // 3. Initialize the EmergencyAlert Entity
//        EmergencyAlert alert = new EmergencyAlert();
//        alert.setPatientId(request.patientId());
//        alert.setHospitalId(request.hospitalId());
//        alert.setRequestedBedType(request.requestedBedType());
//        alert.setPatientNotes(request.patientNotes());
//        alert.setEstimatedArrivalTimeMinutes(eta);
//        alert.setStatus(AlertStatus.IN_TRANSIT);
//
//        String message;
//        String assignedBedNumber = null;
//
//        // 4. THE CRITICAL STEP: Attempt to reserve the bed immediately
//        try {
//            // This calls the BedService logic we wrote earlier
//            BedResponse reservedBed = bedService.reserveBedForPatient(
//                    request.hospitalId(),
//                    request.requestedBedType(),
//                    request.patientId()
//            );
//
//            // Link the specific bed to the alert
//            alert.setAssignedBedId(reservedBed.bedId());
//            alert.setStatus(AlertStatus.RESPONDING); // Move from PENDING to RESPONDING
//            assignedBedNumber = reservedBed.bedNumber();
//            message = "Emergency Alert active. Bed " + assignedBedNumber + " reserved. ETA: " + eta + " mins.";
//
//        } catch (Exception e) {
//            log.error("Alert created but bed reservation failed: {}", e.getMessage());
//            alert.setStatus(AlertStatus.FAILED);
//            message = "Emergency received, but no " + request.requestedBedType() + " beds available. Manual triage required.";
//        }
//
//        // 5. Save the Alert
//        EmergencyAlert savedAlert = alertRepository.save(alert);
//
//        // 6. Return the fully mapped Response
//        return alertMapper.toDto(savedAlert, bedNumber, message);
//    }

    @Transactional
    public EmergencyAlertResponse triggerAlert(EmergencyAlertRequest request) {
        // 1. Logic for OSRM ETA calculation (omitted for brevity)
        Integer eta = locationService.getEstimatedMinutes(...);

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
            // CALLING THE NEW SERVICE METHOD
            BedResponse reservedBed = bedService.reserveBedForEmergency(
                    request.hospitalId(),
                    request.requestedBedType(),
                    request.patientId()
            );

            alert.setAssignedBedId(reservedBed.bedId());
            alert.setStatus(AlertStatus.ON_THE_WAY); // Using your new status!

            bedNumber = reservedBed.bedNumber();
            statusMessage = "Ambulance dispatched. Bed " + bedNumber + " is ON THE WAY.";

        } catch (Exception e) {
            log.error("Failed to reserve bed during alert trigger: {}", e.getMessage());
            alert.setStatus(AlertStatus.FAILED);
            statusMessage = "Alert logged, but no beds were found. Redirecting to backup.";
        }

        EmergencyAlert savedAlert = alertRepository.save(alert);
        return alertMapper.toDto(savedAlert, bedNumber, statusMessage);
    }


    /**
     * Step 1: Patient reaches the facility.
     * The alert status changes, but the bed is still RESERVED.
     */
    @Transactional
    public void markPatientAsArrived(UUID alertId) {
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        alert.setStatus(AlertStatus.ARRIVED);
        alertRepository.save(alert);
    }

    @Transactional
    public void checkInToBed(UUID alertId) {
        // 1. Update Alert Status
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));
        alert.setStatus(AlertStatus.OCCUPIED);
        alertRepository.save(alert);

        // 2. Delegate Bed Update to BedService
        // DON'T fetch and save here. Tell the BedService to handle its own state.
        if (alert.getAssignedBedId() != null) {
            bedService.confirmArrival(alert.getAssignedBedId());
        }
    }

    @Transactional
    public EmergencyAlertResponse markAsArrived(UUID alertId) {
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        alert.setStatus(AlertStatus.ARRIVED);
        EmergencyAlert savedAlert = alertRepository.save(alert);

        // We return the DTO so the Hospital App knows the status update hit the DB
        return alertMapper.toDto(savedAlert, null, "Patient has arrived at facility");
    }
}
