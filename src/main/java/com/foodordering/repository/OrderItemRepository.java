package com.foodordering.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.foodordering.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi.foodName AS nama, SUM(oi.quantity) AS terjual, SUM(oi.subtotal) AS omzet "
            + "FROM OrderItem oi GROUP BY oi.foodName ORDER BY terjual DESC")
    Page<Object[]> topSelling(Pageable pageable);

    @Query("SELECT oi.order.createdAt AS tgl, SUM(oi.subtotal) AS total "
            + "FROM OrderItem oi WHERE oi.order.status <> com.foodordering.model.OrderStatus.CANCELLED "
            + "AND oi.order.createdAt BETWEEN :start AND :end "
            + "GROUP BY oi.order.createdAt ORDER BY oi.order.createdAt ASC")
    List<Object[]> salesPerDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi "
            + "WHERE oi.order.user.id = :userId AND oi.food.id = :foodId "
            + "AND oi.order.status = com.foodordering.model.OrderStatus.COMPLETED")
    boolean existsCompletedPurchase(@Param("userId") Long userId, @Param("foodId") Long foodId);

    boolean existsByFoodId(Long foodId);
}