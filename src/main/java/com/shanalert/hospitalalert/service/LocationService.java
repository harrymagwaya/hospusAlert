package com.shanalert.hospitalalert.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanalert.hospitalalert.dto.OsrmResponse;
import com.shanalert.hospitalalert.dto.OsrmRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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

    /**
     * Fetches the full route including the polyline (geometry)
     * and turn-by-turn instructions (steps).
     */
    public OsrmRoute getDetailedRoute(Double pLat, Double pLng, Double hLat, Double hLng) {

        // OSRM requires {longitude},{latitude}
        String coordinates = String.format("%f,%f;%f,%f", pLng, pLat, hLng, hLat);

        // overview=full: returns the high-detail path
        // geometries=polyline: returns the encoded string for the map
        // steps=true: returns the instructions for each turn
        String url = UriComponentsBuilder.fromHttpUrl(OSRM_API_URL + coordinates)
                .queryParam("overview", "full")
                .queryParam("geometries", "polyline")
                .queryParam("steps", "true")
                .toUriString();

        log.info("Requesting detailed route from OSRM: {}", url);

        try {
            OsrmResponse response = restTemplate.getForObject(url, OsrmResponse.class);

            if (response != null && "Ok".equals(response.getCode()) && !response.getRoutes().isEmpty()) {
                return response.getRoutes().get(0);
            } else {
                log.warn("OSRM returned an empty or invalid response for coordinates: {}", coordinates);
            }
        } catch (Exception e) {
            log.error("OSRM service communication error: {}", e.getMessage());
        }

        return null; // Handle this null in your calling service
    }
}