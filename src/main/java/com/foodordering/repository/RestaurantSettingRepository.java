package com.foodordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.foodordering.model.RestaurantSetting;

public interface RestaurantSettingRepository extends JpaRepository<RestaurantSetting, Long> {
}
