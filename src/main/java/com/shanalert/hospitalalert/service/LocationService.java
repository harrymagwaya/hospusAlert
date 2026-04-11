package com.shanalert.hospitalalert.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class LocationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Public OSRM Demo Server (Note: Use your own instance for production)
    private static final String OSRM_API_URL = "http://router.project-osrm.org/route/v1/driving/";

    public Integer getEstimatedMinutes(Double startLat, Double startLng, Double endLat, Double endLng) {
        try {
            // OSRM format: {longitude},{latitude};{longitude},{latitude}
            String url = String.format("%s%f,%f;%f,%f?overview=false",
                    OSRM_API_URL, startLng, startLat, endLng, endLat);

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            // duration is in seconds, convert to minutes
            double seconds = root.path("routes").get(0).path("duration").asDouble();
            return (int) Math.ceil(seconds / 60.0);

        } catch (Exception e) {
            log.error("OSRM Routing failed, falling back to 15 mins: {}", e.getMessage());
            return 15; // Safe fallback
        }
    }
}