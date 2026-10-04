package com.foodordering.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodordering.model.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndFoodId(Long cartId, Long foodId);

    List<CartItem> findAllByCartIdOrderByIdAsc(Long cartId);

    void deleteByCartId(Long cartId);
}