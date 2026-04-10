package com.shanalert.hospitalalert.entity;


import com.shanalert.hospitalalert.model.Auditable;
import com.shanalert.hospitalalert.model.BedStatus;
import com.shanalert.hospitalalert.model.BedType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "beds")
public class Bed extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "bed_id", updatable = false, nullable = false)
    private UUID id;

    // Link back to the hospital
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @Column(nullable = false)
    private String bedNumber; // Human-readable (e.g., "SURGERY-101")

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedType bedType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BedStatus status;

    // The bridge to the Patient/User UUID
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "occupied_by_patient_id")
    private UUID occupiedByPatientId;

}