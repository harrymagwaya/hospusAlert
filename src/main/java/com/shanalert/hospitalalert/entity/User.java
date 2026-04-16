package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.Gender;
import com.shanalert.hospitalalert.model.UserRole;
import com.shanalert.hospitalalert.model.UserStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username; // For login (can be a handle)

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password; // Bcrypt encoded hash

    private String firstName;
    private String lastName;

    @Column(length = 15)
    private String phoneNumber; // Critical for emergency SMS/Calls

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus userStatus;

    private Gender gender;

}