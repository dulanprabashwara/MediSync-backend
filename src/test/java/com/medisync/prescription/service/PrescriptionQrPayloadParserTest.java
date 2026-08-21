package com.medisync.prescription.service;

import com.medisync.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionQrPayloadParserTest {

    private final PrescriptionQrPayloadParser parser = new PrescriptionQrPayloadParser();

    @Test
    void acceptsOnlyPhaseFourPayloadWithA256BitBase64UrlToken() {
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);

        assertThat(parser.parseToken("MEDISYNC:RX:" + token)).isEqualTo(token);
    }

    @Test
    void rejectsBlankWrongPrefixMalformedAndWrongLengthPayloads() {
        for (String payload : new String[]{null, "", "RX:abc", "MEDISYNC:RX:abc",
                "MEDISYNC:RX:!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"}) {
            assertThatThrownBy(() -> parser.parseToken(payload))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage("Prescription QR is invalid or no longer usable");
        }
    }
}
