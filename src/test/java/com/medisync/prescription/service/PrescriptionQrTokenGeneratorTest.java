package com.medisync.prescription.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionQrTokenGeneratorTest {

    @Test
    void createsOpaque256BitUrlSafeTokenAndMinimalPayload() {
        PrescriptionQrTokenGenerator generator = new PrescriptionQrTokenGenerator(new SecureRandom());
        String first = generator.generateToken();
        String second = generator.generateToken();

        assertThat(first).hasSize(43).matches("[A-Za-z0-9_-]+").isNotEqualTo(second);
        assertThat(generator.payload(first)).isEqualTo("MEDISYNC:RX:" + first)
                .doesNotContain("patient", "doctor", "medicine", "@");
    }

    @Test
    void deterministicHasherProducesLowercaseSha256WithoutRawToken() {
        String raw = new PrescriptionQrTokenGenerator(
                new SecureRandom("phase4-hardening".getBytes(StandardCharsets.UTF_8))).generateToken();
        PrescriptionQrTokenHasher hasher = new PrescriptionQrTokenHasher();

        String hash = hasher.hash(raw);

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}").doesNotContain(raw);
        assertThat(hasher.hash(raw)).isEqualTo(hash);
    }
}
