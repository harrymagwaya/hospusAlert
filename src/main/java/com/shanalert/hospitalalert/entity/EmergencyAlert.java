package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.AlertStatus;
import com.shanalert.hospitalalert.model.Auditable;
import com.shanalert.hospitalalert.model.BedType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EqualsAndHashCode(callSuper = false)
@Table(name = "emergency_alerts")
public class EmergencyAlert extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "hospital_id", nullable = true)
    private UUID hospitalId;

    @Enumerated(EnumType.STRING)
    private BedType requestedBedType;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    // --- Route / ETA Info ---
    private Integer estimatedArrivalTimeMinutes;

    private Double estimatedDistanceKm;

    /**
     * Example values:
     * OSRM
     * DISTANCE_FALLBACK
     */
    private String etaSource;

    @Column(nullable = false)
    private Double patientLat;

    @Column(nullable = false)
    private Double patientLng;

    @Column(columnDefinition = "TEXT")
    private String patientNotes;

    private UUID admissionId;
}