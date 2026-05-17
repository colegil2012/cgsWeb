package com.ua.estore.cgsWeb.repositories.shop;

import com.ua.estore.cgsWeb.models.shop.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    /* ---- Existing storefront / driver queries (unchanged) ---- */

    Page<Order> findByUserId(String userId, Pageable page);

    Optional<Order> findByIdAndUserId(String orderId, String userId);

    List<Order> findByStatusAndPlacedAtBetween(Order.OrderStatus status,
                                               LocalDateTime startTime,
                                               LocalDateTime endTime);

    /* ============================================================================
     * Admin queries (Round 2)
     * ============================================================================ */

    /** Paged list of all orders — the admin orders list, unfiltered. */
    Page<Order> findAll(Pageable pageable);

    /** Paged list filtered to a single financial status. */
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);

    /**
     * Admin search: match against the order number or the snapshotted
     * customer first/last name, case-insensitive. The caller (AdminOrderService)
     * passes an already-regex-escaped fragment.
     *
     * <p>Because customer name is denormalized onto the order via
     * CustomerSnapshot, this needs no join to the users collection.</p>
     */
    @Query("{ $or: [ "
            + "{ 'orderNumber': { $regex: ?0, $options: 'i' } }, "
            + "{ 'customer.firstName': { $regex: ?0, $options: 'i' } }, "
            + "{ 'customer.lastName': { $regex: ?0, $options: 'i' } } "
            + "] }")
    Page<Order> searchByNumberOrCustomer(String regex, Pageable pageable);

    /**
     * Same search, constrained to a status. Used when the admin has both a
     * status filter and a search term active.
     */
    @Query("{ $and: [ "
            + "{ 'status': ?1 }, "
            + "{ $or: [ "
            +   "{ 'orderNumber': { $regex: ?0, $options: 'i' } }, "
            +   "{ 'customer.firstName': { $regex: ?0, $options: 'i' } }, "
            +   "{ 'customer.lastName': { $regex: ?0, $options: 'i' } } "
            + "] } "
            + "] }")
    Page<Order> searchByNumberOrCustomerAndStatus(String regex,
                                                  Order.OrderStatus status,
                                                  Pageable pageable);
}