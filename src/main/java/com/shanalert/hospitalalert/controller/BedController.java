package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.BedCreateRequest;
import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.BedUpdateRequest;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.service.BedService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/beds")

public class BedController {

    @Autowired
    private BedService bedService;

    @PostMapping("/sync")
    public void syncBeds(@Valid @RequestBody BedCreateRequest request) {
        bedService.createBeds(request);
    }

    @GetMapping("/hospital/{hospitalId}")
    public Page<BedResponse> getAllBeds(
            @PathVariable UUID hospitalId,
            @PageableDefault(size = 20) Pageable pageable) {
        return bedService.getAllBedsByHospital(hospitalId, pageable);
    }

    @GetMapping("/hospital/{hospitalId}/filter")
    public Page<BedResponse> getBedsByType(
            @PathVariable UUID hospitalId,
            @RequestParam BedType type,
            @PageableDefault(size = 20) Pageable pageable) {
        return bedService.getBedsByHospitalAndType(hospitalId, type, pageable);
    }

    @PatchMapping("/update")
    public BedResponse patchBed(@Valid @RequestBody BedUpdateRequest request, UUID actorId) {
        return bedService.updateBed(request, actorId);
    }


}