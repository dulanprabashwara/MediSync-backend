package com.medisync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medisync.appointments")
public record AppointmentProperties(int minimumLeadMinutes) {
    public AppointmentProperties {
        if (minimumLeadMinutes < 0) {
            throw new IllegalArgumentException("Appointment minimum lead time cannot be negative");
        }
    }
}
