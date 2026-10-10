package com.foodordering.dto;

import com.foodordering.model.PaymentMethod;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CheckoutRequest {

    @NotBlank(message = "Nama penerima wajib diisi")
    @Size(max = 100)
    private String customerName;

    @NotBlank(message = "Nomor telepon wajib diisi")
    @Size(max = 20)
    @Pattern(regexp = "^[0-9+\\- ]+$", message = "Nomor telepon tidak valid")
    private String customerPhone;

    @NotBlank(message = "Alamat pengiriman wajib diisi")
    @Size(max = 500)
    private String customerAddress;

    @Size(max = 500)
    private String note;

    @NotNull(message = "Pilih metode pembayaran")
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}