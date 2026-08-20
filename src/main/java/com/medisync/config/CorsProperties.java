package com.medisync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medisync.cors")
public record CorsProperties(String frontendUrl) {
}
