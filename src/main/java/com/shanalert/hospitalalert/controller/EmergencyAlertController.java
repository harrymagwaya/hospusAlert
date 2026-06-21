package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.EmergencyAlertResponse;
import com.shanalert.hospitalalert.dto.EmergencyBookingRequest;
import com.shanalert.hospitalalert.dto.EmergencySearchRequest;
import com.shanalert.hospitalalert.dto.EmergencySearchResponse;
import com.shanalert.hospitalalert.model.AlertStatus;
import com.shanalert.hospitalalert.service.EmergencyAlertService;
import com.shanalert.hospitalalert.util.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class EmergencyAlertController {

    private final EmergencyAlertService alertService;

    /**
     * Step 1: Patient searches for available hospitals.
     * Creates a PENDING alert and returns alertId + hospital options.
     */
    @PostMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public EmergencySearchResponse searchHospitals(
            @Valid @RequestBody EmergencySearchRequest request
    ) {
        return alertService.searchHospitalsForEmergency(request);
    }

    /**
     * Step 2: Patient selects hospital and books/reserves bed.
     */
    @PostMapping("/book")
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyAlertResponse bookSelectedHospital(
            @Valid @RequestBody EmergencyBookingRequest request
    ) {
        return alertService.bookSelectedHospital(request);
    }

    /**
     * Get all alerts paginated.
     */
    @GetMapping
    public Page<EmergencyAlertResponse> getAllAlerts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return alertService.getAllAlerts(pageable);
    }

    /**
     * Get alert by ID.
     */
    @GetMapping("/{alertId}")
    public EmergencyAlertResponse getAlertById(
            @PathVariable UUID alertId
    ) {
        return alertService.getAlertById(alertId);
    }

    /**
     * Get alerts by patient.
     */
    @GetMapping("/patient/{patientId}")
    public List<EmergencyAlertResponse> getAlertsByPatient(
            @PathVariable UUID patientId
    ) {
        return alertService.getAlertsByPatient(patientId);
    }

    /**
     * Get alerts by hospital.
     */
    @GetMapping("/hospital/{hospitalId}")
    public List<EmergencyAlertResponse> getAlertsByHospital(
            @PathVariable UUID hospitalId
    ) {
        return alertService.getAlertsByHospital(hospitalId);
    }

    /**
     * Get alerts by status.
     */
    @GetMapping("/status/{status}")
    public List<EmergencyAlertResponse> getAlertsByStatus(
            @PathVariable AlertStatus status
    ) {
        return alertService.getAlertsByStatus(status);
    }

    /**
     * Step 3: Hospital marks patient as arrived.
     */
    @PatchMapping("/{alertId}/arrive")
    @ResponseStatus(HttpStatus.OK)
    public EmergencyAlertResponse markAsArrived(
            @PathVariable UUID alertId,
            @RequestHeader(AppConstants.ACTOR_ID) UUID actorId
    ) {
        return alertService.markPatientAsArrived(alertId, actorId);
    }

    /**
     * Step 4: Hospital checks patient into bed.
     */
    @PatchMapping("/{alertId}/check-in")
    @ResponseStatus(HttpStatus.OK)
    public EmergencyAlertResponse checkInToBed(
            @PathVariable UUID alertId,
            @RequestHeader(AppConstants.ACTOR_ID) UUID actorId
    ) {
        return alertService.checkInToBed(alertId, actorId);
    }

    /**
     * Cancel alert.
     */
    @PatchMapping("/{alertId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    public EmergencyAlertResponse cancelAlert(
            @PathVariable UUID alertId
    ) {
        return alertService.cancelAlert(alertId);
    }

    /**
     * Delete alert.
     */
    @DeleteMapping("/{alertId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAlert(
            @PathVariable UUID alertId
    ) {
        alertService.deleteAlert(alertId);
    }
}