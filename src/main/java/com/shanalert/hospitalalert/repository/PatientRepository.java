package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {
}
