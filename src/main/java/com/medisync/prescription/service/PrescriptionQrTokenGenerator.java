package com.medisync.prescription.service;

import com.medisync.exception.ResourceConflictException;
import com.medisync.prescription.repository.PrescriptionQrTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PrescriptionQrTokenGenerator {

    public static final String PAYLOAD_PREFIX = "MEDISYNC:RX:";
    private static final int TOKEN_BYTES = 32;
    private static final int MAX_ATTEMPTS = 5;

    private final SecureRandom secureRandom;
    private final PrescriptionQrTokenRepository tokenRepository;

    @Autowired
    public PrescriptionQrTokenGenerator(PrescriptionQrTokenRepository tokenRepository) {
        this(tokenRepository, new SecureRandom());
    }

    PrescriptionQrTokenGenerator(PrescriptionQrTokenRepository tokenRepository, SecureRandom secureRandom) {
        this.tokenRepository = tokenRepository;
        this.secureRandom = secureRandom;
    }

    public String generateUniqueToken() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            byte[] bytes = new byte[TOKEN_BYTES];
            secureRandom.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (!tokenRepository.existsByToken(token)) {
                return token;
            }
        }
        throw new ResourceConflictException("A secure prescription QR token could not be generated; please retry");
    }

    public String payload(String token) {
        return PAYLOAD_PREFIX + token;
    }
}
