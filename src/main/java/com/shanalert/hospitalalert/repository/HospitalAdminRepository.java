package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.HospitalAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HospitalAdminRepository extends JpaRepository<HospitalAdmin, UUID> {
    List<HospitalAdmin> findByHospitalId(UUID hospitalId);

    boolean existsByIdAndHospital_Id(UUID adminId, UUID hospitalId);


}
