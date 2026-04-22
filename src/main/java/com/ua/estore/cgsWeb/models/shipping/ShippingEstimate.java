package com.ua.estore.cgsWeb.models.shipping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * In-memory shipping estimate for one vendor leg of a cart.
 * Not persisted — recomputed on every cart view / address change.
 * When a quote is accepted at checkout, it should be frozen into the Order.totals.shipping
 * and (eventually) a Delivery document that references the RateCard id used.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingEstimate {
    private String vendorId;
    private String vendor;          // display name — matches cart.tpl expectation
    private double distanceMiles;
    private BigDecimal cost;
    private String rateCardId;      // the version used to price this estimate
}