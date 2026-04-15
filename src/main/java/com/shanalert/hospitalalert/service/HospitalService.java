package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.HospitalDiscoveryResponse;
import com.shanalert.hospitalalert.dto.HospitalRequest;
import com.shanalert.hospitalalert.dto.HospitalResponse;
import com.shanalert.hospitalalert.entity.Address;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.model.EmergencyType;
import com.shanalert.hospitalalert.model.HospitalStatus;
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
import java.util.UUID;

@Slf4j
@Service
public class HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private BedService bedService;

    @Autowired
    private AddressService addressService;



    @Transactional(readOnly = true)
    public Page<HospitalResponse> getAllHospitals(Pageable pageable) {
        return hospitalRepository.findAll(pageable)
                .map(this::mapToResponse);
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
                    int availableCount = (hospital.getBeds() == null) ? 0 : (int) hospital.getBeds().stream()
                            .filter(b -> b.getBedType() == neededBed && b.getStatus() == BedStatus.AVAILABLE)
                            .count();
                    return new HospitalDiscoveryResponse(hospital.getId(), hospital.getName(), street, eta, availableCount, lat,
                           lng);
                })
                .sorted(Comparator.comparing(HospitalDiscoveryResponse::estimatedMinutes,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }


    // Helper to map Entity to Response
    private HospitalResponse mapToResponse(Hospital hospital) {
        return HospitalResponse.builder()
                .id(hospital.getId())
                .name(hospital.getName())
                .licenseNumber(hospital.getLicenseNumber())
                .status(hospital.getStatus())
                .city(hospital.getAddress().getCity())
                .isEmergencyReady(hospital.getIsEmergencyReady())
                .build();
    }

}
