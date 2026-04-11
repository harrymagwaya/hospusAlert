package com.shanalert.hospitalalert.entity;

import com.shanalert.hospitalalert.model.Gender;
import com.shanalert.hospitalalert.model.UserRole;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
public class HospitalAdmin {

    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String username; // For login (can be a handle)

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String firstName;
    private String lastName;

    @Column(length = 15)
    private String phoneNumber; // Critical for emergency SMS/Calls

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", referencedColumnName = "id")
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    private UserRole role; //

}
