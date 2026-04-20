package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.entity.BedAdmission;
import com.shanalert.hospitalalert.model.AdmissionStatus;
import com.shanalert.hospitalalert.model.BedType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BedAdmissionRepository  extends JpaRepository<BedAdmission, UUID> {

    List<BedAdmission> findByHospitalIdAndStatus(
            UUID hospitalId,
            AdmissionStatus status
    );

    List<BedAdmission> findByBedIdAndStatus(
            UUID bedId,
            AdmissionStatus status
    );

    List<BedAdmission> findByHospitalId(UUID hospitalId);

    Optional<BedAdmission> findTopByBedIdAndStatusOrderByAdmittedAtDesc(
            UUID bedId,
            AdmissionStatus status
    );

    @Query("""
    SELECT ba.patientId
    FROM BedAdmission ba
    WHERE ba.status = :status
""")
    List<UUID> findAllAdmittedPatients(@Param("status") AdmissionStatus status);


    @Query("""
    SELECT ba FROM BedAdmission ba
    WHERE ba.hospitalId = :hospitalId
    AND ba.status IN ('ARRIVED', 'ADMITTED')
""")
    List<BedAdmission> findActiveAdmissionsByHospital(UUID hospitalId);

    @Query("""
    SELECT ba FROM BedAdmission ba
    WHERE ba.bed.id IN :bedIds
    AND ba.status IN ('ARRIVED', 'ADMITTED')
""")
    List<BedAdmission> findActiveAdmissionsForBeds(List<UUID> bedIds);
}
