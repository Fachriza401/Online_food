package com.foodordering.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Nama lengkap wajib diisi")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Username wajib diisi")
    @Size(min = 3, max = 50, message = "Username minimal 3 karakter")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username hanya boleh huruf, angka, titik, garis bawah, dan strip")
    private String username;

    @NotBlank(message = "Email wajib diisi")
    @Email(message = "Format email tidak valid")
    private String email;

    @NotBlank(message = "Kata sandi wajib diisi")
    @Size(min = 6, max = 72, message = "Kata sandi minimal 6 karakter")
    private String password;

    @NotBlank(message = "Ulangi kata sandi wajib diisi")
    private String confirmPassword;

    @Size(max = 20, message = "Nomor telepon maksimal 20 karakter")
    private String phone;

    @Size(max = 255, message = "Alamat maksimal 255 karakter")
    private String address;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}