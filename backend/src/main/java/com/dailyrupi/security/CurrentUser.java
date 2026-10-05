package com.dailyrupi.security;

import java.util.List;

import com.dailyrupi.user.Roles;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** What the frontend is told about the logged-in account. Never includes the password hash. */
public record CurrentUser(String username, List<String> roles, boolean passwordChangeRequired) {

    public static CurrentUser from(Authentication authentication) {
        List<String> all = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .toList();
        boolean changeRequired = all.contains(Roles.PASSWORD_CHANGE_REQUIRED);
        List<String> roles = all.stream().filter(r -> !r.equals(Roles.PASSWORD_CHANGE_REQUIRED)).toList();
        return new CurrentUser(authentication.getName(), roles, changeRequired);
    }
}
