package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.HospitalDiscoveryResponse;
import com.shanalert.hospitalalert.model.EmergencyType;
import com.shanalert.hospitalalert.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    /**
     * Returns the sorted list of hospitals directly.
     * Spring converts the List to a JSON array automatically.
     */
    @GetMapping("/discovery")
    public List<HospitalDiscoveryResponse> discoverHospitals(
            @RequestParam EmergencyType type,
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        return hospitalService.findHospitalsForEmergency(type, lat, lng);
    }
}