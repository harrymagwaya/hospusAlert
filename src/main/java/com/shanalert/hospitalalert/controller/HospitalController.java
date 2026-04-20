package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.*;
import com.shanalert.hospitalalert.model.EmergencyType;
import com.shanalert.hospitalalert.service.HospitalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @PostMapping
    public HospitalResponse addHospital(@Valid @RequestBody HospitalRequest request) {
        return hospitalService.addHospital(request);
    }

    @PatchMapping("/{id}")
    public HospitalResponse updateHospital(
            @PathVariable UUID id,
            @RequestBody HospitalRequest request) {
        return hospitalService.patchHospital(id, request);
    }

    @GetMapping
    public Page<HospitalResponse> getAllHospitalsPage(
            @PageableDefault(page = 0, size = 10, sort = "name", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return hospitalService.getAllHospitals(pageable);
    }

    @GetMapping("/discovery")
    public List<HospitalDiscoveryResponse> discoverHospitals(
            @RequestParam EmergencyType type,
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        return hospitalService.findHospitalsForEmergency(type, lat, lng);
    }

    // 1. Patients in hospital (clean clinical view)
    @GetMapping("/{hospitalId}/patients")
    public List<HospitalPatientResponse> getPatientsInHospital(
            @PathVariable UUID hospitalId
    ) {
        return hospitalService.getPatientsInHospital(hospitalId);
    }

    // 2. Bed occupancy map (VERY IMPORTANT UI FEATURE)
    @GetMapping("/{hospitalId}/beds/occupancy")
    public List<BedOccupancyResponse> getBedOccupancy(
            @PathVariable UUID hospitalId
    ) {
        return hospitalService.getBedOccupancy(hospitalId);
    }
}