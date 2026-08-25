package com.medisync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medisync.livekit")
public record LiveKitProperties(String url, String apiKey, String apiSecret) {
}
