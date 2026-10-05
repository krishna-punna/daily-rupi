package com.dailyrupi.security;

import com.dailyrupi.audit.AuditService;
import com.dailyrupi.common.ApiError;
import com.dailyrupi.config.SecurityProperties;
import com.dailyrupi.user.Roles;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data:",
            "font-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @Bean
    public PasswordEncoder passwordEncoder(SecurityProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler,
            JsonResponseWriter json,
            AuditService audit) throws Exception {

        CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokens.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));

        http
            // Deny by default: anything not listed below is refused.
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole(Roles.ADMIN)
                .requestMatchers("/api/**").hasRole(Roles.USER)
                .anyRequest().denyAll())

            // Username and password are checked against the users table by
            // DatabaseUserDetailsService and the BCrypt password encoder.
            .formLogin(login -> login
                .loginProcessingUrl("/api/auth/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler))

            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(logoutSuccessHandler(audit)))

            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(authenticationEntryPoint(json))
                .accessDeniedHandler(accessDeniedHandler(json)))

            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokens)
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))

            // New session ID at login, so a pre-login ID is useless to an attacker.
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId()))

            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                .permissionsPolicyHeader(permissions -> permissions
                    .policy("camera=(), microphone=(), geolocation=(), payment=()")))

            // No cross-origin access: Angular reaches the API through its dev
            // proxy, and later from the same origin.
            .httpBasic(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private static AuthenticationEntryPoint authenticationEntryPoint(JsonResponseWriter json) {
        return (request, response, exception) -> json.write(response, HttpStatus.UNAUTHORIZED,
                new ApiError("UNAUTHENTICATED", "Please log in"));
    }

    private static AccessDeniedHandler accessDeniedHandler(JsonResponseWriter json) {
        return (request, response, exception) -> {
            ApiError body;
            if (exception instanceof CsrfException) {
                body = new ApiError("CSRF_TOKEN_INVALID", "Security token missing or expired, please retry");
            } else if (hasPasswordChangeRole()) {
                body = new ApiError("PASSWORD_CHANGE_REQUIRED", "Change your temporary password to continue");
            } else {
                body = new ApiError("FORBIDDEN", "You do not have permission to do this");
            }
            json.write(response, HttpStatus.FORBIDDEN, body);
        };
    }

    private static boolean hasPasswordChangeRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Roles.PASSWORD_CHANGE_REQUIRED));
    }

    private static LogoutSuccessHandler logoutSuccessHandler(AuditService audit) {
        return (request, response, authentication) -> {
            if (authentication != null) {
                audit.record(authentication.getName(), AuditService.LOGOUT, null, request.getRemoteAddr());
            }
            response.setStatus(HttpStatus.NO_CONTENT.value());
        };
    }
}
