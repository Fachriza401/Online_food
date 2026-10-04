package com.foodordering.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodordering.model.Notification;
import com.foodordering.model.User;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findTop10ByUserOrderByIdDesc(User user);

    List<Notification> findTop10ByUserAndReadFalse(User user);

    long countByUserAndReadFalse(User user);
}