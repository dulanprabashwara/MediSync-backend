package com.medisync.admin.controller;

import com.medisync.admin.dto.AdminActivityResponse;
import com.medisync.admin.dto.AdminAnalyticsPoint;
import com.medisync.admin.dto.AdminAnalyticsSummary;
import com.medisync.admin.service.AdminAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {
    private final AdminAnalyticsService service;

    public AdminAnalyticsController(AdminAnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public AdminAnalyticsSummary summary() {
        return service.summary();
    }

    @GetMapping("/timeseries")
    public List<AdminAnalyticsPoint> timeseries(@RequestParam(defaultValue = "30") int days) {
        return service.timeseries(days);
    }

    @GetMapping({"/user-activity", "/activity"})
    public AdminActivityResponse activity(@RequestParam(defaultValue = "30") int days,
                                          @RequestParam(defaultValue = "10") int limit) {
        return service.activity(days, limit);
    }
}
