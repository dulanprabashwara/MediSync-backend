package com.medisync.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MediaUrlService {

    private static final Logger log = LoggerFactory.getLogger(MediaUrlService.class);
    private final MediaStorageService storageService;

    public MediaUrlService(MediaStorageService storageService) {
        this.storageService = storageService;
    }

    public String signedUrlOrNull(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || !storageService.configured()) {
            return null;
        }
        try {
            return storageService.createSignedUrl(storageKey);
        } catch (MediaStorageUnavailableException exception) {
            log.warn("A private media URL could not be generated");
            return null;
        }
    }
}
