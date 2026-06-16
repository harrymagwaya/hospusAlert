package com.shanalert.hospitalalert.controller;


import com.shanalert.hospitalalert.entity.BedAdmission;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.service.BedAdmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bed-admissions")
@RequiredArgsConstructor
public class BedAdmissionController {

    private final BedAdmissionService admissionService;

        @PostMapping("/reserve")

        public BedAdmission reserve(
                @RequestParam UUID hospitalId,
                @RequestParam BedType type,
                @RequestParam UUID patientId) {
            return admissionService.reserveBed(hospitalId, type, patientId);
        }

        @PatchMapping("/{id}/arrive")
        public BedAdmission markArrived(
                @PathVariable UUID id,
                @AuthenticationPrincipal UUID actorId) {
            return admissionService.markAsArrived(id, actorId);
        }

        @PatchMapping("/{id}/admit")
        public BedAdmission confirmAdmit(
                @PathVariable UUID id,
                @AuthenticationPrincipal UUID actorId) {
            return admissionService.confirmArrival(id, actorId);
        }

        @PatchMapping("/{id}/discharge")
        public BedAdmission discharge(
                @PathVariable UUID id,
                @AuthenticationPrincipal UUID actorId) {
            return admissionService.discharge(id, actorId);
        }
    }

