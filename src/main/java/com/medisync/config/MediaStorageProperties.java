package com.medisync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "medisync.media")
public record MediaStorageProperties(
        String serviceRoleKey,
        String bucket,
        int signedUrlSeconds,
        long maxImageBytes
) {
    public boolean configured() {
        return serviceRoleKey != null && !serviceRoleKey.isBlank()
                && bucket != null && !bucket.isBlank();
    }
}
