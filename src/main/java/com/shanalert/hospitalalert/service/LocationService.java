package com.shanalert.hospitalalert.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shanalert.hospitalalert.dto.RouteEstimateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String OSRM_API_URL =
            "https://router.project-osrm.org/route/v1/driving/";

    /*
     * This is the fallback average driving/ambulance speed in km/h.
     *
     * Example:
     * 25 km/h = heavy traffic / city congestion
     * 35 km/h = normal urban driving
     * 50 km/h = faster road conditions
     */
    private static final double FALLBACK_SPEED_KMH = 35.0;

    /*
     * Straight-line distance is usually shorter than road distance.
     * This multiplier gives a more realistic estimate.
     *
     * 1.2 = roads are about 20% longer than straight-line distance
     * 1.3 = roads are about 30% longer
     * 1.5 = roads are about 50% longer
     */
    private static final double ROAD_DISTANCE_MULTIPLIER = 1.3;

    public RouteEstimateDTO getRouteEstimate(
            Double startLat,
            Double startLng,
            Double endLat,
            Double endLng
    ) {
        try {
            String url = String.format(
                    "%s%f,%f;%f,%f?overview=false",
                    OSRM_API_URL,
                    startLng,
                    startLat,
                    endLng,
                    endLat
            );

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            JsonNode routes = root.path("routes");

            if (!routes.isArray() || routes.isEmpty()) {
                log.warn("No OSRM route found. Using distance-based fallback.");
                return estimateByDistanceFallback(startLat, startLng, endLat, endLng);
            }

            JsonNode route = routes.get(0);

            double seconds = route.path("duration").asDouble();
            double distanceMeters = route.path("distance").asDouble();

            if (seconds <= 0 || distanceMeters <= 0) {
                log.warn("Invalid OSRM duration or distance. Using distance-based fallback.");
                return estimateByDistanceFallback(startLat, startLng, endLat, endLng);
            }

            int estimatedMinutes = (int) Math.ceil(seconds / 60.0);
            double distanceKm = roundToTwoDecimals(distanceMeters / 1000.0);

            return RouteEstimateDTO.builder()
                    .estimatedMinutes(estimatedMinutes)
                    .distanceKm(distanceKm)
                    .source("OSRM")
                    .build();

        } catch (Exception e) {
            log.error("OSRM routing failed. Using distance-based fallback: {}", e.getMessage());
            return estimateByDistanceFallback(startLat, startLng, endLat, endLng);
        }
    }

    /*
     * Keep this method if other parts of your app still expect only minutes.
     */
    public Integer getEstimatedMinutes(
            Double startLat,
            Double startLng,
            Double endLat,
            Double endLng
    ) {
        return getRouteEstimate(startLat, startLng, endLat, endLng)
                .getEstimatedMinutes();
    }

    private RouteEstimateDTO estimateByDistanceFallback(
            Double startLat,
            Double startLng,
            Double endLat,
            Double endLng
    ) {
        double straightLineDistanceKm = calculateDistanceKm(startLat, startLng, endLat, endLng);

        double adjustedDistanceKm = straightLineDistanceKm * ROAD_DISTANCE_MULTIPLIER;

        double hours = adjustedDistanceKm / FALLBACK_SPEED_KMH;

        int estimatedMinutes = (int) Math.ceil(hours * 60);

        estimatedMinutes = Math.max(estimatedMinutes, 1);

        return RouteEstimateDTO.builder()
                .estimatedMinutes(estimatedMinutes)
                .distanceKm(roundToTwoDecimals(adjustedDistanceKm))
                .source("DISTANCE_FALLBACK")
                .build();
    }

    private double calculateDistanceKm(
            Double startLat,
            Double startLng,
            Double endLat,
            Double endLng
    ) {
        final int EARTH_RADIUS_KM = 6371;

        double latDistance = Math.toRadians(endLat - startLat);
        double lngDistance = Math.toRadians(endLng - startLng);

        double a =
                Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(startLat))
                        * Math.cos(Math.toRadians(endLat))
                        * Math.sin(lngDistance / 2)
                        * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}