package com.medisync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medisync.security")
public record SecurityProperties(String supabaseUrl, String jwksUrl, String audience) {
    public String issuer() {
        return supabaseUrl.replaceAll("/+$", "") + "/auth/v1";
    }
}
