package com.shanalert.hospitalalert.repository;

import com.shanalert.hospitalalert.entity.EmergencyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, UUID> {
}
