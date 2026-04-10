package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.BedUpdateRequest;
import com.shanalert.hospitalalert.dto.BedCreateRequest;
import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.mapper.BedMapper;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.repository.BedRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class BedService {

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private BedMapper bedMapper;



    /**
     * Partial Update (PATCH) using BedUpdateRequest.
     */
    @Transactional
    public BedResponse updateBed(BedUpdateRequest request) {
        Bed existingBed = bedRepository.findById(request.bedId())
                .orElseThrow(() -> new EntityNotFoundException("Bed not found"));

        bedMapper.toEntity(request, existingBed);
        Bed updatedBed = bedRepository.save(existingBed);

        return bedMapper.toDto(updatedBed, "Bed updated successfully.");
    }

    @Transactional
    public BedResponse reserveBedForPatient(UUID hospitalId, BedType type, UUID patientId) {
        Bed availableBed = bedRepository.findFirstByHospitalIdAndBedTypeAndStatus(
                        hospitalId, type, BedStatus.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("No " + type + " beds available at this hospital."));

        availableBed.setStatus(BedStatus.RESERVED);
        availableBed.setOccupiedByPatientId(patientId);
        Bed savedBed = bedRepository.save(availableBed);

        return bedMapper.toDto(savedBed, "Reservation successful.");
    }

    @Transactional
    public BedResponse confirmArrival(UUID bedId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new EntityNotFoundException("Bed record not found."));

        bed.setStatus(BedStatus.OCCUPIED);
        Bed savedBed = bedRepository.save(bed);

        return bedMapper.toDto(savedBed, "Patient check-in confirmed.");
    }

    @Transactional
    public BedResponse dischargePatient(UUID bedId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new EntityNotFoundException("Bed record not found."));

        bed.setStatus(BedStatus.AVAILABLE);
        bed.setOccupiedByPatientId(null);
        Bed savedBed = bedRepository.save(bed);

        return bedMapper.toDto(savedBed, "Patient discharged. Bed is now available.");
    }
}
