package com.medisync.media;

import com.medisync.config.MediaStorageProperties;
import com.medisync.exception.InvalidRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Component
public class ImageUploadValidator {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final MediaStorageProperties properties;

    public ImageUploadValidator(MediaStorageProperties properties) {
        this.properties = properties;
    }

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("An image file is required");
        }
        long maxBytes = properties.maxImageBytes() > 0 ? properties.maxImageBytes() : 5 * 1024 * 1024L;
        if (file.getSize() > maxBytes) {
            throw new InvalidRequestException("Images must be 5 MB or smaller");
        }

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new InvalidRequestException("Only JPEG, PNG, and WebP images are supported");
        }

        try {
            byte[] bytes = file.getBytes();
            if (!matchesMagic(contentType, bytes)) {
                throw new InvalidRequestException("The uploaded file content does not match its image type");
            }
            return new ValidatedImage(bytes, contentType, extension, safeFilename(file.getOriginalFilename()));
        } catch (IOException exception) {
            throw new InvalidRequestException("The uploaded image could not be read");
        }
    }

    private boolean matchesMagic(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case "image/png" -> bytes.length >= 8
                    && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                    && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a
                    && bytes[6] == 0x1a && bytes[7] == 0x0a;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String normalized = filename.replace('\\', '/');
        String base = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        return base.isEmpty() ? null : base.substring(0, Math.min(base.length(), 255));
    }
}
