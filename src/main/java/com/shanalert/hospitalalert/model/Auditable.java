package com.shanalert.hospitalalert.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class Auditable {

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private UUID createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private UUID updatedBy;

    @PrePersist
    protected void onCreate() {

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
