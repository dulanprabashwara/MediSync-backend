package com.medisync.prescription.service;

import com.medisync.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.regex.Pattern;

@Component
public class PrescriptionQrPayloadParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{43}$");

    public String parseToken(String payload) {
        if (payload == null) {
            throw invalid();
        }
        String normalized = payload.trim();
        if (!normalized.startsWith(PrescriptionQrTokenGenerator.PAYLOAD_PREFIX)) {
            throw invalid();
        }
        String token = normalized.substring(PrescriptionQrTokenGenerator.PAYLOAD_PREFIX.length());
        if (!TOKEN_PATTERN.matcher(token).matches()) {
            throw invalid();
        }
        try {
            if (Base64.getUrlDecoder().decode(token).length != 32) {
                throw invalid();
            }
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
        return token;
    }

    private InvalidRequestException invalid() {
        return new InvalidRequestException("Prescription QR is invalid or no longer usable");
    }
}
