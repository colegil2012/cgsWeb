package com.ua.estore.cgsWeb.models.dto.driver.route;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for {@code POST /api/driver/routes}.
 *
 * <p>The kiosk sends a list of Order ids the driver selected, plus an
 * {@link #idempotencyKey} (a UUID generated client-side per generation attempt).
 * If a Route with the same idempotency key already exists, the server returns
 * the existing one instead of calling the optimizer again — guards against
 * double-clicks and network retries.</p>
 *
 * <p>{@link #origin} is optional. When omitted, the server uses its configured
 * home base. Useful for future "regenerate from current GPS" support.</p>
 */
@Data
@NoArgsConstructor
public class GenerateRouteRequest {

    /** Order ids selected by the driver. Must be non-empty; max varies by optimizer. */
    private List<String> orderIds;

    /** Client-generated UUID. Required for idempotency guarantees. */
    private String idempotencyKey;

    /** Optional override for the route's starting point. Null = use configured home base. */
    private OriginInput origin;

    @Data
    @NoArgsConstructor
    public static class OriginInput {
        private Double latitude;
        private Double longitude;
    }
}