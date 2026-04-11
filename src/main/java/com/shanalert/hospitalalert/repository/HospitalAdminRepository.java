package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.HospitalAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HospitalAdminRepository extends JpaRepository<HospitalAdmin, UUID> {

}
