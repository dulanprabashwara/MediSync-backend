package com.medisync.media;

import com.medisync.config.MediaStorageProperties;
import com.medisync.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadValidatorTest {

    private final ImageUploadValidator validator = new ImageUploadValidator(
            new MediaStorageProperties("", "medisync-private", 300, 5 * 1024 * 1024L));

    @Test
    void acceptsSupportedImageWhenMimeAndMagicBytesMatch() {
        byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
        ValidatedImage image = validator.validate(new MockMultipartFile(
                "file", "portrait.jpg", "image/jpeg", jpeg));

        assertThat(image.contentType()).isEqualTo("image/jpeg");
        assertThat(image.extension()).isEqualTo("jpg");
        assertThat(image.originalFilename()).isEqualTo("portrait.jpg");
    }

    @Test
    void rejectsSpoofedImageMimeType() {
        MockMultipartFile spoofed = new MockMultipartFile(
                "file", "not-really.png", "image/png", "plain text".getBytes());

        assertThatThrownBy(() -> validator.validate(spoofed))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("does not match");
    }

    @Test
    void rejectsUnsupportedTypesBeforeStorage() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", "%PDF".getBytes());

        assertThatThrownBy(() -> validator.validate(pdf))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("JPEG, PNG, and WebP");
    }
}
