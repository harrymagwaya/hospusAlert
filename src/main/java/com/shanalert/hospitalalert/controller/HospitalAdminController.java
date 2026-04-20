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

    // READ ONE
    @GetMapping("/{userId}")
    public HospitalAdmin getById(@PathVariable UUID userId) {
        return hospitalAdminService.findById(userId);
    }

    @GetMapping // Specific path for paginated results
    public Page<HospitalAdmin> getAllPaged(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return hospitalAdminService.findAllPaged(pageable);
    }

    // PATCH (Update data or re-link to a different hospital)
    @PatchMapping("/{userId}")
    public HospitalAdmin update(
            @PathVariable UUID userId,
            @RequestBody HospitalAdminUpdateDto updateData, @RequestHeader(AppConstants.ACTOR_ID) UUID actorId) {
        return hospitalAdminService.updateAdminProfile(userId, updateData, actorId);
    }

    @PostMapping("/link/user/{userId}/hospital/{hospitalId}")
    public void linkToHospital(
            @PathVariable UUID userId,
            @PathVariable UUID hospitalId) {
        hospitalAdminService.linkUserToHospital(userId, hospitalId);
    }

    // DELETE
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId) {
        hospitalAdminService.removeAdminProfile(userId);
    }

    @GetMapping("/{hospitalId}/admin")
    public List<HospitalAdmin> findByHospitalId(@PathVariable UUID hospitalId){
        return hospitalAdminService.findByHospitalId(hospitalId);
    }
}