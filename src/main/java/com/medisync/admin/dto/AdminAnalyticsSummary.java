package com.medisync.admin.dto;

import java.util.Map;

public record AdminAnalyticsSummary(
        Map<String, Long> usersByRole,
        Map<String, Long> usersByStatus,
        Map<String, Long> professionalVerification,
        Map<String, Long> appointmentsByStatus,
        Map<String, Long> consultationsByStatus,
        Map<String, Long> prescriptionsByStatus,
        Map<String, Long> doctorFeesByStatus,
        long totalDispensations,
        long totalChatMessages,
        long totalChatImages
) {
}
