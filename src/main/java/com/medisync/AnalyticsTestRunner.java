package com.medisync;

import com.medisync.admin.service.AdminAnalyticsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsTestRunner implements CommandLineRunner {
    private final AdminAnalyticsService service;

    public AnalyticsTestRunner(AdminAnalyticsService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
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
