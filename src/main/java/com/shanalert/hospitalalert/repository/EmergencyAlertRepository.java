package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.EmergencyAlert;
import com.shanalert.hospitalalert.model.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, UUID> {

    List<EmergencyAlert> findByPatientId(UUID patientId);

    List<EmergencyAlert> findByHospitalId(UUID hospitalId);

    List<EmergencyAlert> findByStatus(AlertStatus status);
}
