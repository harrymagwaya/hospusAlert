package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.Bed;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    List<Bed> findByHospitalIdAndBedType(UUID hospitalId, BedType bedType);

    Page<Bed> findByHospitalIdAndBedType(UUID hospitalId, BedType bedType, Pageable pageable);
    Page<Bed> findAllByHospitalId(UUID hospitalId, Pageable pageable);
}
