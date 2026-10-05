package com.dailyrupi.security;

import java.io.IOException;

import com.dailyrupi.audit.AuditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final LoginAttemptService attempts;
    private final AuditService audit;
    private final JsonResponseWriter json;

    public LoginSuccessHandler(LoginAttemptService attempts, AuditService audit, JsonResponseWriter json) {
        this.attempts = attempts;
        this.audit = audit;
        this.json = json;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        attempts.recordSuccess(authentication.getName());
        audit.record(authentication.getName(), AuditService.LOGIN_SUCCESS, null, request.getRemoteAddr());
        json.write(response, HttpStatus.OK, CurrentUser.from(authentication));
    }
}
