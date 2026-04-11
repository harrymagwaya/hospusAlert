package com.shanalert.hospitalalert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

import java.util.UUID;

public class Admin {
    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    // --- Authentication Fields ---
    @Column(unique = true, nullable = false)
    private String username; // For login (can be a handle)

    @Column(unique = true, nullable = false)
    private String email;

}
