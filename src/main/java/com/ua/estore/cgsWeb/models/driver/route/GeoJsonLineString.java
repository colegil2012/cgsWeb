package com.ua.estore.cgsWeb.models.driver.route;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Minimal GeoJSON LineString embedded type. Mirrors the pattern of
 * {@code Order.GeoPoint} and {@code Delivery.GeoPoint} for consistency.
 *
 * <p>The {@code type} field is fixed at {@code "LineString"}. {@link #coordinates}
 * is a list of {@code [lng, lat]} pairs (per GeoJSON spec — longitude first).
 * Fed directly to the kiosk's {@code celtechSetRoute()} which expects exactly
 * this shape.</p>
 *
 * <p>Stored as a Map-ish embedded document in Mongo, not indexed for geo queries
 * — we don't need spatial queries on route geometries, just round-trip storage.</p>
 */
@Data
@NoArgsConstructor
public class GeoJsonLineString {

    private final String type = "LineString";

    /** Ordered list of {@code [lng, lat]} pairs. */
    private List<double[]> coordinates;

    public GeoJsonLineString(List<double[]> coordinates) {
        this.coordinates = coordinates;
    }
}