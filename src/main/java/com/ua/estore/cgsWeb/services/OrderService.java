package com.ua.estore.cgsWeb.services;

import com.ua.estore.cgsWeb.models.Order;
import com.ua.estore.cgsWeb.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Query("{ 'userId': ?0 }")
    public Page<Order> findByUserId(String userId, Pageable page) {
        return orderRepository.findByUserId(userId, page);
    };
}
