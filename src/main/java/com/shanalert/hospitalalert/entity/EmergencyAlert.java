package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.AlertStatus;
import com.shanalert.hospitalalert.model.Auditable;
import com.shanalert.hospitalalert.model.BedType;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.util.UUID;

@Entity
@Table(name = "emergency_alerts")
public class EmergencyAlert extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @JdbcTypeCode(Types.VARCHAR)
    private UUID assignedBedId;

    @Enumerated(EnumType.STRING)
    private BedType requestedBedType;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    // --- Critical Triage Info ---
    private Integer estimatedArrivalTimeMinutes;

    @Column(columnDefinition = "TEXT")
    private String patientNotes; // e.g., "Difficulty breathing, chest pain"

}