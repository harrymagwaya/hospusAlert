package com.shanalert.hospitalalert.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmRoute {
    private Double duration;  // Total time in seconds
    private Double distance;  // Total distance in meters
    private String geometry;  // The encoded Polyline string for the map
    private List<Leg> legs;
}