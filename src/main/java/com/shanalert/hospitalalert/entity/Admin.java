package com.shanalert.hospitalalert.entity;

import jakarta.persistence.Column;

import java.util.UUID;

public class Admin {

    private UUID id;

    // --- Authentication Fields ---
    @Column(unique = true, nullable = false)
    private String username; // For login (can be a handle)

    @Column(unique = true, nullable = false)
    private String email;

}
