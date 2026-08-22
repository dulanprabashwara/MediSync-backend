package com.medisync.admin.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAnalyticsQueryContractTest {

    @Test
    void analyticsUseDatabaseCountsGroupingAndBoundedRankings() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/medisync/admin/service/AdminAnalyticsService.java"));

        assertThat(source)
                .contains("COUNT(*)", "GROUP BY", "LIMIT ?", "AT TIME ZONE 'UTC'")
                .doesNotContain("findAll(");
    }
}
