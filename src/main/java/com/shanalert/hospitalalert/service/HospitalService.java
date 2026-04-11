package com.shanalert.hospitalalert.service;

import com.shanalert.hospitalalert.dto.HospitalDiscoveryResponse;
import com.shanalert.hospitalalert.entity.Address;
import com.shanalert.hospitalalert.entity.Hospital;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import com.shanalert.hospitalalert.model.EmergencyType;
import com.shanalert.hospitalalert.repository.HospitalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

}
