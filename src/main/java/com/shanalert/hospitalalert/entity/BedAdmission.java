package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.AdmissionStatus;
import com.shanalert.hospitalalert.model.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bed_admission")
public class BedAdmission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    private Bed bed;

    private UUID patientId;

    private UUID hospitalId;

    @Enumerated(EnumType.STRING)
    private AdmissionStatus status;

    private LocalDateTime admittedAt;

    private LocalDateTime dischargedAt;

    private LocalDateTime reservedAt;

}