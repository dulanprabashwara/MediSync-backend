package com.medisync.prescription.service;

import com.medisync.prescription.repository.PrescriptionQrTokenRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrescriptionQrTokenGeneratorTest {
    @Test
    void createsOpaque256BitUrlSafeTokenAndMinimalPayload() {
        PrescriptionQrTokenRepository repository = mock(PrescriptionQrTokenRepository.class);
        when(repository.existsByToken(anyString())).thenReturn(false);
        PrescriptionQrTokenGenerator generator = new PrescriptionQrTokenGenerator(repository, new SecureRandom());
        String first = generator.generateUniqueToken();
        String second = generator.generateUniqueToken();
        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+").isNotEqualTo(second);
        assertThat(generator.payload(first)).isEqualTo("MEDISYNC:RX:" + first)
                .doesNotContain("patient", "doctor", "medicine", "@");
    }

    @Test
    void retriesRepositoryCollisionWithoutWeakeningToken() {
        PrescriptionQrTokenRepository repository = mock(PrescriptionQrTokenRepository.class);
        when(repository.existsByToken(anyString())).thenReturn(true, false);
        SecureRandom deterministic = new SecureRandom("phase4".getBytes(StandardCharsets.UTF_8));
        String token = new PrescriptionQrTokenGenerator(repository, deterministic).generateUniqueToken();
        assertThat(token).hasSize(43);
    }
}
