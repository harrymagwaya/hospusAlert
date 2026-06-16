package com.shanalert.hospitalalert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotBlank(message = "Street information is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    private String district; // Useful for regions like Rubaga, Nakawa, etc.

    private String state;

    @NotBlank(message = "Country is required")
    private String country;

    private Double latitude;

    private Double longitude;
}