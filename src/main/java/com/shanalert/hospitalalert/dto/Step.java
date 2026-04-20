package com.shanalert.hospitalalert.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Step {
    private String name;        // e.g., "Entebbe Road"
    private String instruction; // OSRM's generated turn instruction
    private Double distance;
    private Double duration;
    @JsonProperty("maneuver")
    private Map<String, Object> maneuver; // Contains 'type' and 'modifier' (left, right, etc)
}