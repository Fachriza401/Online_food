package com.foodordering.model;

public enum PaymentStatus {
    PENDING("Belum Dibayar", "badge-pending"),
    PAID("Lunas", "badge-paid"),
    CANCELLED("Dibatalkan", "badge-cancelled");

    private final String label;
    private final String badgeClass;

    PaymentStatus(String label, String badgeClass) {
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