package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BedRepository extends JpaRepository<Bed, UUID> {
    Optional<Bed> findFirstByHospitalIdAndBedTypeAndStatus(
            UUID hospitalId,
            BedType bedType,
            BedStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT b FROM Bed b
    WHERE b.hospitalId = :hospitalId
    AND b.bedType = :type
    AND b.status = 'AVAILABLE'
    ORDER BY b.id ASC
""")
    Optional<Bed> findLockedAvailableBed(UUID hospitalId, BedType type);


    @Query("SELECT DISTINCT b.hospitalId FROM Bed b " +
            "WHERE b.bedType = :type " +
            "AND b.status = :status")
    List<UUID> findHospitalIdsWithAvailableBeds(
            @Param("type") BedType type,
            @Param("status") BedStatus status
    );

    List<Bed> findByHospitalIdAndBedType(UUID hospitalId, BedType bedType);

    Page<Bed> findByHospitalIdAndBedType(UUID hospitalId, BedType bedType, Pageable pageable);
    Page<Bed> findAllByHospitalId(UUID hospitalId, Pageable pageable);

    List<Bed> findAllByHospitalId(UUID hospitalId);


    @Query("""
    SELECT COUNT(b)
    FROM Bed b
    WHERE b.hospitalId = :hospitalId
    AND b.bedType = :type
    AND b.status = com.shanalert.hospitalalert.model.BedStatus.AVAILABLE
""")
    int countAvailableBeds(UUID hospitalId, BedType type);
}
