package com.foodordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodordering.model.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    void deleteByUserId(Long userId);
}