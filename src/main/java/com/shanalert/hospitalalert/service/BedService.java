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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class BedService {

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private BedMapper bedMapper;

    @Transactional
    public void createBeds(BedCreateRequest request) {
        // 1. Check how many beds of this type already exist for this hospital
        List<Bed> existingBeds = bedRepository.findByHospitalIdAndBedType(
                request.hospitalId(),
                request.bedType()
        );
        int currentCount = existingBeds.size();

        // 2. We only add beds if the totalCount requested is greater than what we have
        if (request.totalCount() > currentCount) {
            int amountToAdd = request.totalCount() - currentCount;

            log.info("Adding {} new {} beds to hospital {}", amountToAdd, request.bedType(), request.hospitalId());

            for (int i = 0; i < amountToAdd; i++) {
                Bed bed = new Bed();
                bed.setHospitalId(request.hospitalId());
                bed.setBedType(request.bedType());
                bed.setStatus(BedStatus.AVAILABLE);

                // 3. Generate a readable bed number (e.g., "SURGERY-11")
                String generatedNumber = request.bedType().name() + "-" + (currentCount + i + 1);
                bed.setBedNumber(generatedNumber);

                bedRepository.save(bed);
            }
        } else {
            log.warn("Requested count ({}) is not greater than current count ({}). No beds created.",
                    request.totalCount(), currentCount);
        }
    }

    public Page<BedResponse> getBedsByHospitalAndType(UUID hospitalId, BedType type, Pageable pageable) {
        Page<Bed> bedPage = bedRepository.findByHospitalIdAndBedType(hospitalId, type, pageable);
        return bedPage.map(bed -> bedMapper.toDto(bed, "Bed details retrieved."));
    }

    @Transactional(readOnly = true)
    public Page<BedResponse> getAllBedsByHospital(UUID hospitalId, Pageable pageable) {
        log.info("Fetching all beds for hospital: {}", hospitalId);

        return bedRepository.findAllByHospitalId(hospitalId, pageable)
                .map(bed -> bedMapper.toDto(bed, "Hospital-wide bed list retrieved."));
    }

    @Transactional
    public BedResponse updateBed(BedUpdateRequest request, UUID actorId) {

        Bed existingBed = bedRepository.findById(request.bedId())
                .orElseThrow(() -> new EntityNotFoundException("Bed not found"));

        bedMapper.toEntity(request, existingBed);
        Bed updatedBed = bedRepository.save(existingBed);
        log.info("Updated bed successfully by {}", actorId);

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

    public Bed getBedById(UUID bedId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new EntityNotFoundException("Bed with ID " + bedId + " not found"));

        // Convert entity to DTO using your mapper
        return bed;
    }

    @Transactional(readOnly = true)
    public List<UUID> getHospitalsWithAvailableCapacity(BedType neededBedType) {
        log.info("Querying hospitals with available {} beds", neededBedType);

        List<UUID> hospitalIds = bedRepository.findHospitalIdsWithAvailableBeds(
                neededBedType,
                BedStatus.AVAILABLE
        );

        if (hospitalIds.isEmpty()) {
            log.warn("Zero hospitals found with available {} capacity", neededBedType);
        }

        return hospitalIds;
    }

    public String getBedNumberById(UUID bedId) {
        return bedRepository.findById(bedId)
                .map(Bed::getBedNumber) // Or whatever your field name is
                .orElse("Unknown Bed");
    }

    /**
     * Internal method called by EmergencyAlertService to finalize a reservation
     * after the patient selects a hospital from the narrowed-down list.
     */
    @Transactional
    public BedResponse reserveBedForEmergency(UUID hospitalId, BedType type, UUID patientId) {
        log.info("Attempting to lock a {} bed at hospital {} for patient {}", type, hospitalId, patientId);

        // Using the 'findFirst' method we created earlier to get the next available slot
        Bed availableBed = bedRepository.findFirstByHospitalIdAndBedTypeAndStatus(
                        hospitalId, type, BedStatus.AVAILABLE)
                .orElseThrow(() -> new RuntimeException("Sorry, the last available " + type + " bed was just taken."));

        // Update the state
        availableBed.setStatus(BedStatus.RESERVED);
        availableBed.setOccupiedByPatientId(patientId);

        Bed savedBed = bedRepository.save(availableBed);

        return bedMapper.toDto(savedBed, "Bed successfully reserved for incoming emergency.");
    }
}
