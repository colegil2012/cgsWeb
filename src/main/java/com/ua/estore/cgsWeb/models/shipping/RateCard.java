package com.ua.estore.cgsWeb.models.shipping;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted, versioned pricing configuration used to generate shipping estimates.
 *
 * <p>Phase 1 keeps this intentionally minimal — just a base fee and a per-mile rate.
 * Future phases will layer on weight tiers, peak-time multipliers, fuel surcharges,
 * driver pay splits, etc. Old deliveries should always reference the {@code id} of
 * the rate card that priced them, so history stays accurate when rates change.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "rateCards")
public class RateCard {

    @Id
    private String id;

    /** Human-readable label, e.g. "Standard 2026". */
    private String name;

    /** Monotonically increasing — new rates create a new version rather than mutate. */
    private int version;

    /** Only one card should be active at a time. Enforced in the service layer. */
    private boolean active;

    // ---- Phase 1 pricing knobs ------------------------------------------------

    /** Flat fee applied to every delivery. */
    private BigDecimal baseFee;

    /** Dollars charged per mile between pickup and dropoff. */
    private BigDecimal perMileRate;

    /** Floor price — estimate will never fall below this. */
    private BigDecimal minimumFee;

    /** If the order subtotal meets this threshold, shipping is free. Nullable = disabled. */
    private BigDecimal freeShippingThreshold;

    // ---- Audit ----------------------------------------------------------------

    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;   // null = open-ended
    private LocalDateTime createdAt;
}