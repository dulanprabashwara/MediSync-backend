package com.medisync.media;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medisync.config.MediaStorageProperties;
import com.medisync.config.SecurityProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class SupabaseMediaStorageService implements MediaStorageService {

    private final MediaStorageProperties properties;
    private final String supabaseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public SupabaseMediaStorageService(MediaStorageProperties properties,
                                       SecurityProperties securityProperties,
                                       ObjectMapper objectMapper) {
        this.properties = properties;
        this.supabaseUrl = stripTrailingSlash(securityProperties.supabaseUrl());
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public boolean configured() {
        return properties.configured() && supabaseUrl != null && !supabaseUrl.isBlank();
    }

    @Override
    public void upload(String storageKey, ValidatedImage image) {
        requireConfigured();
        HttpRequest request = baseRequest(objectUri(storageKey))
                .header("Content-Type", image.contentType())
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofByteArray(image.bytes()))
                .build();
        sendExpectSuccess(request, "The image could not be stored");
    }

    @Override
    public void delete(String storageKey) {
        if (!configured() || storageKey == null || storageKey.isBlank()) {
            return;
        }
        HttpRequest request = baseRequest(objectUri(storageKey)).DELETE().build();
        sendExpectSuccess(request, "The previous image could not be removed");
    }

    @Override
    public String createSignedUrl(String storageKey) {
        requireConfigured();
        int expires = properties.signedUrlSeconds() > 0 ? properties.signedUrlSeconds() : 300;
        HttpRequest request = baseRequest(signUri(storageKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"expiresIn\":" + expires + "}"))
                .build();
        String response = sendExpectSuccess(request, "A private image link could not be created");
        try {
            JsonNode json = objectMapper.readTree(response);
            String signedPath = json.path("signedURL").asText(json.path("signedUrl").asText());
            if (signedPath.isBlank()) {
                throw new MediaStorageUnavailableException("The storage provider returned an invalid signed link");
            }
            return signedPath.startsWith("http")
                    ? signedPath
                    : supabaseUrl + "/storage/v1" + (signedPath.startsWith("/") ? signedPath : "/" + signedPath);
        } catch (IOException exception) {
            throw new MediaStorageUnavailableException("The storage provider returned an invalid response", exception);
        }
    }

    private HttpRequest.Builder baseRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + properties.serviceRoleKey())
                .header("apikey", properties.serviceRoleKey());
    }

    private URI objectUri(String key) {
        return URI.create(supabaseUrl + "/storage/v1/object/" + encode(properties.bucket()) + "/" + encodePath(key));
    }

    private URI signUri(String key) {
        return URI.create(supabaseUrl + "/storage/v1/object/sign/" + encode(properties.bucket()) + "/" + encodePath(key));
    }

    private String sendExpectSuccess(HttpRequest request, String publicMessage) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new MediaStorageUnavailableException(publicMessage);
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MediaStorageUnavailableException(publicMessage, exception);
        } catch (IOException exception) {
            throw new MediaStorageUnavailableException(publicMessage, exception);
        }
    }

    private void requireConfigured() {
        if (!configured()) {
            throw new MediaStorageUnavailableException(
                    "Private media storage is not configured. Set SUPABASE_SERVICE_ROLE_KEY and SUPABASE_STORAGE_BUCKET."
            );
        }
    }

    private String encodePath(String path) {
        return Arrays.stream(path.split("/"))
                .map(this::encode)
                .collect(Collectors.joining("/"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String stripTrailingSlash(String value) {
        if (value == null) return null;
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
