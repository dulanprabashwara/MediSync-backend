package com.medisync.admin.dto;

import java.time.LocalDate;

public record AdminAnalyticsPoint(
        LocalDate date,
        long newUsers,
        long appointments,
        long consultations,
        long prescriptions,
        long dispensations,
        long chatMessages
) {
}
