package com.foodordering.model;

public enum PaymentMethod {
    COD("Bayar di Tempat");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}