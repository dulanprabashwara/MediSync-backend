package com.medisync.admin.dto;

import java.util.List;

public record AdminActivityResponse(
        List<AdminActivityRanking> busiestDoctors,
        List<AdminActivityRanking> mostActivePatients,
        List<AdminActivityRanking> busiestPharmacists
) {
}
