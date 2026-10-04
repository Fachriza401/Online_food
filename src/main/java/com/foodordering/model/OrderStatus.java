package com.foodordering.model;

public enum OrderStatus {
    PENDING("Menunggu Konfirmasi", "badge-pending"),
    CONFIRMED("Dikonfirmasi", "badge-confirmed"),
    PREPARING("Sedang Dimasak", "badge-preparing"),
    DELIVERING("Sedang Diantar", "badge-delivering"),
    COMPLETED("Selesai", "badge-completed"),
    CANCELLED("Dibatalkan", "badge-cancelled");

    private final String label;
    private final String badgeClass;

    OrderStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}