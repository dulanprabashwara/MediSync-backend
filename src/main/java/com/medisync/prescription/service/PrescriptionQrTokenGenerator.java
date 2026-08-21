package com.medisync.prescription.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PrescriptionQrTokenGenerator {

    public static final String PAYLOAD_PREFIX = "MEDISYNC:RX:";
    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom;

    public PrescriptionQrTokenGenerator() {
        this(new SecureRandom());
    }

    PrescriptionQrTokenGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String payload(String token) {
        return PAYLOAD_PREFIX + token;
    }
}
