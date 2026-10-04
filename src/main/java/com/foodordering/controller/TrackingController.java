package com.foodordering.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Order;
import com.foodordering.model.User;
import com.foodordering.security.CurrentUser;
import com.foodordering.service.DeliveryTrackingService;
import com.foodordering.service.OrderService;

@RestController
@RequestMapping("/api")
public class TrackingController {

    private final OrderService orderService;
    private final DeliveryTrackingService trackingService;

    public TrackingController(OrderService orderService, DeliveryTrackingService trackingService) {
        this.orderService = orderService;
        this.trackingService = trackingService;
    }

    @GetMapping("/orders/{id}/tracking")
    public Map<String, Object> tracking(@PathVariable("id") Long id) {
        User user = CurrentUser.get();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Order order;
        try {
            order = orderService.findOwnedOrder(user, id);
        } catch (BadRequestException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Akses ditolak");
        }
        return trackingService.track(order);
    }
}