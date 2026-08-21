package com.medisync.pharmacy.service;

import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.pharmacy.dto.DispensePrescriptionRequest;
import com.medisync.pharmacy.dto.PharmacyPrescriptionVerificationResponse;
import com.medisync.pharmacy.dto.PharmacyVerificationStatus;
import com.medisync.pharmacy.entity.PrescriptionDispensation;
import com.medisync.pharmacy.repository.PrescriptionDispensationRepository;
import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionQrToken;
import com.medisync.prescription.repository.PrescriptionQrTokenRepository;
import com.medisync.prescription.repository.PrescriptionRepository;
import com.medisync.prescription.service.PrescriptionQrPayloadParser;
import com.medisync.prescription.service.PrescriptionQrTokenHasher;
import com.medisync.user.entity.PharmacistProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionDispensingServiceTest {

    @Mock PharmacistAccessService accessService;
    @Mock PrescriptionQrTokenRepository tokenRepository;
    @Mock PrescriptionRepository prescriptionRepository;
    @Mock PrescriptionDispensationRepository dispensationRepository;
    @Mock PharmacyPrescriptionMapper mapper;

    private final PrescriptionQrTokenHasher hasher = new PrescriptionQrTokenHasher();
    private final PrescriptionQrPayloadParser parser = new PrescriptionQrPayloadParser();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-08-21T08:00:00Z");
    private PrescriptionDispensingService service;
    private PharmacistProfile pharmacist;
    private Prescription prescription;
    private PrescriptionQrToken token;
    private Jwt jwt;
    private String rawToken;
    private String payload;

    @BeforeEach
    void setUp() {
        service = new PrescriptionDispensingService(accessService, parser, hasher, tokenRepository,
                prescriptionRepository, dispensationRepository, mapper,
                Clock.fixed(now.toInstant(), ZoneId.of("UTC")));
        pharmacist = new PharmacistProfile(UUID.randomUUID());
        pharmacist.updateProfessionalProfile("PH-1", "City Pharmacy", null, "1 Main Street", null);
        pharmacist.verify(UUID.randomUUID());
        prescription = new Prescription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        prescription.issue(now.minusDays(1));
        rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        payload = "MEDISYNC:RX:" + rawToken;
        token = new PrescriptionQrToken(prescription.getId(), hasher.hash(rawToken), now.minusDays(1),
                prescription.getValidUntil());
        jwt = Jwt.withTokenValue("jwt").header("alg", "none").subject(UUID.randomUUID().toString()).build();
        when(accessService.requireVerifiedPharmacist(jwt)).thenReturn(pharmacist);
    }

    @Test
    void validQrVerificationIsReadOnlyAndReturnsOnlyMappedDispensingData() {
        PharmacyPrescriptionVerificationResponse expected = new PharmacyPrescriptionVerificationResponse(
                PharmacyVerificationStatus.VERIFIED, "Prescription verified", true, "Patient", "Dr. Doctor",
                "DOC-1", "Medicine", "Central", prescription.getIssuedAt(), prescription.getValidUntil(),
                List.of(), null, null, null);
        when(tokenRepository.findByTokenHash(hasher.hash(rawToken))).thenReturn(Optional.of(token));
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(mapper.verified(prescription)).thenReturn(expected);

        assertThat(service.verify(jwt, payload)).isSameAs(expected);
        verify(dispensationRepository, never()).saveAndFlush(any());
        verify(tokenRepository, never()).saveAndFlush(any());
    }

    @Test
    void unknownQrIsRejectedWithoutResolvingAProvidedPrescriptionId() {
        when(tokenRepository.findByTokenHash(hasher.hash(rawToken))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify(jwt, payload))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Prescription QR is invalid or no longer usable");
        verify(prescriptionRepository, never()).findById(any());
    }

    @Test
    void alreadyDispensedVerificationReturnsOnlyMinimalFulfilmentMetadata() {
        PrescriptionDispensation existing = new PrescriptionDispensation(prescription.getId(), pharmacist.getId(),
                now.minusMinutes(5), "City Pharmacy", "PH-1", null);
        when(tokenRepository.findByTokenHash(hasher.hash(rawToken))).thenReturn(Optional.of(token));
        when(prescriptionRepository.findById(prescription.getId())).thenReturn(Optional.of(prescription));
        when(dispensationRepository.findByPrescriptionId(prescription.getId())).thenReturn(Optional.of(existing));

        var result = service.verify(jwt, payload);

        assertThat(result.status()).isEqualTo(PharmacyVerificationStatus.ALREADY_DISPENSED);
        assertThat(result.dispensingEligible()).isFalse();
        assertThat(result.patientName()).isNull();
        assertThat(result.doctorName()).isNull();
        assertThat(result.items()).isEmpty();
        assertThat(result.dispensedAt()).isEqualTo(existing.getDispensedAt());
        assertThat(result.pharmacyName()).isEqualTo("City Pharmacy");
    }

    @Test
    void dispensingCreatesExactlyOneRecordAndRevokesToken() {
        when(tokenRepository.findByTokenHashForUpdate(hasher.hash(rawToken))).thenReturn(Optional.of(token));
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));

        var result = service.dispense(jwt, new DispensePrescriptionRequest(payload, "Dispensed as prescribed"));

        assertThat(result.status()).isEqualTo("DISPENSED");
        assertThat(result.pharmacyName()).isEqualTo("City Pharmacy");
        assertThat(token.getRevokedAt()).isEqualTo(now);
        verify(dispensationRepository).saveAndFlush(any(PrescriptionDispensation.class));
        verify(tokenRepository).saveAndFlush(token);
    }

    @Test
    void alreadyDispensedPrescriptionCannotBeReused() {
        PrescriptionDispensation existing = new PrescriptionDispensation(prescription.getId(), pharmacist.getId(),
                now.minusMinutes(5), "City Pharmacy", "PH-1", null);
        when(tokenRepository.findByTokenHashForUpdate(hasher.hash(rawToken))).thenReturn(Optional.of(token));
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));
        when(dispensationRepository.findByPrescriptionId(prescription.getId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.dispense(jwt, new DispensePrescriptionRequest(payload, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already been dispensed");
        verify(dispensationRepository, never()).saveAndFlush(any());
    }

    @Test
    void rotationCancelledAndExpiredStateAreRecheckedAtDispenseTime() {
        when(tokenRepository.findByTokenHashForUpdate(hasher.hash(rawToken))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.dispense(jwt, new DispensePrescriptionRequest(payload, null)))
                .isInstanceOf(InvalidRequestException.class);

        when(tokenRepository.findByTokenHashForUpdate(hasher.hash(rawToken))).thenReturn(Optional.of(token));
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));
        prescription.cancel("Treatment changed", now.minusMinutes(1));
        assertThatThrownBy(() -> service.dispense(jwt, new DispensePrescriptionRequest(payload, null)))
                .isInstanceOf(ResourceConflictException.class).hasMessageContaining("cancelled");
    }

    @Test
    void revokedAndExpiredTokensAreRejectedAtFinalDispense() {
        token.revoke(now.minusMinutes(1));
        when(tokenRepository.findByTokenHashForUpdate(hasher.hash(rawToken))).thenReturn(Optional.of(token));
        when(prescriptionRepository.findByIdForUpdate(prescription.getId())).thenReturn(Optional.of(prescription));

        assertThatThrownBy(() -> service.dispense(jwt, new DispensePrescriptionRequest(payload, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Prescription QR is invalid or no longer usable");

        PrescriptionQrToken expiredToken = new PrescriptionQrToken(prescription.getId(), hasher.hash(rawToken),
                now.minusDays(2), now.minusSeconds(1));
        when(tokenRepository.findByTokenHashForUpdate(hasher.hash(rawToken))).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> service.dispense(jwt, new DispensePrescriptionRequest(payload, null)))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("expired");
        verify(dispensationRepository, never()).saveAndFlush(any());
    }

    @Test
    void dispensingHistoryDetailIsRestrictedToTheOwningPharmacist() {
        PrescriptionDispensation otherPharmacistsRecord = new PrescriptionDispensation(prescription.getId(),
                UUID.randomUUID(), now.minusMinutes(5), "Other Pharmacy", "PH-2", null);
        when(dispensationRepository.findById(otherPharmacistsRecord.getId()))
                .thenReturn(Optional.of(otherPharmacistsRecord));

        assertThatThrownBy(() -> service.historyDetail(jwt, otherPharmacistsRecord.getId()))
                .isInstanceOf(AccessDeniedException.class);
        verify(prescriptionRepository, never()).findById(any());
    }
}
