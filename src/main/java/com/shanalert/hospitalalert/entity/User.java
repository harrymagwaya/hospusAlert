package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.UserRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    // --- Authentication Fields ---
    @Column(unique = true, nullable = false)
    private String username; // For login (can be a handle)

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // Bcrypt encoded hash

    // --- Basic Identity Fields ---
    private String firstName;
    private String lastName;

    @Column(length = 15)
    private String phoneNumber; // Critical for emergency SMS/Calls

    @Enumerated(EnumType.STRING)
    private UserRole role; //

}