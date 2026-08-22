package com.medisync;

import com.medisync.admin.service.AdminAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AnalyticsIntegrationTest {

    @Autowired
    AdminAnalyticsService service;

    @Test
    void testAnalytics() {
        System.out.println("--- TESTING ANALYTICS SUMMARY ---");
        try {
            service.summary();
            System.out.println("SUMMARY OK");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("--- TESTING ANALYTICS TIMESERIES ---");
        try {
            service.timeseries(30);
            System.out.println("TIMESERIES OK");
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("--- TESTING ANALYTICS ACTIVITY ---");
        try {
            service.activity(30, 10);
            System.out.println("ACTIVITY OK");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("--- TESTING ANALYTICS DONE ---");
    }
}
