package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
}
