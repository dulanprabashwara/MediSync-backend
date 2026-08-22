package com.medisync.admin.dto;

import com.medisync.audit.dto.AuditEventResponse;

import java.util.List;
import java.util.Map;

public record AdminUserDetail(
        AdminUserSummary user,
        Map<String, Object> roleProfile,
        Map<String, Long> operationalCounts,
        List<AdminBanHistoryResponse> banHistory,
        List<AuditEventResponse> recentActivity,
        Map<String, Object> deletionMetadata
) {
}
