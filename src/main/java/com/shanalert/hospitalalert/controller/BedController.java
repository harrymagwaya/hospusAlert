package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.BedCreateRequest;
import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.BedUpdateRequest;
import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.service.BedService;
import com.shanalert.hospitalalert.util.AppConstants;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/beds")
public class BedController {

    @Autowired
    private BedService bedService;

    @PostMapping
    public void createBeds(
            @PathVariable UUID hospitalId,
            @RequestBody BedCreateRequest request
    ) {
        // enforce hospital consistency
        BedCreateRequest updatedRequest = new BedCreateRequest(
                request.bedType(),
                request.totalCount()
        );

        bedService.createBeds(updatedRequest, hospitalId);
    }

    @GetMapping("/hospital/{hospitalId}")
    public Page<BedResponse> getAllBeds(
            @PathVariable UUID hospitalId,
            @PageableDefault(size = 20) Pageable pageable) {
        return bedService.getAllBedsByHospital(hospitalId, pageable);
    }

    @GetMapping("/hospital/{hospitalId}/type")
    public Page<BedResponse> getBedsByType(
            @PathVariable UUID hospitalId,
            @RequestParam BedType type,
            @PageableDefault(size = 20) Pageable pageable) {
        return bedService.getBedsByHospitalAndType(hospitalId, type, pageable);
    }


    @GetMapping("/available")
    public List<UUID> getHospitalsWithCapacity(
            @RequestParam BedType type
    ) {
        return bedService.getHospitalsWithAvailableCapacity(type);
    }

    @GetMapping("/count")
    public int countAvailableBeds(
            @PathVariable UUID hospitalId,
            @RequestParam BedType type
    ) {
        return bedService.countAvailableBeds(hospitalId, type);
    }

    @GetMapping("/{bedId}/number")
    public String getBedNumber(
            @PathVariable UUID hospitalId,
            @PathVariable UUID bedId
    ) {
        return bedService.getBedNumberById(bedId);
    }

    @PatchMapping("/hospitals/{hospitalId}/beds/{bedId}")
    public BedResponse updateBed(
            @PathVariable UUID hospitalId,
            @PathVariable UUID bedId,
            @RequestBody BedUpdateRequest request,
            @RequestHeader(AppConstants.ACTOR_ID) UUID actorId
    ) {
        BedUpdateRequest updatedRequest = new BedUpdateRequest(
                bedId,
                request.bedType(),
                request.status()
        );

        return bedService.updateBed(hospitalId, updatedRequest, actorId);
    }

    @GetMapping("/{bedId}")
    public Bed getBedById(
            @PathVariable UUID hospitalId,
            @PathVariable UUID bedId
    ) {
        // Controller just delegates and returns
        return bedService.getBedByIdScoped(bedId, hospitalId);
    }

    @PatchMapping("/{bedId}")
    public BedResponse updateHospitalBed(
            @PathVariable UUID hospitalId,
            @PathVariable UUID bedId,
            @Valid @RequestBody BedUpdateRequest request,
            @RequestHeader(AppConstants.ACTOR_ID) UUID actorId) {

        return bedService.updateBedScoped(hospitalId, bedId, request, actorId);
    }


}