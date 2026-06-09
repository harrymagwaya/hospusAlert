package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.DoctorUpdateDto;
import com.shanalert.hospitalalert.entity.Doctor;
import com.shanalert.hospitalalert.service.DoctorService;
import com.shanalert.hospitalalert.util.AppConstants;
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

    // GET ALL DOCTORS
    // This fetches users with role DOCTOR and lazy-creates/syncs Doctor profiles.
    @GetMapping
    public Page<Doctor> getAllDoctors(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return doctorService.findAllPaged(pageable);
    }

    // GET DOCTOR BY USER ID
    // This will lazy-create the Doctor profile if the User has role DOCTOR.
    @GetMapping("/{userId}")
    public Doctor getDoctorById(@PathVariable UUID userId) {
        return doctorService.findById(userId);
    }

    // UPDATE DOCTOR PROFILE
    @PatchMapping("/{userId}")
    public Doctor updateDoctor(
            @PathVariable UUID userId,
            @RequestBody DoctorUpdateDto updateData,
            @RequestHeader(AppConstants.ACTOR_ID) UUID actorId
    ) {
        return doctorService.updateDoctorProfile(userId, updateData, actorId);
    }

    // LINK DOCTOR USER TO HOSPITAL
    @PostMapping("/link/user/{userId}/hospital/{hospitalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkDoctorToHospital(
            @PathVariable UUID userId,
            @PathVariable UUID hospitalId
    ) {
        doctorService.linkUserToHospital(userId, hospitalId);
    }

    // UNLINK DOCTOR FROM HOSPITAL
    @DeleteMapping("/{doctorId}/hospital/{hospitalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkDoctorFromHospital(
            @PathVariable UUID doctorId,
            @PathVariable UUID hospitalId
    ) {
        doctorService.unlinkDoctorFromHospital(doctorId, hospitalId);
    }

    // DELETE DOCTOR PROFILE
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDoctor(@PathVariable UUID userId) {
        doctorService.removeDoctorProfile(userId);
    }

    // GET DOCTORS BY HOSPITAL
    @GetMapping("/hospital/{hospitalId}")
    public List<Doctor> getDoctorsByHospital(@PathVariable UUID hospitalId) {
        return doctorService.findByHospitalId(hospitalId);
    }

    // OPTIONAL: CHECK IF DOCTOR BELONGS TO HOSPITAL
    @GetMapping("/{doctorId}/hospital/{hospitalId}/access")
    public boolean checkDoctorHospitalAccess(
            @PathVariable UUID doctorId,
            @PathVariable UUID hospitalId
    ) {
        return doctorService.belongsToHospital(doctorId, hospitalId);
    }
}