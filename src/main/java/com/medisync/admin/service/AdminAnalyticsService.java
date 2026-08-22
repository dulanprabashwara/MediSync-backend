package com.medisync.admin.service;

import com.medisync.admin.dto.AdminActivityRanking;
import com.medisync.admin.dto.AdminActivityResponse;
import com.medisync.admin.dto.AdminAnalyticsPoint;
import com.medisync.admin.dto.AdminAnalyticsSummary;
import com.medisync.exception.InvalidRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    public AdminAnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsSummary summary() {
        return new AdminAnalyticsSummary(
                grouped("SELECT role AS label, COUNT(*) AS total FROM app_users GROUP BY role"),
                grouped("SELECT status AS label, COUNT(*) AS total FROM app_users GROUP BY status"),
                professionalVerification(),
                grouped("SELECT status AS label, COUNT(*) AS total FROM appointments GROUP BY status"),
                grouped("SELECT status AS label, COUNT(*) AS total FROM consultation_sessions GROUP BY status"),
                grouped("SELECT status AS label, COUNT(*) AS total FROM prescriptions GROUP BY status"),
                grouped("SELECT doctor_fee_status AS label, COUNT(*) AS total FROM prescriptions GROUP BY doctor_fee_status"),
                scalar("SELECT COUNT(*) FROM prescription_dispensations"),
                scalar("SELECT COUNT(*) FROM consultation_messages"),
                scalar("SELECT COUNT(*) FROM consultation_message_attachments")
        );
    }

    @Transactional(readOnly = true)
    public List<AdminAnalyticsPoint> timeseries(int days) {
        validateDays(days);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstDay = today.minusDays(days - 1L);
        OffsetDateTime from = firstDay.atStartOfDay().atOffset(ZoneOffset.UTC);
        Map<LocalDate, MutablePoint> points = new LinkedHashMap<>();
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = firstDay.plusDays(offset);
            points.put(date, new MutablePoint(date));
        }
        fill(points, "SELECT (created_at AT TIME ZONE 'UTC')::date day, COUNT(*) total FROM app_users WHERE created_at >= ? GROUP BY day", from, Metric.USERS);
        fill(points, "SELECT (created_at AT TIME ZONE 'UTC')::date day, COUNT(*) total FROM appointments WHERE created_at >= ? GROUP BY day", from, Metric.APPOINTMENTS);
        fill(points, "SELECT (created_at AT TIME ZONE 'UTC')::date day, COUNT(*) total FROM consultation_sessions WHERE created_at >= ? GROUP BY day", from, Metric.CONSULTATIONS);
        fill(points, "SELECT (created_at AT TIME ZONE 'UTC')::date day, COUNT(*) total FROM prescriptions WHERE created_at >= ? GROUP BY day", from, Metric.PRESCRIPTIONS);
        fill(points, "SELECT (dispensed_at AT TIME ZONE 'UTC')::date day, COUNT(*) total FROM prescription_dispensations WHERE dispensed_at >= ? GROUP BY day", from, Metric.DISPENSATIONS);
        fill(points, "SELECT (sent_at AT TIME ZONE 'UTC')::date day, COUNT(*) total FROM consultation_messages WHERE sent_at >= ? GROUP BY day", from, Metric.MESSAGES);
        return points.values().stream().map(MutablePoint::response).toList();
    }

    @Transactional(readOnly = true)
    public AdminActivityResponse activity(int days, int limit) {
        validateDays(days);
        if (limit < 1 || limit > 25) throw new InvalidRequestException("Activity limit must be between 1 and 25");
        OffsetDateTime from = OffsetDateTime.now(ZoneOffset.UTC).minusDays(days);
        return new AdminActivityResponse(
                rankings("""
                        SELECT u.id, CONCAT(u.first_name, ' ', u.last_name) display_name, COUNT(a.id) total
                        FROM app_users u JOIN doctor_profiles d ON d.user_id = u.id
                        JOIN appointments a ON a.doctor_id = d.id AND a.created_at >= ?
                        GROUP BY u.id, u.first_name, u.last_name ORDER BY total DESC, display_name LIMIT ?
                        """, from, limit),
                rankings("""
                        SELECT u.id, CONCAT(u.first_name, ' ', u.last_name) display_name, COUNT(a.id) total
                        FROM app_users u JOIN patient_profiles p ON p.user_id = u.id
                        JOIN appointments a ON a.patient_id = p.id AND a.created_at >= ?
                        GROUP BY u.id, u.first_name, u.last_name ORDER BY total DESC, display_name LIMIT ?
                        """, from, limit),
                rankings("""
                        SELECT u.id, CONCAT(u.first_name, ' ', u.last_name) display_name, COUNT(x.id) total
                        FROM app_users u JOIN pharmacist_profiles p ON p.user_id = u.id
                        JOIN prescription_dispensations x ON x.pharmacist_id = p.id AND x.dispensed_at >= ?
                        GROUP BY u.id, u.first_name, u.last_name ORDER BY total DESC, display_name LIMIT ?
                        """, from, limit)
        );
    }

    private Map<String, Long> professionalVerification() {
        Map<String, Long> counts = new LinkedHashMap<>();
        grouped("SELECT 'DOCTOR_' || verification_status AS label, COUNT(*) total FROM doctor_profiles GROUP BY verification_status")
                .forEach(counts::put);
        grouped("SELECT 'PHARMACIST_' || verification_status AS label, COUNT(*) total FROM pharmacist_profiles GROUP BY verification_status")
                .forEach(counts::put);
        return counts;
    }

    private Map<String, Long> grouped(String sql) {
        Map<String, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (RowCallbackHandler) row ->
                result.put(row.getString("label"), row.getLong("total")));
        return result;
    }

    private long scalar(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private void fill(Map<LocalDate, MutablePoint> points, String sql, OffsetDateTime from, Metric metric) {
        jdbcTemplate.query(sql, row -> {
            LocalDate date = row.getObject("day", LocalDate.class);
            MutablePoint point = points.get(date);
            if (point != null) point.set(metric, row.getLong("total"));
        }, from);
    }

    private List<AdminActivityRanking> rankings(String sql, OffsetDateTime from, int limit) {
        return jdbcTemplate.query(sql, (row, index) -> new AdminActivityRanking(
                row.getObject("id", UUID.class), row.getString("display_name"), row.getLong("total")), from, limit);
    }

    private void validateDays(int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new InvalidRequestException("Analytics range must be 7, 30, or 90 days");
        }
    }

    private enum Metric { USERS, APPOINTMENTS, CONSULTATIONS, PRESCRIPTIONS, DISPENSATIONS, MESSAGES }

    private static final class MutablePoint {
        private final LocalDate date;
        private long users;
        private long appointments;
        private long consultations;
        private long prescriptions;
        private long dispensations;
        private long messages;

        private MutablePoint(LocalDate date) { this.date = date; }

        private void set(Metric metric, long value) {
            switch (metric) {
                case USERS -> users = value;
                case APPOINTMENTS -> appointments = value;
                case CONSULTATIONS -> consultations = value;
                case PRESCRIPTIONS -> prescriptions = value;
                case DISPENSATIONS -> dispensations = value;
                case MESSAGES -> messages = value;
            }
        }

        private AdminAnalyticsPoint response() {
            return new AdminAnalyticsPoint(date, users, appointments, consultations, prescriptions,
                    dispensations, messages);
        }
    }
}
