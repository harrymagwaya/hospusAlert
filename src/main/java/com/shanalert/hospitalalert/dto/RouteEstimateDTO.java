package com.shanalert.hospitalalert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteEstimateDTO {

    private Integer estimatedMinutes;
    private Double distanceKm;
    private String source;
}