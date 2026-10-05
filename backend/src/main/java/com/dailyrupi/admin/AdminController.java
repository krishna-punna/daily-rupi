package com.dailyrupi.admin;

import java.util.List;

import com.dailyrupi.audit.AuditLog;
import com.dailyrupi.audit.AuditLogRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Protected twice: by the /api/admin/** URL rule and by the method-level check here. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AuditLogRepository auditLog;

    public AdminController(AuditLogRepository auditLog) {
        this.auditLog = auditLog;
    }

    @GetMapping("/audit-log")
    public List<AuditLog> auditLog(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return auditLog.findAll(PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id")))
                .getContent();
    }
}
