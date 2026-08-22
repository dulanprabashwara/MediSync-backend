package com.medisync.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public record AdminAnalyticsPoint(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        long newUsers,
        long appointments,
        long consultations,
        long prescriptions,
        long dispensations,
        long chatMessages
) {
}
