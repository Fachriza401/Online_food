package com.foodordering.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.foodordering.model.User;
import com.foodordering.model.UserRole;

public class UserPrincipal implements UserDetails {

    private final User user;
    private final Set<String> roles;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user, Set<String> roles) {
        this.user = user;
        this.roles = roles;
        this.authorities = new java.util.ArrayList<>(roles.stream()
                .map(SimpleGrantedAuthority::new).toList());
    }

    public User getUser() {
        return user;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}