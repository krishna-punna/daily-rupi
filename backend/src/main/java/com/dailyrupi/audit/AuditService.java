package com.dailyrupi.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String LOGOUT = "LOGOUT";
    public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** Own transaction, so an audit entry survives even if the caller rolls back. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String username, String event, String detail, String ipAddress) {
        repository.save(new AuditLog(truncate(username, 50), event, truncate(detail, 255), ipAddress));
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        // Strip line breaks so user-supplied text cannot forge log lines.
        String clean = value.replaceAll("[\\r\\n\\t]", " ");
        return clean.length() <= max ? clean : clean.substring(0, max);
    }
}
