package com.medisync.pharmacy.service;

import com.medisync.common.dto.PageResponse;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.pharmacy.dto.DispensationHistoryDetail;
import com.medisync.pharmacy.dto.DispensationHistorySummary;
import com.medisync.pharmacy.dto.DispensePrescriptionRequest;
import com.medisync.pharmacy.dto.DispensePrescriptionResponse;
import com.medisync.pharmacy.dto.PharmacyPrescriptionVerificationResponse;
import com.medisync.pharmacy.dto.PharmacyVerificationStatus;
import com.medisync.pharmacy.entity.PrescriptionDispensation;
import com.medisync.pharmacy.repository.PrescriptionDispensationRepository;
import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionQrToken;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.prescription.repository.PrescriptionQrTokenRepository;
import com.medisync.prescription.repository.PrescriptionRepository;
import com.medisync.prescription.service.PrescriptionQrPayloadParser;
import com.medisync.prescription.service.PrescriptionQrTokenHasher;
import com.medisync.user.entity.PharmacistProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PrescriptionDispensingService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionDispensingService.class);
    private static final int MAX_PAGE_SIZE = 50;
    private static final String INVALID_QR = "Prescription QR is invalid or no longer usable";

    private final PharmacistAccessService pharmacistAccessService;
    private final PrescriptionQrPayloadParser payloadParser;
    private final PrescriptionQrTokenHasher tokenHasher;
    private final PrescriptionQrTokenRepository tokenRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDispensationRepository dispensationRepository;
    private final PharmacyPrescriptionMapper mapper;
    private final Clock clock;

    public PrescriptionDispensingService(PharmacistAccessService pharmacistAccessService,
                                         PrescriptionQrPayloadParser payloadParser,
                                         PrescriptionQrTokenHasher tokenHasher,
                                         PrescriptionQrTokenRepository tokenRepository,
                                         PrescriptionRepository prescriptionRepository,
                                         PrescriptionDispensationRepository dispensationRepository,
                                         PharmacyPrescriptionMapper mapper,
                                         Clock clock) {
        this.pharmacistAccessService = pharmacistAccessService;
        this.payloadParser = payloadParser;
        this.tokenHasher = tokenHasher;
        this.tokenRepository = tokenRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.dispensationRepository = dispensationRepository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PharmacyPrescriptionVerificationResponse verify(Jwt jwt, String qrPayload) {
        pharmacistAccessService.requireVerifiedPharmacist(jwt);
        String tokenHash = tokenHasher.hash(payloadParser.parseToken(qrPayload));
        PrescriptionQrToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidQr);
        Prescription prescription = prescriptionRepository.findById(token.getPrescriptionId())
                .orElseThrow(this::invalidQr);
        PrescriptionDispensation dispensation = dispensationRepository.findByPrescriptionId(prescription.getId())
                .orElse(null);
        OffsetDateTime now = OffsetDateTime.now(clock);

        if (dispensation != null) {
            return status(PharmacyVerificationStatus.ALREADY_DISPENSED,
                    "Prescription has already been dispensed", dispensation.getDispensedAt(),
                    dispensation.getPharmacyNameSnapshot());
        }
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            return status(PharmacyVerificationStatus.CANCELLED, "Prescription has been cancelled", null, null);
        }
        if (expired(prescription, token, now)) {
            return status(PharmacyVerificationStatus.EXPIRED, "Prescription has expired", null, null);
        }
        if (prescription.getStatus() != PrescriptionStatus.ISSUED || token.getRevokedAt() != null) {
            return status(PharmacyVerificationStatus.QR_NO_LONGER_VALID, INVALID_QR, null, null);
        }
        return mapper.verified(prescription);
    }

    @Transactional
    public DispensePrescriptionResponse dispense(Jwt jwt, DispensePrescriptionRequest request) {
        PharmacistProfile pharmacist = pharmacistAccessService.requireVerifiedPharmacist(jwt);
        String tokenHash = tokenHasher.hash(payloadParser.parseToken(request.qrPayload()));
        PrescriptionQrToken token = tokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::invalidQr);
        Prescription prescription = prescriptionRepository.findByIdForUpdate(token.getPrescriptionId())
                .orElseThrow(this::invalidQr);
        PrescriptionDispensation existing = dispensationRepository.findByPrescriptionId(prescription.getId())
                .orElse(null);
        if (existing != null) {
            throw new ResourceConflictException("This prescription has already been dispensed");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (prescription.getStatus() == PrescriptionStatus.CANCELLED) {
            throw new ResourceConflictException("A cancelled prescription cannot be dispensed");
        }
        if (expired(prescription, token, now)) {
            throw new ResourceConflictException("An expired prescription cannot be dispensed");
        }
        if (prescription.getStatus() != PrescriptionStatus.ISSUED || token.getRevokedAt() != null) {
            throw new ResourceConflictException(INVALID_QR);
        }

        PrescriptionDispensation dispensation = new PrescriptionDispensation(prescription.getId(),
                pharmacist.getId(), now, pharmacist.getPharmacyName(),
                pharmacist.getProfessionalRegistrationNumber(), optional(request.note()));
        dispensationRepository.saveAndFlush(dispensation);
        token.revoke(now);
        tokenRepository.saveAndFlush(token);
        log.info("Prescription {} dispensed", prescription.getId());
        return new DispensePrescriptionResponse("DISPENSED", now, pharmacist.getPharmacyName());
    }

    @Transactional(readOnly = true)
    public PageResponse<DispensationHistorySummary> history(Jwt jwt, int page, int size) {
        PharmacistProfile pharmacist = pharmacistAccessService.requireVerifiedPharmacist(jwt);
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 50");
        }
        return PageResponse.from(dispensationRepository.findByPharmacistIdOrderByDispensedAtDesc(
                pharmacist.getId(), PageRequest.of(page, size)), dispensation -> mapper.historySummary(dispensation,
                prescription(dispensation.getPrescriptionId())));
    }

    @Transactional(readOnly = true)
    public DispensationHistoryDetail historyDetail(Jwt jwt, UUID dispensationId) {
        PharmacistProfile pharmacist = pharmacistAccessService.requireVerifiedPharmacist(jwt);
        PrescriptionDispensation dispensation = dispensationRepository.findById(dispensationId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispensing record not found"));
        if (!pharmacist.getId().equals(dispensation.getPharmacistId())) {
            throw new AccessDeniedException("This dispensing record belongs to another pharmacist");
        }
        return mapper.historyDetail(dispensation, prescription(dispensation.getPrescriptionId()));
    }

    private PharmacyPrescriptionVerificationResponse status(PharmacyVerificationStatus status, String message,
                                                             OffsetDateTime dispensedAt, String pharmacyName) {
        return new PharmacyPrescriptionVerificationResponse(status, message, false, null, null, null, null, null,
                null, null, List.of(), null, dispensedAt, pharmacyName);
    }

    private boolean expired(Prescription prescription, PrescriptionQrToken token, OffsetDateTime now) {
        return prescription.getValidUntil() == null || !now.isBefore(prescription.getValidUntil())
                || !now.isBefore(token.getExpiresAt());
    }

    private Prescription prescription(UUID prescriptionId) {
        return prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
    }

    private InvalidRequestException invalidQr() {
        return new InvalidRequestException(INVALID_QR);
    }

    private String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
