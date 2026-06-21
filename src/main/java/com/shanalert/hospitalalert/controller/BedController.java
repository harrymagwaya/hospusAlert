package com.shanalert.hospitalalert.controller;

import com.shanalert.hospitalalert.dto.BedCreateRequest;
import com.shanalert.hospitalalert.dto.BedCreateResponse;
import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.BedUpdateRequest;
import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.service.BedService;
import com.shanalert.hospitalalert.util.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/beds")
@RequiredArgsConstructor
public class BedController {

    private final BedService bedService;

    @PostMapping("/hospital/{hospitalId}")
    @ResponseStatus(HttpStatus.CREATED)
    public BedCreateResponse createBeds(
            @PathVariable UUID hospitalId,
            @Valid @RequestBody BedCreateRequest request
    ) {
        return bedService.createBeds(request, hospitalId);
    }

    @GetMapping("/hospital/{hospitalId}")
    public Page<BedResponse> getAllBeds(
            @PathVariable UUID hospitalId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return bedService.getAllBedsByHospital(hospitalId, pageable);
    }

    @GetMapping("/hospital/{hospitalId}/type")
    public Page<BedResponse> getBedsByType(
            @PathVariable UUID hospitalId,
            @RequestParam BedType type,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return bedService.getBedsByHospitalAndType(hospitalId, type, pageable);
    }

    @GetMapping("/available")
    public List<UUID> getHospitalsWithCapacity(
            @RequestParam BedType type
    ) {
        return bedService.getHospitalsWithAvailableCapacity(type);
    }

    @GetMapping("/hospital/{hospitalId}/count")
    public int countAvailableBeds(
            @PathVariable UUID hospitalId,
            @RequestParam BedType type
    ) {
        return bedService.countAvailableBeds(hospitalId, type);
    }

    @GetMapping("/{bedId}/number")
    public String getBedNumber(
            @PathVariable UUID bedId
    ) {
        return bedService.getBedNumberById(bedId);
    }

    @GetMapping("/hospital/{hospitalId}/bed/{bedId}")
    public Bed getBedById(
            @PathVariable UUID hospitalId,
            @PathVariable UUID bedId
    ) {
        return bedService.getBedByIdScoped(bedId, hospitalId);
    }

    @PatchMapping("/hospital/{hospitalId}/bed/{bedId}")
    public BedResponse updateHospitalBed(
            @PathVariable UUID hospitalId,
            @PathVariable UUID bedId,
            @Valid @RequestBody BedUpdateRequest request,
            @RequestHeader(AppConstants.ACTOR_ID) UUID actorId
    ) {
        BedUpdateRequest updatedRequest = new BedUpdateRequest(
                bedId,
                request.bedType(),
                request.status()
        );

        return bedService.updateBedScoped(hospitalId, bedId, updatedRequest, actorId);
    }
}