package com.foodordering.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        current.setFullName(form.getFullName());
        current.setPhone(form.getPhone());
        current.setAddress(form.getAddress());
        if (newAvatarUrl != null) {
            if (current.getAvatarUrl() != null) {
                fileStorageService.deleteIfUploaded(current.getAvatarUrl());
            }
            current.setAvatarUrl(newAvatarUrl);
        }
        userRepository.save(current);
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