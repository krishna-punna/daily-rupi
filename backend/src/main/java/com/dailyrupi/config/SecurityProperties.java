package com.dailyrupi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tunable login-protection settings, see {@code dailyrupi.security} in application.yml. */
@ConfigurationProperties(prefix = "dailyrupi.security")
public record SecurityProperties(int maxFailedAttempts, int lockMinutes, int bcryptStrength) {

    public SecurityProperties {
        if (maxFailedAttempts <= 0) maxFailedAttempts = 5;
        if (lockMinutes <= 0) lockMinutes = 15;
        if (bcryptStrength < 10) bcryptStrength = 12;
    }
}
