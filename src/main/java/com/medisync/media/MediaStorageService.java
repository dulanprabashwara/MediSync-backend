package com.medisync.media;

public interface MediaStorageService {
    boolean configured();
    void upload(String storageKey, ValidatedImage image);
    void delete(String storageKey);
    String createSignedUrl(String storageKey);
}
