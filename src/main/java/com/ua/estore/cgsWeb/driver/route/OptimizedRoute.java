package com.ua.estore.cgsWeb.driver.route;

import java.util.List;

/**
 * Full optimizer output. Plain data — the service layer maps this into the
 * {@code Route} document.
 *
 * @param stops             ordered list of stops, sequence-numbered from 1
 * @param geometryCoordinates GeoJSON-style [lng, lat] pairs forming the driving polyline
 * @param totalDistanceMeters total driving distance including all legs (and return-to-origin when requested)
 * @param totalDurationSeconds total driving time, in seconds
 * @param geometryProvider  identifier of the service that produced the geometry
 * @param optimizerVersion  version string for the optimizer (when available)
 * @param elapsedMillis     wall-clock time the optimizer took
 */
public record OptimizedRoute(
        List<OptimizedStop> stops,
        List<double[]> geometryCoordinates,
        long totalDistanceMeters,
        long totalDurationSeconds,
        String geometryProvider,
        String optimizerVersion,
        long elapsedMillis
) {}