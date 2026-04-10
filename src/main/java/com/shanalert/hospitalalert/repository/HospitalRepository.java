package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HospitalRepository extends JpaRepository<Hospital, UUID>{
}
