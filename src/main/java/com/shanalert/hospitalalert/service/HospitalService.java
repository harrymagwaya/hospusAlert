package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.*;
import com.shanalert.hospitalalert.entity.*;
import com.shanalert.hospitalalert.mapper.BedMapper;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.model.EmergencyType;
import com.shanalert.hospitalalert.model.HospitalStatus;
import com.shanalert.hospitalalert.repository.BedAdmissionRepository;
import com.shanalert.hospitalalert.repository.BedRepository;
import com.shanalert.hospitalalert.repository.HospitalAdminRepository;
import com.shanalert.hospitalalert.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private HospitalAdminRepository hospitalAdminRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private BedService bedService;

    @Autowired
    private AddressService addressService;

    @Autowired
    private BedAdmissionRepository admissionRepository;

    @Autowired
    private BedRepository bedRepository;

    @Autowired
    private BedMapper bedMapper;



    @Transactional(readOnly = true)
    public Page<HospitalResponse> getAllHospitals(Pageable pageable) {
        return hospitalRepository.findAll(pageable)
                .map(this::mapToResponse);
    }


    @Transactional(readOnly = true)
    public HospitalResponse findById(UUID id) {
        Hospital hospital = getById(id); // Uses your existing getById entity method
        return mapToResponse(hospital);
    }


    @Transactional
    public void deleteHospital(UUID hospitalId) {
        // 1. Check existence
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        // 2. Clear Admin Links
        // We use the new repository method here
        List<HospitalAdmin> linkedAdmins = hospitalAdminRepository.findByHospitalId(hospitalId);

        linkedAdmins.forEach(admin -> {
            admin.setHospital(null); // Remove the link
            hospitalAdminRepository.save(admin);
        });

        // 3. Now delete the hospital
        hospitalRepository.delete(hospital);
    }


    @Transactional
    public HospitalResponse addHospital(HospitalRequest request) {
        log.info("Creating new hospital: {}", request.getName());

        // 1. Save the Address using the AddressService
        Address savedAddress = addressService.saveAddress(request.getAddress());

        // 2. Map Request to Entity using Builder
        Hospital hospital = Hospital.builder()
                .name(request.getName())
                .licenseNumber(request.getLicenseNumber())
                .address(savedAddress)
                .icuBedsAvailable(request.getIcuBedsAvailable())
                .isEmergencyReady(request.getIsEmergencyReady())
                .status(HospitalStatus.ACTIVE)
                .build();

        // 3. Save Hospital
        Hospital savedHospital = hospitalRepository.save(hospital);

        // 4. Return the Response DTO
        return mapToResponse(savedHospital);
    }

    @Transactional
    public HospitalResponse patchHospital(UUID id, HospitalRequest request) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));

        if (request.getName() != null) hospital.setName(request.getName());
        if (request.getIcuBedsAvailable() != null) hospital.setIcuBedsAvailable(request.getIcuBedsAvailable());
        if (request.getIsEmergencyReady() != null) hospital.setIsEmergencyReady(request.getIsEmergencyReady());

        // If the request contains address updates, delegate to AddressService
        if (request.getAddress() != null) {
            addressService.updateAddress(hospital.getAddress().getId(), request.getAddress());
        }

        return mapToResponse(hospitalRepository.save(hospital));
    }


    public Hospital getById(UUID hospitalId){
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new EntityNotFoundException("Hospital not found"));
        return hospital;
    }



    @Transactional(readOnly = true)
    public List<HospitalDiscoveryResponse> findHospitalsForEmergency(EmergencyType type, Double patientLat, Double patientLng) {
        BedType neededBed = type.getRequiredBedType();

        // 1. Get all hospitals that have available beds of this type
        // We use a custom query in BedRepository for this
        List<UUID> capableHospitalIds = bedService.getHospitalsWithAvailableCapacity(neededBed);

        // 2. Fetch those hospitals and calculate distance/ETA
        return hospitalRepository.findAllById(capableHospitalIds).stream()
                .map(hospital -> {
                    Integer eta = null;
                    Double lat = null;
                    Double lng = null;
                    String street = "Address not listed";

                    // NULL SAFETY: Check if address and coordinates exist
                    if (hospital.getAddress() != null &&
                            hospital.getAddress().getLatitude() != null &&
                            hospital.getAddress().getLongitude() != null) {

                        lat = hospital.getAddress().getLatitude();
                        lng = hospital.getAddress().getLongitude();
                        street = hospital.getAddress().getStreet();

                        // Call Location Service (OSRM) inside a try-catch
                        // so one bad coordinate doesn't crash the whole list
                        try {
                            eta = locationService.getEstimatedMinutes(patientLat, patientLng, lat, lng);
                        } catch (Exception e) {
                            log.error("Failed to calculate ETA for hospital {}: {}", hospital.getName(), e.getMessage());
                        }
                    }
                    int availableCount = bedService.countAvailableBeds(hospital.getId(), neededBed);
                    return new HospitalDiscoveryResponse(hospital.getId(), hospital.getName(), street, eta, availableCount, lat,
                           lng);
                })
                .sorted(Comparator.comparing(HospitalDiscoveryResponse::estimatedMinutes,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }


    @Transactional(readOnly = true)
    public List<BedOccupancyResponse> getBedOccupancy(UUID hospitalId) {

        // 1. Get all beds in hospital
        List<Bed> beds = bedRepository.findAllByHospitalId(hospitalId);

        List<UUID> bedIds = beds.stream()
                .map(Bed::getId)
                .toList();

        // 2. Get active admissions
        List<BedAdmission> admissions =
                admissionRepository.findActiveAdmissionsForBeds(bedIds);

        // 3. Map: bedId → admission
        Map<UUID, BedAdmission> admissionMap = admissions.stream()
                .collect(Collectors.toMap(
                        ba -> ba.getBed().getId(),
                        ba -> ba
                ));

        // 4. Build response
        return beds.stream()
                .map(bed -> {

                    BedAdmission admission = admissionMap.get(bed.getId());

                    UUID patientId = null;
                    String patientName = null;
                    String admissionStatus = null;

                    if (admission != null) {
                        patientId = admission.getPatientId();
                        admissionStatus = admission.getStatus().name();

                        // OPTIONAL: fetch patient
                        // (optimize later with join)
                        // Example:
                        // Patient p = patientRepo.findById(patientId).orElse(null);
                        // patientName = p != null ? p.getFirstName() + " " + p.getLastName() : null;
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

        List<BedAdmission> admissions =
                admissionRepository.findActiveAdmissionsByHospital(hospitalId);

        return admissions.stream()
                .map(admission -> {

                    Bed bed = admission.getBed();

                    return new HospitalPatientResponse(
                            admission.getPatientId(),
                            admission.getId(),
                            bed.getBedNumber(),
                            bed.getBedType().name(),
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

        // Check if the bed actually belongs to this hospital before trying to remove
        boolean removed = hospital.getBeds().removeIf(bed -> bed.getId().equals(bedId));

        if (!removed) {
            throw new EntityNotFoundException("Bed " + bedId + " not found in Hospital " + hospitalId);
        }

        // When the method exits, Hibernate flushes the change and deletes the orphan.
        log.info("Bed {} removed from Hospital {}", bedId, hospitalId);
    }


    // Helper to map Entity to Response
    private HospitalResponse mapToResponse(Hospital hospital) {
        return HospitalResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .licenseNumber(hospital.getLicenseNumber())
                .status(hospital.getStatus())
                .beds(hospital.getBeds() != null ?
                        hospital.getBeds().stream()
                                .map(bed -> bedMapper.toDto(bed, "Mapped from Hospital"))
                                .toList() : List.of()) // Fix: properly close stream and handle nulls
                .city(hospital.getAddress() != null ? hospital.getAddress().getCity() : "Unknown")
                .isEmergencyReady(hospital.getIsEmergencyReady())
                .build();
    }

}
