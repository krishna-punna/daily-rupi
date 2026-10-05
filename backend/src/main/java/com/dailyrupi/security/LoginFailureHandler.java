package com.dailyrupi.security;

import java.io.IOException;

import com.dailyrupi.audit.AuditService;
import com.dailyrupi.common.ApiError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService attempts;
    private final AuditService audit;
    private final JsonResponseWriter json;

    public LoginFailureHandler(LoginAttemptService attempts, AuditService audit, JsonResponseWriter json) {
        this.attempts = attempts;
        this.audit = audit;
        this.json = json;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        String ip = request.getRemoteAddr();

        audit.record(username, AuditService.LOGIN_FAILURE, exception.getClass().getSimpleName(), ip);
        if (exception instanceof BadCredentialsException && attempts.recordFailure(username)) {
            audit.record(username, AuditService.ACCOUNT_LOCKED, null, ip);
        }

        // One message for every cause, so the response never reveals whether
        // the username exists, the password was wrong or the account is locked.
        json.write(response, HttpStatus.UNAUTHORIZED, new ApiError("LOGIN_FAILED",
                "Invalid username or password, or the account is temporarily locked"));
    }
}
