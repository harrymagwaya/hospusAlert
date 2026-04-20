package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    @Query("SELECT d FROM Doctor d JOIN d.hospitals h WHERE h.id = :hospitalId")
    List<Doctor> findByHospitalId(@Param("hospitalId") UUID hospitalId);
}
