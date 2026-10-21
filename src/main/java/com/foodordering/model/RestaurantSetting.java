package com.foodordering.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurant_settings")
public class RestaurantSetting {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "restaurant_name", nullable = false, length = 100)
    private String restaurantName = "RM KAK ROS";

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(length = 255)
    private String address;

    @Column(length = 30)
    private String phone;

    @Column(length = 120)
    private String email;

    @Column(length = 500)
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** Logo dinamis, atau logo bawaan bila admin belum mengunggah logo. */
    public String resolveLogoUrl() {
        return (logoUrl != null && !logoUrl.isBlank()) ? logoUrl : "/images/branding/logo.png?v=2";
    }

    public String resolveName() {
        return (restaurantName != null && !restaurantName.isBlank()) ? restaurantName : "RM KAK ROS";
    }
}
