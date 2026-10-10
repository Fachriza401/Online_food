package com.foodordering.model;

import java.util.Arrays;
import java.util.List;

public enum PaymentMethod {
    COD("Bayar di Tempat", "cod",
            "Pembayaran dilakukan saat pesanan tiba. Tanpa pembayaran di muka."),

    GOPAY("GoPay", "ewallet", "Bayar instan lewat aplikasi Gojek."),
    OVO("OVO", "ewallet", "Bayar instan lewat aplikasi OVO."),
    DANA("DANA", "ewallet", "Bayar instan lewat aplikasi DANA."),
    SHOPEEPAY("ShopeePay", "ewallet", "Bayar instan lewat aplikasi Shopee."),
    LINKAJA("LinkAja", "ewallet", "Bayar instan lewat aplikasi LinkAja."),

    BCA("Bank BCA", "bank", "Transfer atau Virtual Account BCA."),
    MANDIRI("Bank Mandiri", "bank", "Transfer atau Virtual Account Mandiri."),
    BNI("Bank BNI", "bank", "Transfer atau Virtual Account BNI."),
    BRI("Bank BRI", "bank", "Transfer atau Virtual Account BRI.");

    private final String label;
    private final String group;
    private final String description;

    PaymentMethod(String label, String group, String description) {
        this.label = label;
        this.group = group;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getGroup() {
        return group;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEwallet() {
        return "ewallet".equals(group);
    }

    public boolean isBank() {
        return "bank".equals(group);
    }

    /** Metode digital (e-wallet & bank) yang butuh langkah pembayaran. */
    public boolean isDigital() {
        return isEwallet() || isBank();
    }

    public static List<PaymentMethod> byGroup(String group) {
        return Arrays.stream(values())
                .filter(m -> m.group.equals(group))
                .toList();
    }
}
