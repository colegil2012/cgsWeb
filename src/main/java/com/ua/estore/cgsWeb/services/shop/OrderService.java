package com.ua.estore.cgsWeb.services.shop;

import com.ua.estore.cgsWeb.models.Order;
import com.ua.estore.cgsWeb.models.dto.shop.OrderDTO;
import com.ua.estore.cgsWeb.repositories.OrderRepository;
import com.ua.estore.cgsWeb.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Query("{ 'userId': ?0 }")
    public Page<Order> findByUserId(String userId, Pageable page) {
        return orderRepository.findByUserId(userId, page);
    };

    public Order getOrderById(String id) {
        return orderRepository.findById(id).orElse(null);
    };


    //Save Pending Order in Checkout
    public String savePendingOrder(OrderDTO orderDTO) {
        Order order = new Order();
        order.setUserId(orderDTO.getUser().getId());
        order.setItems(order.getItems());
        order.setTotals(order.getTotals());
        order.setCreatedAt(TimeUtil.getCurrentDateTime());
        order.setUpdatedAt(TimeUtil.getCurrentDateTime());
        order.setStatus("PENDING");

        return orderRepository.save(order).getId();
    }

    //Save Shipment Created Order
    public String saveShipmentCreatedOrder(OrderDTO orderDTO) {
        Order order = getOrderById(orderDTO.getOrderId());

        if (order == null) return null;

        List<Order.RoadieData> roadieDataList = new ArrayList<>();
        if (orderDTO.getRoadieShipments() != null) {
            for (OrderDTO.Shipment shipment : orderDTO.getRoadieShipments()) {
                Order.RoadieData roadieData = new Order.RoadieData();
                roadieData.setOrderId(shipment.getOrderId());
                roadieData.setTrackingNumber(shipment.getTrackingNumber());
                roadieDataList.add(roadieData);
            }
        }

        order.setRoadieData(roadieDataList);
        order.setUpdatedAt(TimeUtil.getCurrentDateTime());
        order.setStatus("SHIPMENT_CREATED");

        return orderRepository.save(order).getId();
    }
}
