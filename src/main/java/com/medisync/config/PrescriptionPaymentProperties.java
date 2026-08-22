package com.medisync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medisync.prescriptions.payment")
public record PrescriptionPaymentProperties(String currency) {
    public String normalizedCurrency() {
        String value = currency == null ? "LKR" : currency.trim().toUpperCase();
        return value.matches("[A-Z]{3}") ? value : "LKR";
    }
}
