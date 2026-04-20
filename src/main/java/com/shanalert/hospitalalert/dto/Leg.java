package com.shanalert.hospitalalert.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shanalert.hospitalalert.dto.Step;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Leg {
    private List<Step> steps;
    private String summary;
}