package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.*;
import com.shanalert.hospitalalert.entity.BedAdmission;
import com.shanalert.hospitalalert.entity.EmergencyAlert;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.mapper.EmergencyAlertMapper;
import com.shanalert.hospitalalert.model.AlertStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.repository.EmergencyAlertRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shanalert.hospitalalert.model.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
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
    public EmergencySearchResponse searchHospitalsForEmergency(EmergencySearchRequest request) {

        log.info("Searching hospitals for patient: {}, emergency type: {}",
                request.patientId(), request.emergencyType());

        BedType requestedBedType = request.emergencyType().getRequiredBedType();

        EmergencyAlert alert = new EmergencyAlert();
        alert.setPatientId(request.patientId());
        alert.setRequestedBedType(requestedBedType);
        alert.setPatientNotes(request.patientNotes());
        alert.setStatus(AlertStatus.PENDING);

        // Store patient location so booking can use it later
        alert.setPatientLat(request.patientLat());
        alert.setPatientLng(request.patientLng());

        EmergencyAlert savedAlert = alertRepository.save(alert);

        List<HospitalDiscoveryResponse> hospitals = hospitalService.findHospitalsForEmergency(
                request.emergencyType(),
                request.patientLat(),
                request.patientLng()
        );

        return new EmergencySearchResponse(savedAlert.getId(), hospitals);
    }

    /**
     * STEP 2:
     * Patient selects one hospital from the search results.
     * This reserves a bed and creates the emergency alert.
     */
    @Transactional
    public EmergencyAlertResponse bookSelectedHospital(EmergencyBookingRequest request) {

        log.info("Booking selected hospital. Alert: {}, Hospital: {}",
                request.alertId(), request.hospitalId());

        EmergencyAlert alert = alertRepository.findById(request.alertId())
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        if (alert.getStatus() != AlertStatus.PENDING) {
            throw new IllegalStateException("This alert has already been booked or processed");
        }

        Hospital hospital = hospitalService.getById(request.hospitalId());

        if (hospital.getAddress() == null
                || hospital.getAddress().getLatitude() == null
                || hospital.getAddress().getLongitude() == null) {
            throw new IllegalStateException("Hospital address or coordinates are missing.");
        }

        if (alert.getPatientLat() == null || alert.getPatientLng() == null) {
            throw new IllegalStateException("Patient location is missing from alert");
        }

        RouteEstimateDTO routeEstimate = locationService.getRouteEstimate(
                alert.getPatientLat(),
                alert.getPatientLng(),
                hospital.getAddress().getLatitude(),
                hospital.getAddress().getLongitude()
        );

        BedAdmission reservedBed = bedAdmissionService.reserveBed(
                request.hospitalId(),
                alert.getRequestedBedType(),
                alert.getPatientId()
        );

        alert.setHospitalId(request.hospitalId());
        alert.setAdmissionId(reservedBed.getId());
        alert.setEstimatedArrivalTimeMinutes(routeEstimate.getEstimatedMinutes());
        alert.setEstimatedDistanceKm(routeEstimate.getDistanceKm());
        alert.setEtaSource(routeEstimate.getSource());
        alert.setStatus(AlertStatus.ON_THE_WAY);

        EmergencyAlert savedAlert = alertRepository.save(alert);

        String bedNumber = reservedBed.getBed().getBedNumber();

        return alertMapper.toDto(
                savedAlert,
                bedNumber,
                "Hospital selected. Bed " + bedNumber + " is reserved."
        );
    }
    /**
     * STEP 3:
     * Hospital marks patient as arrived.
     */
    @Transactional
    public EmergencyAlertResponse markPatientAsArrived(UUID alertId, UUID actorId) {

        log.info("Processing arrival for Alert ID: {}", alertId);

        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        if (alert.getStatus() == AlertStatus.OCCUPIED) {
            throw new IllegalStateException("Patient is already checked into a bed");
        }

        if (alert.getStatus() == AlertStatus.FAILED) {
            throw new IllegalStateException("Cannot mark arrival for a failed alert");
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

    /**
     * STEP 4:
     * Hospital checks patient into reserved bed.
     */
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

        String bedLabel = admission.getBed().getBedNumber();

        log.info("Check-in complete. Alert {} is now OCCUPIED", alertId);

        return alertMapper.toDto(
                savedAlert,
                bedLabel,
                "Patient successfully checked into Bed " + bedLabel
        );
    }


    @Transactional(readOnly = true)
    public Page<EmergencyAlertResponse> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable)
                .map(alert -> alertMapper.toDto(
                        alert,
                        null,
                        "Alert retrieved successfully"
                ));
    }

    @Transactional(readOnly = true)
    public EmergencyAlertResponse getAlertById(UUID alertId) {
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        return alertMapper.toDto(
                alert,
                null,
                "Alert retrieved successfully"
        );
    }

    @Transactional(readOnly = true)
    public List<EmergencyAlertResponse> getAlertsByPatient(UUID patientId) {
        return alertRepository.findByPatientId(patientId)
                .stream()
                .map(alert -> alertMapper.toDto(
                        alert,
                        null,
                        "Patient alert retrieved successfully"
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmergencyAlertResponse> getAlertsByHospital(UUID hospitalId) {
        return alertRepository.findByHospitalId(hospitalId)
                .stream()
                .map(alert -> alertMapper.toDto(
                        alert,
                        null,
                        "Hospital alert retrieved successfully"
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmergencyAlertResponse> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatus(status)
                .stream()
                .map(alert -> alertMapper.toDto(
                        alert,
                        null,
                        "Alert retrieved by status"
                ))
                .toList();
    }

    @Transactional
    public EmergencyAlertResponse cancelAlert(UUID alertId) {
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        if (alert.getStatus() == AlertStatus.OCCUPIED) {
            throw new IllegalStateException("Cannot cancel alert because patient is already checked into bed");
        }

        if (alert.getStatus() == AlertStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed alert");
        }

        alert.setStatus(AlertStatus.CANCELLED);

        EmergencyAlert savedAlert = alertRepository.save(alert);

        return alertMapper.toDto(
                savedAlert,
                null,
                "Alert cancelled successfully"
        );
    }

    @Transactional
    public void deleteAlert(UUID alertId) {
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        if (alert.getStatus() == AlertStatus.ON_THE_WAY
                || alert.getStatus() == AlertStatus.ARRIVED
                || alert.getStatus() == AlertStatus.OCCUPIED) {
            throw new IllegalStateException("Cannot delete an active alert. Cancel or complete it first.");
        }

        alertRepository.delete(alert);
    }
}