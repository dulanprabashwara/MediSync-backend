package com.medisync.admin.controller;

import com.medisync.audit.dto.AuditEventResponse;
import com.medisync.audit.service.AuditService;
import com.medisync.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping({"/api/admin/audit-logs", "/api/admin/audit-events"})
public class AdminAuditController {
    private final AuditService auditService;

    public AdminAuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Page<AuditEventResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false) UserRole actorRole,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) UUID targetUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        UUID effectiveTargetId = targetUserId == null ? targetId : targetUserId;
        return auditService.search(page, size, action, actorUserId, actorRole, targetType,
                effectiveTargetId, from, to);
    }
}
