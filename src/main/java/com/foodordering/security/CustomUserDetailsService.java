    package com.foodordering.security;

import java.util.Set;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.foodordering.model.User;
import com.foodordering.model.UserRole;
import com.foodordering.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan: " + username));
        return toPrincipal(user);
    }

    public UserPrincipal loadUserByEmail(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User tidak ditemukan: " + email));
        return toPrincipal(user);
    }

    private UserPrincipal toPrincipal(User user) {
        Set<String> roleNames = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getName())
                .collect(java.util.stream.Collectors.toSet());
        return new UserPrincipal(user, roleNames);
    }
}