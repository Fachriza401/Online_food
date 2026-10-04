package com.foodordering.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodordering.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    default void saveIfAbsent(String name) {
        if (!existsByName(name)) {
            save(new Role(name));
        }
    }
}