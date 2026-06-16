package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.BedCreateRequest;
import com.shanalert.hospitalalert.dto.BedCreateResponse;
import com.shanalert.hospitalalert.dto.BedResponse;
import com.shanalert.hospitalalert.dto.BedUpdateRequest;
import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.mapper.BedMapper;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.repository.BedRepository;
import com.shanalert.hospitalalert.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedService {

    private final BedRepository bedRepository;
    private final BedMapper bedMapper;
    private final HospitalRepository hospitalRepository;

    @Transactional
    public BedCreateResponse createBeds(BedCreateRequest request, UUID hospitalId) {

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        validateBedTypeAllowedForFacility(hospital, request.bedType());

        List<Bed> existingBeds = bedRepository.findByHospital_IdAndBedType(
                hospital.getId(),
                request.bedType()
        );

        int currentCount = existingBeds.size();

        if (request.totalCount() <= currentCount) {
            log.warn(
                    "Requested count ({}) is not greater than current count ({}). No beds created.",
                    request.totalCount(),
                    currentCount
            );

            return new BedCreateResponse(
                    hospitalId,
                    request.bedType(),
                    request.totalCount(),
                    currentCount,
                    0,
                    currentCount,
                    "No beds created. Requested total is not greater than current count."
            );
        }

        int amountToAdd = request.totalCount() - currentCount;

        log.info(
                "Adding {} new {} beds to hospital {}",
                amountToAdd,
                request.bedType(),
                hospitalId
        );

        for (int i = 0; i < amountToAdd; i++) {
            Bed bed = new Bed();
            bed.setHospital(hospital);
            bed.setBedType(request.bedType());
            bed.setStatus(BedStatus.AVAILABLE);

            String generatedNumber = generateBedNumber(
                    request.bedType(),
                    currentCount + i + 1
            );

            bed.setBedNumber(generatedNumber);

            bedRepository.save(bed);
        }

        int finalCount = currentCount + amountToAdd;

        return new BedCreateResponse(
                hospitalId,
                request.bedType(),
                request.totalCount(),
                currentCount,
                amountToAdd,
                finalCount,
                amountToAdd + " " + request.bedType() + " beds created successfully."
        );
    }

    @Transactional(readOnly = true)
    public Page<BedResponse> getBedsByHospitalAndType(
            UUID hospitalId,
            BedType type,
            Pageable pageable
    ) {
        Page<Bed> bedPage = bedRepository.findByHospital_IdAndBedType(
                hospitalId,
                type,
                pageable
        );

        return bedPage.map(bed -> bedMapper.toDto(bed, "Bed details retrieved."));
    }

    @Transactional(readOnly = true)
    public Page<BedResponse> getAllBedsByHospital(UUID hospitalId, Pageable pageable) {
        log.info("Fetching all beds for hospital: {}", hospitalId);

        return bedRepository.findAllByHospital_Id(hospitalId, pageable)
                .map(bed -> bedMapper.toDto(bed, "Hospital-wide bed list retrieved."));
    }

    @Transactional
    public BedResponse updateBed(UUID hospitalId, BedUpdateRequest request, UUID actorId) {

        if (!hospitalRepository.existsById(hospitalId)) {
            throw new EntityNotFoundException("Hospital not found");
        }

        Bed bed = bedRepository.findById(request.bedId())
                .orElseThrow(() -> new EntityNotFoundException("Bed not found"));

        if (bed.getHospital() == null || !bed.getHospital().getId().equals(hospitalId)) {
            log.error(
                    "Unauthorized bed update attempt. Actor {} tried to update Bed {} under Hospital {}",
                    actorId,
                    request.bedId(),
                    hospitalId
            );

            throw new IllegalStateException("Bed does not belong to this hospital");
        }

        if (request.bedType() != null) {
            validateBedTypeAllowedForFacility(bed.getHospital(), request.bedType());
            bed.setBedType(request.bedType());
        }

        if (request.status() != null) {
            bed.setStatus(request.status());
        }

        Bed updated = bedRepository.save(bed);

        log.info("Bed {} updated by {} in hospital {}", bed.getId(), actorId, hospitalId);

        return bedMapper.toDto(updated, "Bed updated successfully");
    }

    @Transactional
    public BedResponse updateBedScoped(
            UUID hospitalId,
            UUID bedId,
            BedUpdateRequest request,
            UUID actorId
    ) {
        Bed bed = getBedByIdScoped(bedId, hospitalId);

        if (request.bedId() != null && !request.bedId().equals(bedId)) {
            throw new IllegalStateException("Request bed ID does not match path bed ID");
        }

        if (request.bedType() != null) {
            validateBedTypeAllowedForFacility(bed.getHospital(), request.bedType());
            bed.setBedType(request.bedType());
        }

        if (request.status() != null) {
            bed.setStatus(request.status());
        }

        Bed updatedBed = bedRepository.save(bed);

        log.info("Bed {} updated by Admin {} in hospital {}", bedId, actorId, hospitalId);

        return bedMapper.toDto(updatedBed, "Bed updated successfully.");
    }

    @Transactional(readOnly = true)
    public Bed getBedById(UUID bedId) {
        return bedRepository.findById(bedId)
                .orElseThrow(() -> new EntityNotFoundException("Bed with ID " + bedId + " not found"));
    }

    @Transactional(readOnly = true)
    public Bed getBedByIdScoped(UUID bedId, UUID hospitalId) {
        Bed bed = getBedById(bedId);

        if (bed.getHospital() == null || !bed.getHospital().getId().equals(hospitalId)) {
            log.error(
                    "Security Alert: Attempt to access Bed {} from wrong Hospital {}",
                    bedId,
                    hospitalId
            );

            throw new IllegalStateException("Bed does not belong to the specified hospital");
        }

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

    @Transactional(readOnly = true)
    public String getBedNumberById(UUID bedId) {
        return bedRepository.findById(bedId)
                .map(Bed::getBedNumber)
                .orElse("Unknown Bed");
    }

    @Transactional(readOnly = true)
    public int countAvailableBeds(UUID hospitalId, BedType neededBedType) {
        return bedRepository.countAvailableBeds(hospitalId, neededBedType);
    }

    private String generateBedNumber(BedType bedType, int number) {
        return bedType.name() + "-" + number;
    }

    private void validateBedTypeAllowedForFacility(Hospital hospital, BedType bedType) {
        if (hospital.getFacilityLevel() == null) {
            throw new IllegalStateException("Hospital facility level is not set");
        }

        boolean allowed = switch (hospital.getFacilityLevel()) {
            case HC_II -> bedType == BedType.GENERAL;

            case HC_III -> bedType == BedType.GENERAL
                    || bedType == BedType.MATERNITY;

            case HC_IV -> bedType == BedType.GENERAL
                    || bedType == BedType.MATERNITY
                    || bedType == BedType.EMERGENCY;

            case GENERAL_HOSPITAL -> bedType == BedType.GENERAL
                    || bedType == BedType.MATERNITY
                    || bedType == BedType.EMERGENCY
                    || bedType == BedType.SURGERY
                    || bedType == BedType.ICU;

            case REGIONAL_REFERRAL_HOSPITAL -> bedType == BedType.GENERAL
                    || bedType == BedType.MATERNITY
                    || bedType == BedType.EMERGENCY
                    || bedType == BedType.SURGERY
                    || bedType == BedType.ICU
                    || bedType == BedType.PEDIATRIC;

            case NATIONAL_REFERRAL_HOSPITAL -> true;

            case SPECIALIZED_HOSPITAL -> bedType == BedType.ICU
                    || bedType == BedType.SPECIALIZED
                    || bedType == BedType.SURGERY
                    || bedType == BedType.EMERGENCY;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Bed type " + bedType + " is not allowed for facility level " + hospital.getFacilityLevel()
            );
        }
    }
}