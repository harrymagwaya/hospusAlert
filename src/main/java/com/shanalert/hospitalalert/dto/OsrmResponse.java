package com.shanalert.hospitalalert.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.shanalert.hospitalalert.dto.OsrmRoute;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OsrmResponse {
    private String code;
    private List<OsrmRoute> routes;
}