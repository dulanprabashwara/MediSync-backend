package com.medisync;

import com.medisync.admin.service.AdminAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.hikari.maximum-pool-size=2"
})
public class AnalyticsDebugQueryTest {

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @Test
    public void runQueries() {
        System.out.println("--- STARTING QUERY DEBUG ---");
        try {
            System.out.println("Summary:");
            adminAnalyticsService.summary();
            System.out.println("Summary SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Timeseries:");
            adminAnalyticsService.timeseries(30);
            System.out.println("Timeseries SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Activity:");
            adminAnalyticsService.activity(30, 10);
            System.out.println("Activity SUCCESS");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("--- END QUERY DEBUG ---");
    }
}
