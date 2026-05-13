package com.ua.estore.cgsWeb.repositories.driver.delivery;

import com.ua.estore.cgsWeb.models.driver.delivery.Delivery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends MongoRepository<Delivery, String> {

    /**
     * Lookup by source Order. Used by the driver service to lazy-create a
     * Delivery the first time an Order is fetched without one. Indexed +
     * unique on orderId — at most one result.
     */
    Optional<Delivery> findByOrderId(String orderId);

    /**
     * Bulk lookup by Order ids. Used when generating a route — we hold a list
     * of selected Order ids and need their Deliveries (creating any missing).
     */
    List<Delivery> findByOrderIdIn(List<String> orderIds);

    /**
     * "What Deliveries are currently assigned to this Route" — used when
     * cancelling a Route to release its Deliveries back to PENDING.
     */
    List<Delivery> findByCurrentRouteId(String routeId);
}