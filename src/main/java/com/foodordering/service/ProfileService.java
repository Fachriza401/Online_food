package com.foodordering.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.foodordering.exception.BadRequestException;
import com.foodordering.model.User;
import com.foodordering.repository.UserRepository;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public ProfileService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public void update(User current, User form, String newAvatarUrl) {
        String fullName = form.getFullName() == null ? "" : form.getFullName().trim();
        String phone = form.getPhone() == null ? "" : form.getPhone().trim();
        String address = form.getAddress() == null ? "" : form.getAddress().trim();
        if (fullName.isEmpty()) {
            throw new BadRequestException("Nama lengkap wajib diisi");
        }
        if (fullName.length() > 100 || phone.length() > 20 || address.length() > 255) {
            throw new BadRequestException("Ukuran data profil melebihi batas (nama 100, telepon 20, alamat 255 karakter)");
        }
        current.setFullName(fullName);
        current.setPhone(phone);
        current.setAddress(address);
        String oldAvatarUrl = null;
        if (newAvatarUrl != null) {
            oldAvatarUrl = current.getAvatarUrl();
            current.setAvatarUrl(newAvatarUrl);
        }
        userRepository.save(current);
        // File lama hanya dihapus setelah transaksi benar-benar ter-commit,
        // agar tidak kehilangan foto bila penyimpanan ke database gagal.
        if (oldAvatarUrl != null) {
            deleteAfterCommit(oldAvatarUrl);
        }
    }

    private void deleteAfterCommit(String imageUrl) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileStorageService.deleteIfUploaded(imageUrl);
                }
            });
        } else {
            fileStorageService.deleteIfUploaded(imageUrl);
        }
    }

    @Transactional
    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadRequestException("Kata sandi lama tidak sesuai");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BadRequestException("Kata sandi baru minimal 6 karakter");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}