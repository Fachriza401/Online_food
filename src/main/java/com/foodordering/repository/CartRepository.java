package com.foodordering.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodordering.model.Cart;
import com.foodordering.model.User;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    Optional<Cart> findByUser(User user);
}