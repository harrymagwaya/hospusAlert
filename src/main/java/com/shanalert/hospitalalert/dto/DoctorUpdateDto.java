package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.Gender;
import com.shanalert.hospitalalert.model.MedicalSpecialty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO for partial updates to a Doctor's profile.
 * Fields are optional to support PATCH logic.
 */
public record DoctorUpdateDto(
        @Size(min = 2, max = 50)
        String firstName,

        @Size(min = 2, max = 50)
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone format")
        String phoneNumber,

        Gender gender,

        // Professional Fields
        String medicalLicenseNumber,

        MedicalSpecialty specialization,

        @Size(max = 500)
        String qualifications,

        @Min(0)
        Integer yearsOfExperience,

        String department,

        Boolean isAvailable,

        // To add an additional hospital via the many-to-many relationship
        UUID newHospitalId
) {}