package com.ua.estore.cgsWeb.services.shipping;

import com.ua.estore.cgsWeb.models.Address;
import com.ua.estore.cgsWeb.models.Cart;
import com.ua.estore.cgsWeb.models.Product;
import com.ua.estore.cgsWeb.models.shipping.RateCard;
import com.ua.estore.cgsWeb.models.shipping.ShippingEstimate;
import com.ua.estore.cgsWeb.models.shipping.Warehouse;
import com.ua.estore.cgsWeb.services.shop.ProductService;
import com.ua.estore.cgsWeb.util.GeoDistance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Generates shipping estimates for a cart.
 *
 * <p><b>Phase 1 model (single-origin):</b> all goods ship from the primary
 * warehouse resolved via {@link WarehouseService}. One distance, one estimate.</p>
 *
 * <p><b>Phase 1 formula:</b>
 * <pre>
 *   cost = max( baseFee + (distanceMiles * perMileRate), minimumFee )
 *   if (cartSubtotal >= freeShippingThreshold) cost = 0
 * </pre>
 *
 * <p>When the logistics model grows (multi-warehouse, real MPG, weight tiers,
 * driver pay, peak demand), modify ONLY {@link #priceLeg(RateCard, double, BigDecimal)}.
 * The public surface — {@link #estimateForCart(Cart, Address)} — stays stable.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingEstimateService {

    private final ShippingRateService shippingRateService;
    private final WarehouseService warehouseService;
    private final ProductService productService;

    /**
     * Produce a shipping estimate for the cart as a whole.
     * Returned as a single-element list so templates that already iterate over
     * {@code shippingEstimates} keep working, and so we can re-introduce
     * multi-origin later without changing callers.
     */
    public List<ShippingEstimate> estimateForCart(Cart cart, Address destination) {
        if (cart == null || cart.getItems().isEmpty() || destination == null) {
            return List.of();
        }

        RateCard rateCard = shippingRateService.getActiveRateCard();
        Warehouse origin = warehouseService.getPrimaryWarehouse();

        Address originAddress = origin.getAddress();
        if (originAddress == null) {
            log.error("Primary warehouse '{}' has no address — cannot produce estimate.", origin.getId());
            return List.of();
        }

        BigDecimal cartSubtotal = computeCartSubtotal(cart);

        double miles = GeoDistance.haversineMiles(
                originAddress.getLatitude(), originAddress.getLongitude(),
                destination.getLatitude(), destination.getLongitude()
        );

        BigDecimal cost = priceLeg(rateCard, miles, cartSubtotal);

        ShippingEstimate estimate = ShippingEstimate.builder()
                .vendorId(null)                           // N/A for consolidated shipping
                .vendor(origin.getName())                 // e.g. "Celtech General Store"
                .distanceMiles(round(miles, 2))
                .cost(cost)
                .rateCardId(rateCard.getId())
                .build();

        return List.of(estimate);
    }

    /**
     * 🔧 THE ONE METHOD TO CHANGE WHEN YOU HAVE REAL DATA.
     * See class javadoc for the full upgrade path.
     */
    private BigDecimal priceLeg(RateCard card, double miles, BigDecimal cartSubtotal) {
        if (card.getFreeShippingThreshold() != null
                && cartSubtotal.compareTo(card.getFreeShippingThreshold()) >= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal distanceCost = card.getPerMileRate()
                .multiply(BigDecimal.valueOf(miles));

        BigDecimal subtotal = card.getBaseFee().add(distanceCost);

        BigDecimal floor = Optional.ofNullable(card.getMinimumFee()).orElse(BigDecimal.ZERO);
        return subtotal.max(floor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeCartSubtotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (Cart.Item item : cart.getItems()) {
            Product product = productService.getProductById(item.getProductId());
            if (product == null) continue;

            BigDecimal unit = Optional.ofNullable(product.getSalePrice()).orElse(product.getPrice());
            if (unit == null) continue;

            total = total.add(unit.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    private double round(double v, int places) {
        return BigDecimal.valueOf(v).setScale(places, RoundingMode.HALF_UP).doubleValue();
    }
}