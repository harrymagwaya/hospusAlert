package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.*;
import com.shanalert.hospitalalert.entity.*;
import com.shanalert.hospitalalert.mapper.BedMapper;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.model.EmergencyType;
import com.shanalert.hospitalalert.model.HospitalStatus;
import com.shanalert.hospitalalert.repository.BedAdmissionRepository;
import com.shanalert.hospitalalert.repository.BedRepository;
import com.shanalert.hospitalalert.repository.HospitalAdminRepository;
import com.shanalert.hospitalalert.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalAdminRepository hospitalAdminRepository;
    private final LocationService locationService;
    private final BedService bedService;
    private final AddressService addressService;
    private final BedAdmissionRepository admissionRepository;
    private final BedRepository bedRepository;
    private final BedMapper bedMapper;

    @Transactional(readOnly = true)
    public Page<HospitalResponse> getAllHospitals(Pageable pageable) {
        return hospitalRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public HospitalResponse findById(UUID id) {
        Hospital hospital = getById(id);
        return mapToResponse(hospital);
    }

    @Transactional(readOnly = true)
    public Hospital getById(UUID hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));
    }

    @Transactional
    public HospitalResponse addHospital(HospitalRequest request) {
        log.info("Creating new hospital: {}", request.getName());

        Address savedAddress = null;

        if (request.getAddress() != null) {
            savedAddress = addressService.saveAddress(request.getAddress());
        }

        Hospital hospital = Hospital.builder()
                .name(request.getName())
                .licenseNumber(request.getLicenseNumber())
                .address(savedAddress)
                .ownershipType(request.getOwnershipType())
                .facilityLevel(request.getFacilityLevel())
                .isEmergencyReady(
                        request.getIsEmergencyReady() != null
                                ? request.getIsEmergencyReady()
                                : true
                )
                .status(HospitalStatus.ACTIVE)
                .build();

        Hospital savedHospital = hospitalRepository.save(hospital);

        return mapToResponse(savedHospital);
    }

    @Transactional
    public HospitalResponse patchHospital(UUID id, HospitalRequest request) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        if (request.getName() != null) {
            hospital.setName(request.getName());
        }

        if (request.getLicenseNumber() != null) {
            hospital.setLicenseNumber(request.getLicenseNumber());
        }

        if (request.getOwnershipType() != null) {
            hospital.setOwnershipType(request.getOwnershipType());
        }

        if (request.getFacilityLevel() != null) {
            hospital.setFacilityLevel(request.getFacilityLevel());
        }

        if (request.getIsEmergencyReady() != null) {
            hospital.setIsEmergencyReady(request.getIsEmergencyReady());
        }

        if (request.getStatus() != null) {
            hospital.setStatus(request.getStatus());
        }

        if (request.getAddress() != null) {
            if (hospital.getAddress() == null) {
                Address savedAddress = addressService.saveAddress(request.getAddress());
                hospital.setAddress(savedAddress);
            } else {
                addressService.updateAddress(hospital.getAddress().getId(), request.getAddress());
            }
        }

        return mapToResponse(hospitalRepository.save(hospital));
    }

    @Transactional
    public void deleteHospital(UUID hospitalId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        List<HospitalAdmin> linkedAdmins = hospitalAdminRepository.findByHospitalId(hospitalId);

        linkedAdmins.forEach(admin -> {
            admin.setHospital(null);
            hospitalAdminRepository.save(admin);
        });

        hospitalRepository.delete(hospital);

        log.info("Deleted hospital {}", hospitalId);
    }

    @Transactional(readOnly = true)
    public List<HospitalDiscoveryResponse> findHospitalsForEmergency(
            EmergencyType type,
            Double patientLat,
            Double patientLng
    ) {
        BedType neededBed = type.getRequiredBedType();

        List<UUID> capableHospitalIds = bedService.getHospitalsWithAvailableCapacity(neededBed);

        return hospitalRepository.findAllById(capableHospitalIds)
                .stream()
                .filter(hospital -> hospital.getStatus() == HospitalStatus.ACTIVE)
                .filter(hospital -> Boolean.TRUE.equals(hospital.getIsEmergencyReady()))
                .map(hospital -> mapToDiscoveryResponse(hospital, neededBed, patientLat, patientLng))
                .sorted(
                        Comparator.comparing(
                                HospitalDiscoveryResponse::estimatedMinutes,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                )
                .toList();
    }

    private HospitalDiscoveryResponse mapToDiscoveryResponse(
            Hospital hospital,
            BedType neededBed,
            Double patientLat,
            Double patientLng
    ) {
        Integer eta = null;
        Double lat = null;
        Double lng = null;
        String street = "Address not listed";

        if (hospital.getAddress() != null) {
            lat = hospital.getAddress().getLatitude();
            lng = hospital.getAddress().getLongitude();

            if (hospital.getAddress().getStreet() != null) {
                street = hospital.getAddress().getStreet();
            }
        }

        if (patientLat != null
                && patientLng != null
                && lat != null
                && lng != null) {
            RouteEstimateDTO routeEstimate = locationService.getRouteEstimate(
                    patientLat,
                    patientLng,
                    lat,
                    lng
            );

            eta = routeEstimate.getEstimatedMinutes();
        }

        int availableCount = bedService.countAvailableBeds(hospital.getId(), neededBed);

        return new HospitalDiscoveryResponse(
                hospital.getId(),
                hospital.getName(),
                street,
                eta,
                availableCount,
                lat,
                lng
        );
    }

    @Transactional(readOnly = true)
    public List<BedOccupancyResponse> getBedOccupancy(UUID hospitalId) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new EntityNotFoundException("Hospital not found");
        }

        List<Bed> beds = bedRepository.findAllByHospital_Id(hospitalId);

        List<UUID> bedIds = beds.stream()
                .map(Bed::getId)
                .toList();

        if (bedIds.isEmpty()) {
            return List.of();
        }

        List<BedAdmission> admissions = admissionRepository.findActiveAdmissionsForBeds(bedIds);

        Map<UUID, BedAdmission> admissionMap = admissions.stream()
                .collect(Collectors.toMap(
                        admission -> admission.getBed().getId(),
                        admission -> admission,
                        (existing, duplicate) -> existing
                ));

        return beds.stream()
                .map(bed -> {
                    BedAdmission admission = admissionMap.get(bed.getId());

                    UUID patientId = null;
                    String patientName = null;
                    String admissionStatus = null;

                    if (admission != null) {
                        patientId = admission.getPatientId();
                        admissionStatus = admission.getStatus().name();
                    }

                    return new BedOccupancyResponse(
                            bed.getId(),
                            bed.getBedNumber(),
                            bed.getBedType().name(),
                            bed.getStatus().name(),
                            patientId,
                            patientName,
                            admissionStatus
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HospitalPatientResponse> getPatientsInHospital(UUID hospitalId) {
        if (!hospitalRepository.existsById(hospitalId)) {
            throw new EntityNotFoundException("Hospital not found");
        }

        List<BedAdmission> admissions =
                admissionRepository.findActiveAdmissionsByHospital(hospitalId);

        return admissions.stream()
                .map(admission -> {
                    Bed bed = admission.getBed();

                    return new HospitalPatientResponse(
                            admission.getPatientId(),
                            admission.getId(),
                            bed != null ? bed.getBedNumber() : "Unassigned",
                            bed != null ? bed.getBedType().name() : null,
                            admission.getStatus().name(),
                            admission.getAdmittedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void removeBedFromHospital(UUID hospitalId, UUID bedId) {
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new EntityNotFoundException("Bed not found"));

        if (bed.getHospital() == null || !bed.getHospital().getId().equals(hospitalId)) {
            throw new IllegalStateException("Bed does not belong to this hospital");
        }

        if (bed.getStatus() == com.shanalert.hospitalalert.model.BedStatus.OCCUPIED
                || bed.getStatus() == com.shanalert.hospitalalert.model.BedStatus.RESERVED) {
            throw new IllegalStateException("Cannot remove a bed that is currently occupied or reserved");
        }

        hospital.getBeds().removeIf(existingBed -> existingBed.getId().equals(bedId));
        bedRepository.delete(bed);

        log.info("Bed {} removed from Hospital {}", bedId, hospitalId);
    }

    private HospitalResponse mapToResponse(Hospital hospital) {
        return HospitalResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .licenseNumber(hospital.getLicenseNumber())
                .status(hospital.getStatus())
                .ownershipType(hospital.getOwnershipType())
                .facilityLevel(hospital.getFacilityLevel())
                .beds(hospital.getBeds() != null
                        ? hospital.getBeds().stream()
                        .map(bed -> bedMapper.toDto(bed, "Mapped from Hospital"))
                        .toList()
                        : List.of())
                .city(hospital.getAddress() != null ? hospital.getAddress().getCity() : "Unknown")
                .isEmergencyReady(hospital.getIsEmergencyReady())
                .build();
    }
}