package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.EmergencyAlertRequest;
import com.shanalert.hospitalalert.dto.EmergencyAlertResponse;
import com.shanalert.hospitalalert.service.EmergencyAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class EmergencyAlertController {

    private final EmergencyAlertService alertService;

    /**
     * Triggered by the Patient App.
     * Calculates ETA, reserves a bed, and notifies the hospital.
     */
    @PostMapping("/trigger")
    @ResponseStatus(HttpStatus.CREATED)
    public EmergencyAlertResponse triggerAlert(@RequestBody EmergencyAlertRequest request) {
        return alertService.triggerAlert(request);
    }

    /**
     * Triggered by the Hospital App when the ambulance/patient arrives at the gate.
     */
    @PatchMapping("/{alertId}/arrive")
    @ResponseStatus(HttpStatus.OK)
    public EmergencyAlertResponse markAsArrived(@PathVariable UUID alertId) {
        return alertService.markAsArrived(alertId);
    }

    /**
     * Triggered when the patient is physically moved into the reserved bed.
     * This finalizes the bed occupancy in the system.
     */
    @PatchMapping("/{alertId}/check-in")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void checkInToBed(@PathVariable UUID alertId) {
        alertService.checkInToBed(alertId);
    }
}