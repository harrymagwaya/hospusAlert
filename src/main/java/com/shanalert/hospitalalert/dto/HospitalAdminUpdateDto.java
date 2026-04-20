package com.shanalert.hospitalalert.dto;

import com.shanalert.hospitalalert.model.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO for updating Hospital Admin profile information.
 * Fields are optional to support partial updates (PATCH).
 */
public record HospitalAdminUpdateDto(
        @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
        String firstName,

        @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid phone number format")
        String phoneNumber,

        Gender gender,

        UUID hospitalId // Used if the admin is being reassigned to a different facility
) {}