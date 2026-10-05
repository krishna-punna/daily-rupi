package com.dailyrupi.user;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the default account (admin / admin) when the users table is empty.
 * The password is hashed here rather than shipped in a migration, and the
 * account is flagged so nothing works until the password has been replaced.
 */
@Component
public class DefaultAdminInitializer implements ApplicationRunner {

    public static final String DEFAULT_USERNAME = "admin";
    public static final String DEFAULT_PASSWORD = "admin";

    private static final Logger log = LoggerFactory.getLogger(DefaultAdminInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DefaultAdminInitializer(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureDefaultAdmin();
    }

    @Transactional
    public void ensureDefaultAdmin() {
        if (users.count() > 0) {
            return;
        }
        User admin = new User(DEFAULT_USERNAME, passwordEncoder.encode(DEFAULT_PASSWORD),
                Set.of(Roles.USER, Roles.ADMIN));
        admin.setPasswordChangeRequired(true);
        users.save(admin);
        log.warn("Created default account '{}' with a temporary password. "
                + "Log in and change it; the app stays locked until you do.", DEFAULT_USERNAME);
    }
}
