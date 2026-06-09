package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.EmergencyAlertRequest;
import com.shanalert.hospitalalert.dto.EmergencyAlertResponse;
import com.shanalert.hospitalalert.dto.RouteEstimateDTO;
import com.shanalert.hospitalalert.entity.BedAdmission;
import com.shanalert.hospitalalert.entity.EmergencyAlert;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.mapper.EmergencyAlertMapper;
import com.shanalert.hospitalalert.model.AlertStatus;
import com.shanalert.hospitalalert.repository.EmergencyAlertRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyAlertService {

    private final HospitalService hospitalService;
    private final EmergencyAlertMapper alertMapper;
    private final LocationService locationService;
    private final BedAdmissionService bedAdmissionService;
    private final EmergencyAlertRepository alertRepository;

    @Transactional
    public EmergencyAlertResponse triggerAlert(EmergencyAlertRequest request) {

        log.info("Emergency Alert triggered for Patient: {} -> Hospital: {}",
                request.patientId(), request.hospitalId());

        Hospital hospital = hospitalService.getById(request.hospitalId());

        if (hospital.getAddress() == null
                || hospital.getAddress().getLatitude() == null
                || hospital.getAddress().getLongitude() == null) {
            throw new IllegalStateException("Hospital address or coordinates are missing.");
        }

        RouteEstimateDTO routeEstimate = locationService.getRouteEstimate(
                request.patientLat(),
                request.patientLng(),
                hospital.getAddress().getLatitude(),
                hospital.getAddress().getLongitude()
        );

        EmergencyAlert alert = new EmergencyAlert();
        alert.setPatientId(request.patientId());
        alert.setHospitalId(request.hospitalId());
        alert.setEstimatedArrivalTimeMinutes(routeEstimate.getEstimatedMinutes());
        alert.setEstimatedDistanceKm(routeEstimate.getDistanceKm());
        alert.setEtaSource(routeEstimate.getSource());
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
            alert.setStatus(AlertStatus.ON_THE_WAY);

            bedNumber = reservedBed.getBed().getBedNumber();
            statusMessage = "Ambulance dispatched. Bed " + bedNumber + " is reserved.";

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

        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        if (alert.getStatus() == AlertStatus.OCCUPIED) {
            throw new IllegalStateException("Patient is already checked into a bed");
        }

        alert.setStatus(AlertStatus.ARRIVED);

        String bedNumber = "Unassigned";
        String statusMessage = "Patient has arrived at the facility.";

        if (alert.getAdmissionId() != null) {
            BedAdmission admission = bedAdmissionService.markAsArrived(alert.getAdmissionId(), actorId);

            bedNumber = admission.getBed().getBedNumber();
            statusMessage = "Patient arrived. Proceed to Bed: " + bedNumber;
        } else {
            log.warn("Alert {} has no admission linked", alertId);
            statusMessage = "Arrival logged. Warning: No bed was reserved for this patient.";
        }

        EmergencyAlert savedAlert = alertRepository.save(alert);

        return alertMapper.toDto(savedAlert, bedNumber, statusMessage);
    }

    @Transactional
    public EmergencyAlertResponse checkInToBed(UUID alertId, UUID actorId) {

        log.info("Initiating Bed Check-in for Alert ID: {}", alertId);

        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        if (alert.getAdmissionId() == null) {
            throw new IllegalStateException("No admission linked to this alert");
        }

        if (alert.getStatus() != AlertStatus.ARRIVED) {
            throw new IllegalStateException("Patient must arrive before check-in");
        }

        BedAdmission admission = bedAdmissionService.confirmArrival(alert.getAdmissionId(), actorId);

        alert.setStatus(AlertStatus.OCCUPIED);

        EmergencyAlert savedAlert = alertRepository.save(alert);

        log.info("Check-in complete. Alert {} is now OCCUPIED", alertId);

        String bedLabel = admission.getBed().getBedNumber();

        return alertMapper.toDto(
                savedAlert,
                bedLabel,
                "Patient successfully checked into Bed " + bedLabel
        );
    }
}