package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.BedUpdateRequest;
import com.shanalert.hospitalalert.dto.BedCreateRequest;
import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.mapper.BedMapper;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.repository.BedRepository;
import com.shanalert.hospitalalert.repository.HospitalRepository;
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

    @Autowired
    private HospitalRepository hospitalRepository;

    @Transactional
    public void createBeds(BedCreateRequest request, UUID hospitalId) {

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        // 1. Check how many beds of this type already exist for this hospital
        List<Bed> existingBeds = bedRepository.findByHospitalIdAndBedType(
                hospital.getId(),
                request.bedType()
        );
        int currentCount = existingBeds.size();

        // 2. We only add beds if the totalCount requested is greater than what we have
        if (request.totalCount() > currentCount) {
            int amountToAdd = request.totalCount() - currentCount;

            log.info("Adding {} new {} beds to hospital {}", amountToAdd, request.bedType(), hospitalId);

            for (int i = 0; i < amountToAdd; i++) {
                Bed bed = new Bed();
                bed.setHospital(hospital);
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
    public BedResponse updateBed(UUID hospitalId, BedUpdateRequest request, UUID actorId) {

        // 1. Validate hospital exists (IMPORTANT)
        // Ideally via hospitalRepository.existsById(...)
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new EntityNotFoundException("Hospital not found");
        }

        // 2. Fetch bed
        Bed bed = bedRepository.findById(request.bedId())
                .orElseThrow(() -> new EntityNotFoundException("Bed not found"));

        // 3. SECURITY CHECK: Ensure bed belongs to hospital
        if (!bed.getHospital().getId().equals(hospitalId)) {
            throw new IllegalStateException("Bed does not belong to this hospital");
        }

        // 4. PATCH logic (only update non-null fields)
        if (request.bedType() != null) {
            bed.setBedType(request.bedType());
        }

        if (request.status() != null) {
            bed.setStatus(request.status());
        }

        // 5. Save
        Bed updated = bedRepository.save(bed);

        log.info("Bed {} updated by {} in hospital {}", bed.getId(), actorId, hospitalId);

        return bedMapper.toDto(updated, "Bed updated successfully");
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

    public int countAvailableBeds(UUID hospitalId, BedType neededBed){
        return bedRepository.countAvailableBeds(hospitalId, neededBed);
    }


    /**
     * Fetches a bed only if it belongs to the specified hospital.
     * This prevents cross-hospital data leakage.
     */
    @Transactional(readOnly = true)
    public Bed getBedByIdScoped(UUID bedId, UUID hospitalId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new EntityNotFoundException("Bed with ID " + bedId + " not found"));

        // Validation moved to service layer
        if (!bed.getHospital().getId().equals(hospitalId)) {
            log.error("Security Alert: Attempt to access Bed {} from wrong Hospital {}", bedId, hospitalId);
            throw new IllegalStateException("Bed does not belong to the specified hospital");
        }

        return bed;
    }

    @Transactional
    public BedResponse updateBedScoped(UUID hospitalId, UUID bedId, BedUpdateRequest request, UUID actorId) {
        // 1. Fetch the bed
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new EntityNotFoundException("Bed not found"));

        // 2. Security Check: Does this bed belong to THIS hospital?
        if (!bed.getHospital().getId().equals(hospitalId)) {
            log.error("Unauthorized edit attempt! Actor {} tried to edit Bed {} under wrong Hospital {}",
                    actorId, bedId, hospitalId);
            throw new IllegalStateException("This bed does not belong to the specified hospital.");
        }

        // 3. Map updates from DTO to Entity
        // Using your existing mapper logic
        bedMapper.toEntity(request, bed);

        // 4. Save and return
        Bed updatedBed = bedRepository.save(bed);
        log.info("Bed {} updated by Admin {}", bedId, actorId);

        return bedMapper.toDto(updatedBed, "Bed updated successfully.");
    }

}
