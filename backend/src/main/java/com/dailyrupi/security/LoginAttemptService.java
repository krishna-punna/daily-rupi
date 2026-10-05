package com.dailyrupi.security;

import java.time.LocalDateTime;

import com.dailyrupi.config.SecurityProperties;
import com.dailyrupi.user.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Counts failed logins and locks the account for a while after too many. */
@Service
public class LoginAttemptService {

    private final UserRepository users;
    private final SecurityProperties properties;

    public LoginAttemptService(UserRepository users, SecurityProperties properties) {
        this.users = users;
        this.properties = properties;
    }

    /** @return true if this failure locked the account */
    @Transactional
    public boolean recordFailure(String username) {
        return users.findByUsername(DatabaseUserDetailsService.normalize(username)).map(user -> {
            int attempts = user.getFailedAttempts() + 1;
            if (attempts >= properties.maxFailedAttempts()) {
                user.setFailedAttempts(0);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(properties.lockMinutes()));
                return true;
            }
            user.setFailedAttempts(attempts);
            return false;
        }).orElse(false);
    }

    @Transactional
    public void recordSuccess(String username) {
        users.findByUsername(DatabaseUserDetailsService.normalize(username)).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        });
    }
}
