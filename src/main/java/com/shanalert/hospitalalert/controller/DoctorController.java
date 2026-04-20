package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.DoctorUpdateDto; // Create a similar Record/DTO for Doctor
import com.shanalert.hospitalalert.entity.Doctor;
import com.shanalert.hospitalalert.service.DoctorService;
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
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    // --- READ OPERATIONS ---

    @GetMapping("/{userId}")
    public Doctor getById(@PathVariable UUID userId) {
        return doctorService.findById(userId);
    }

    @GetMapping
    public Page<Doctor> getAllPaged(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return doctorService.findAllPaged(pageable);
    }

    /**
     * Search doctors by hospital.
     * Uses the "/hospital/" segment to avoid ambiguity with getById.
     */
    @GetMapping("/hospital/{hospitalId}")
    public List<Doctor> getByHospital(@PathVariable UUID hospitalId) {
        return doctorService.findByHospitalId(hospitalId);
    }

    // --- WRITE OPERATIONS ---

    /**
     * LINKING: Assigns a User to a Hospital as a Doctor.
     * URI: POST /api/v1/doctors/link/user/{userId}/hospital/{hospitalId}
     */
    @PostMapping("/link/user/{userId}/hospital/{hospitalId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void linkToHospital(
            @PathVariable UUID userId,
            @PathVariable UUID hospitalId) {
        doctorService.linkUserToHospital(userId, hospitalId);
    }

    /**
     * UPDATE: Updates professional details or adds a new hospital association.
     */
    @PatchMapping("/{userId}")
    public Doctor update(
            @PathVariable UUID userId,
            @Valid @RequestBody DoctorUpdateDto updateData, // Or use a DoctorUpdateDto
            @RequestHeader(AppConstants.ACTOR_ID) UUID actorId) {
        return doctorService.updateDoctorProfile(userId, updateData, actorId);
    }

    // --- DELETE / DEACTIVATE ---

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId) {
        doctorService.removeDoctorProfile(userId);
    }

    @PostMapping("/{userId}/deactivate")
    @ResponseStatus(HttpStatus.OK)
    public void deactivate(@PathVariable UUID userId) {
        doctorService.deactivateProfile(userId);
    }
}