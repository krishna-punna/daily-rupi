package com.dailyrupi.security;

import java.util.List;
import java.util.Locale;

import com.dailyrupi.user.Roles;
import com.dailyrupi.user.User;
import com.dailyrupi.user.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the account from the MySQL {@code users} table. Spring Security then
 * compares the submitted password with the stored BCrypt hash.
 */
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    public static String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = users.findByUsername(normalize(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // A temporary password grants no real role: only the account
        // endpoints work until it has been changed.
        List<SimpleGrantedAuthority> authorities = user.isPasswordChangeRequired()
                ? List.of(new SimpleGrantedAuthority("ROLE_" + Roles.PASSWORD_CHANGE_REQUIRED))
                : user.getRoles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .disabled(!user.isEnabled())
                .accountLocked(user.isLocked())
                .build();
    }
}
