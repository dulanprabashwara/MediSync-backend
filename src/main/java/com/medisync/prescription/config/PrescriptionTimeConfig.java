package com.medisync.prescription.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PrescriptionTimeConfig {

    @Bean
    Clock prescriptionClock() {
        return Clock.systemUTC();
    }
}
