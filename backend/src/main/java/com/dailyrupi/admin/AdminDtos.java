package com.dailyrupi.admin;

import java.time.LocalDateTime;

import com.dailyrupi.audit.AuditLog;

public final class AdminDtos {

    private AdminDtos() {
    }

    /**
     * One audit-log row as the API shows it. The field names match what the endpoint returned when it
     * exposed the AuditLog entity directly, so callers see no change. A field added to the entity later
     * stays out of the response until it is added here on purpose.
     */
    public record AuditEntry(Long id, String username, String event, String detail, String ipAddress,
            LocalDateTime createdAt) {

        static AuditEntry from(AuditLog log) {
            return new AuditEntry(log.getId(), log.getUsername(), log.getEvent(), log.getDetail(),
                    log.getIpAddress(), log.getCreatedAt());
        }
    }
}
