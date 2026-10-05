package com.dailyrupi.auth;

import com.dailyrupi.audit.AuditService;
import com.dailyrupi.security.CurrentUser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login (POST /api/auth/login) and logout (POST /api/auth/logout) are handled
 * by Spring Security's own filters, configured in SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PasswordService passwords;
    private final AuditService audit;

    public AuthController(PasswordService passwords, AuditService audit) {
        this.passwords = passwords;
        this.audit = audit;
    }

    /** Public and empty: calling it makes the server issue the XSRF-TOKEN cookie before login. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public CurrentUser me(Authentication authentication) {
        return CurrentUser.from(authentication);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body,
            Authentication authentication, HttpServletRequest request) {
        passwords.changePassword(authentication.getName(), body.currentPassword(), body.newPassword());
        audit.record(authentication.getName(), AuditService.PASSWORD_CHANGED, null, request.getRemoteAddr());

        // End the session: the old password's login must not outlive it, and
        // logging in again picks up the account's real roles.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
