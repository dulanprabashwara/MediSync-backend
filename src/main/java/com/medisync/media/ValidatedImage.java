package com.medisync.media;

public record ValidatedImage(
        byte[] bytes,
        String contentType,
        String extension,
        String originalFilename
) {
}
