package com.foodordering.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.foodordering.model.Order;
import com.foodordering.model.OrderStatus;
import com.foodordering.repository.FoodRepository;
import com.foodordering.repository.OrderItemRepository;
import com.foodordering.repository.OrderRepository;
import com.foodordering.repository.UserRepository;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ReportService(OrderRepository orderRepository, FoodRepository foodRepository,
                         UserRepository userRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("menuCount", foodRepository.count());
        stats.put("orderCount", orderRepository.count());
        stats.put("pendingCount", orderRepository.countByStatus(OrderStatus.PENDING));
        stats.put("customerCount", userRepository.countCustomers());
        stats.put("revenue", orderRepository.sumTotalNonCancelled());
        return stats;
    }

    @Transactional(readOnly = true)
    public List<Order> recentOrders(int limit) {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> ordersByStatus() {
        Map<OrderStatus, Long> map = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            map.put(status, orderRepository.countByStatus(status));
        }
        return map;
    }

    @Transactional(readOnly = true)
    public List<Object[]> topFoods(int limit) {
        return orderItemRepository.topSelling(PageRequest.of(0, limit)).getContent();
    }

    /**
     * Penjualan per hari (bukan per timestamp order): hasil query digabung per
     * {@link LocalDate} lalu diurutkan menaik, sehingga satu tanggal hanya muncul satu baris.
     */
    @Transactional(readOnly = true)
    public List<Object[]> salesPerDay(int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<Object[]> raw = orderItemRepository.salesPerDay(start.atStartOfDay(), LocalDateTime.now());
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        for (Object[] row : raw) {
            LocalDate day = ((LocalDateTime) row[0]).toLocalDate();
            byDay.merge(day, (BigDecimal) row[1], BigDecimal::add);
        }
        return byDay.entrySet().stream()
                .map(e -> new Object[]{ e.getKey(), e.getValue() })
                .toList();
    }

    @Transactional(readOnly = true)
    public long activeCustomerCountSince(LocalDateTime since) {
        return orderRepository.countDistinctCustomersSince(since);
    }

    @Transactional(readOnly = true)
    public List<Object[]> findAllCustomers() {
        return userRepository.findAllCustomersWithOrderCount();
    }
}