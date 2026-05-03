package com.ua.estore.cgsWeb.repositories.shop;

import com.ua.estore.cgsWeb.models.shop.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable page);

    Optional<Order> findByIdAndUserId(String orderId, String userId);
}
