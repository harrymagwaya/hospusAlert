package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.HospitalAdminUpdateDto;
import com.shanalert.hospitalalert.entity.HospitalAdmin;
import com.shanalert.hospitalalert.service.HospitalAdminService;
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
@RequestMapping("/api/v1/hospital-admins")
@RequiredArgsConstructor
public class HospitalAdminController {

    private final HospitalAdminService hospitalAdminService;

    // GET ALL HOSPITAL ADMINS
    // This fetches users with HOSPITAL_ADMIN role and lazy-creates/syncs profiles.
    @GetMapping
    public Page<HospitalAdmin> getAllHospitalAdmins(
            @PageableDefault(
                    size = 10,
                    sort = "createdAt",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return hospitalAdminService.findAllPaged(pageable);
    }

    // GET HOSPITAL ADMIN BY USER ID
    @GetMapping("/{userId}")
    public HospitalAdmin getById(@PathVariable UUID userId) {
        return hospitalAdminService.getOrCreateProfile(userId);
    }

    // UPDATE HOSPITAL ADMIN PROFILE
    @PatchMapping("/{userId}")
    public HospitalAdmin update(
            @PathVariable UUID userId,
            @RequestBody HospitalAdminUpdateDto updateData,
            @RequestHeader(AppConstants.ACTOR_ID) UUID actorId
    ) {
        return hospitalAdminService.updateAdminProfile(userId, updateData, actorId);
    }

    // LINK HOSPITAL ADMIN USER TO HOSPITAL
    @PostMapping("/link/user/{userId}/hospital/{hospitalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void linkToHospital(
            @PathVariable UUID userId,
            @PathVariable UUID hospitalId
    ) {
        hospitalAdminService.linkUserToHospital(userId, hospitalId);
    }

    // DELETE HOSPITAL ADMIN PROFILE
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId) {
        hospitalAdminService.removeAdminProfile(userId);
    }

    // GET ADMINS BY HOSPITAL
    @GetMapping("/hospital/{hospitalId}")
    public List<HospitalAdmin> findByHospitalId(@PathVariable UUID hospitalId) {
        return hospitalAdminService.findByHospitalId(hospitalId);
    }

    // OPTIONAL: CHECK IF ADMIN BELONGS TO HOSPITAL
    @GetMapping("/{adminId}/hospital/{hospitalId}/access")
    public boolean checkHospitalAdminAccess(
            @PathVariable UUID adminId,
            @PathVariable UUID hospitalId
    ) {
        return hospitalAdminService.belongsToHospital(adminId, hospitalId);
    }

    @DeleteMapping("/{adminId}/hospital/{hospitalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlinkFromHospital(
            @PathVariable UUID adminId,
            @PathVariable UUID hospitalId
    ) {
        hospitalAdminService.unlinkAdminFromHospital(adminId, hospitalId);
    }
}