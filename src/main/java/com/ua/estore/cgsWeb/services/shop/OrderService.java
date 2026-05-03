package com.ua.estore.cgsWeb.services.shop;


import com.ua.estore.cgsWeb.config.props.TaxProperties;
import com.ua.estore.cgsWeb.models.address.Address;
import com.ua.estore.cgsWeb.models.dto.product.ProductDTO;
import com.ua.estore.cgsWeb.models.dto.shop.OrderDTO;
import com.ua.estore.cgsWeb.models.shop.Order;
import com.ua.estore.cgsWeb.models.user.User;
import com.ua.estore.cgsWeb.repositories.shop.OrderRepository;
import com.ua.estore.cgsWeb.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    public static final Duration CANCEL_WINDOW = Duration.ofMinutes(10);

    private final OrderRepository orderRepository;
    private final TaxProperties taxProperties;

    public BigDecimal getTaxRate() {
        return taxProperties.rate();
    }

    public Page<Order> findByUserId(String userId, Pageable page) {
        return orderRepository.findByUserId(userId, page);
    }


    //Find by user id pageable for Myorders Tab
    public Page<Order> findByUserPage(String userId, int page) {
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                10,
                Sort.by(Sort.Direction.DESC, "placedAt")
        );

        return orderRepository.findByUserId(userId, pageable);
    }

    public Order getOrderById(String id) {
        return orderRepository.findById(id).orElse(null);
    }

    public Optional<Order> getOrderForUser(String orderId, String userId) {
        if (orderId == null || orderId.isBlank() || userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return orderRepository.findByIdAndUserId(orderId, userId);
    }

    /**********************************************************
     * Order Saving Methods
     *********************************************************/

    /**********************************************************
     * Pending Order
     *********************************************************/

    public String savePendingOrder(OrderDTO dto, Address ship,
                                   String rateCardId, BigDecimal shippingCost) {
        Objects.requireNonNull(dto, "OrderDTO required");
        Objects.requireNonNull(dto.getUser(), "OrderDTO.user required");
        Objects.requireNonNull(ship, "Shipping address required");

        User user = dto.getUser();

        Order order = new Order();
        order.setUserId(user.getId());
        order.setIdempotencyKey(resolveIdempotencyKey(dto));
        order.setStatus(Order.OrderStatus.PENDING);
        order.setPaymentStatus(Order.PaymentStatus.NOT_ATTEMPTED);

        order.setCustomer(snapshotCustomer(user));
        order.setShipTo(snapshotAddress(ship));
        // billTo intentionally left null until we wire a separate billing-address picker.

        List<Order.OrderItem> items = snapshotItems(dto.getProducts());
        order.setItems(items);
        order.setTotals(computeTotals(items, nz(shippingCost), rateCardId));

        order.setDeliveryInstructions(dto.getDescription()); // reusing existing DTO field
        order.setOrderNumber(generateOrderNumber());

        order.setPlacedAt(TimeUtil.getCurrentDateTime());
        order.setUpdatedAt(TimeUtil.getCurrentDateTime());

        Order saved = orderRepository.save(order);
        log.info("Saved PENDING order id={} number={} userId={} subtotal={} shipping={} tax={} total={}",
                saved.getId(), saved.getOrderNumber(), saved.getUserId(),
                saved.getTotals().getSubtotal(), saved.getTotals().getShipping(),
                saved.getTotals().getTax(), saved.getTotals().getTotal());

        return saved.getId();
    }

    /**********************************************************
     * Cancel Order
     *********************************************************/

    public Order cancelOrder(String orderId, String userId) {
        Order order = getOrderForUser(orderId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if(order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalArgumentException("This order can no longer be cancelled.");
        }

        LocalDateTime placedAt = order.getPlacedAt();
        if(placedAt == null || Duration.between(placedAt, TimeUtil.getCurrentDateTime())
                .compareTo(CANCEL_WINDOW) > 0) {
            throw new IllegalArgumentException("Cancellation window has expired.");
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelledAt(TimeUtil.getCurrentDateTime());
        order.setUpdatedAt(TimeUtil.getCurrentDateTime());

        Order saved = orderRepository.save(order);
        log.info("Cancelled order id={} number={} userId={}", saved.getId(), saved.getOrderNumber(), userId);
        return saved;
    }

    /* ============================================================================
     * Snapshot helpers – keep the order self-contained.
     * ============================================================================ */

    private Order.CustomerSnapshot snapshotCustomer(User user) {
        Order.CustomerSnapshot c = new Order.CustomerSnapshot();
        c.setUserId(user.getId());
        c.setFirstName(user.getProfile().getFirstName());
        c.setLastName(user.getProfile().getLastName());
        c.setEmail(user.getEmail());
        c.setPhone(user.getProfile().getPhoneNumber());
        return c;
    }

    private Order.AddressSnapshot snapshotAddress(Address a) {
        Order.AddressSnapshot s = new Order.AddressSnapshot();
        s.setSourceAddressId(a.getAddressId());
        s.setType(a.getType());
        s.setStreet1(a.getStreet1());
        s.setStreet2(a.getStreet2());
        s.setCity(a.getCity());
        s.setState(a.getState());
        s.setZip(a.getZip());
        s.setLatitude(a.getLatitude());
        s.setLongitude(a.getLongitude());
        s.syncGeoPoint();
        return s;
    }

    private List<Order.OrderItem> snapshotItems(List<ProductDTO> products) {
        if (products == null) return new ArrayList<>();
        List<Order.OrderItem> out = new ArrayList<>(products.size());
        for (ProductDTO p : products) {
            Order.OrderItem item = new Order.OrderItem();
            item.setProductId(p.getId());
            item.setVendorId(p.getVendorId());
            item.setName(p.getName());
            item.setVendorName(p.getVendorName());
            item.setImageUrl(p.getImageUrl());
            item.setPriceAtPurchase(effectivePrice(p));
            item.setQuantity(p.getQuantity());
            item.setLineTotal(item.getPriceAtPurchase()
                    .multiply(BigDecimal.valueOf(item.getQuantity())));
            out.add(item);
        }
        return out;
    }

    private Order.OrderTotals computeTotals(List<Order.OrderItem> items,
                                            BigDecimal shipping, String rateCardId) {
        BigDecimal subtotal = items.stream()
                .map(Order.OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxableBase = subtotal.add(shipping);
        BigDecimal tax = taxableBase.multiply(taxProperties.rate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shipping).add(tax).setScale(2, RoundingMode.HALF_UP);

        Order.OrderTotals t = new Order.OrderTotals();
        t.setSubtotal(subtotal);
        t.setShipping(shipping.setScale(2, RoundingMode.HALF_UP));
        t.setTax(tax);
        t.setDiscount(BigDecimal.ZERO);
        t.setTotal(total);
        t.setRateCardId(rateCardId);
        return t;
    }

    private static String resolveIdempotencyKey(OrderDTO dto) {
        if (dto.getIdempotencyKey() != null) return dto.getIdempotencyKey().toString();
        return UUID.randomUUID().toString();
    }

    private static BigDecimal effectivePrice(ProductDTO p) {
        return p.getSalePrice() != null && p.getSalePrice().signum() > 0
                ? p.getSalePrice() : p.getPrice();
    }

    private static BigDecimal nz(BigDecimal b) { return b != null ? b : BigDecimal.ZERO; }

    /**
     * Year-prefixed sequential number generated cheaply via timestamp. Replace with a
     * real Mongo counter collection if collisions ever surface in load testing.
     */
    private static String generateOrderNumber() {
        return Year.now() + "-" + System.currentTimeMillis() % 100_000_000L;
    }
}