package com.medisync.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
public class SupabaseAuthDeletionService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthDeletionService.class);

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final RestTemplate restTemplate;

    public SupabaseAuthDeletionService(
            @Value("${medisync.security.supabase-url}") String supabaseUrl,
            @Value("${medisync.media.service-role-key}") String serviceRoleKey) {
        this.supabaseUrl = supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
        this.restTemplate = new RestTemplate();
    }

    public void deleteSupabaseAuthUser(UUID authUserId) {
        if (serviceRoleKey == null || serviceRoleKey.isBlank()) {
            throw new IllegalStateException("Supabase Auth deletion is not configured");
        }

        try {
            String url = supabaseUrl + "/auth/v1/admin/users/" + authUserId;
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(serviceRoleKey);
            headers.set("apikey", serviceRoleKey);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, request, String.class);
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Supabase Auth deletion returned status " + response.getStatusCode());
            }
            log.info("Successfully deleted Supabase Auth identity for {}", authUserId);
        } catch (Exception e) {
            log.error("Failed to delete Supabase Auth identity for {}: {}", authUserId, e.getMessage());
            throw new IllegalStateException("The authentication identity could not be deleted", e);
        }
    }
}
