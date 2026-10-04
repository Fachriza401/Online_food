package com.foodordering.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.foodordering.dto.RegisterRequest;
import com.foodordering.exception.BadRequestException;
import com.foodordering.model.Role;
import com.foodordering.model.User;
import com.foodordering.model.UserRole;
import com.foodordering.repository.RoleRepository;
import com.foodordering.repository.UserRepository;
import com.foodordering.repository.UserRoleRepository;
import com.foodordering.security.CustomUserDetailsService;
import com.foodordering.security.UserPrincipal;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CustomUserDetailsService userDetailsService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository, CustomUserDetailsService userDetailsService,
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();

        if (userRepository.existsByUsername(username)) {
            throw new BadRequestException("Username sudah digunakan");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email sudah terdaftar");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Konfirmasi kata sandi tidak sama");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        Role customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Role ROLE_CUSTOMER belum tersedia"));
        userRoleRepository.save(new UserRole(user, customerRole));
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadPrincipalByUsername(String username) {
        try {
            return (UserPrincipal) userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }
}