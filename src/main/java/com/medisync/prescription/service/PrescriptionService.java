package com.medisync.prescription.service;

import com.medisync.common.dto.PageResponse;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.consultation.service.ConsultationAccessService;
import com.medisync.exception.InvalidRequestException;
import com.medisync.exception.ResourceConflictException;
import com.medisync.pharmacy.repository.PrescriptionDispensationRepository;
import com.medisync.prescription.dto.DoctorPrescriptionResponse;
import com.medisync.prescription.dto.PatientPrescriptionDetail;
import com.medisync.prescription.dto.PatientPrescriptionSummary;
import com.medisync.prescription.dto.PrescriptionDraftRequest;
import com.medisync.prescription.dto.PrescriptionItemRequest;
import com.medisync.prescription.dto.PrescriptionQrResponse;
import com.medisync.prescription.entity.Prescription;
import com.medisync.prescription.entity.PrescriptionItem;
import com.medisync.prescription.entity.PrescriptionQrToken;
import com.medisync.prescription.entity.PrescriptionStatus;
import com.medisync.prescription.repository.PrescriptionItemRepository;
import com.medisync.prescription.repository.PrescriptionQrTokenRepository;
import com.medisync.prescription.repository.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Map;
import com.medisync.audit.AuditActions;
import com.medisync.audit.service.AuditService;
import com.medisync.config.PrescriptionPaymentProperties;
import com.medisync.user.service.CurrentUserService;
import com.medisync.user.entity.AppUser;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);
    private static final int MAX_PAGE_SIZE = 50;

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository itemRepository;
    private final PrescriptionQrTokenRepository tokenRepository;
    private final PrescriptionDispensationRepository dispensationRepository;
    private final PrescriptionAccessService accessService;
    private final ConsultationAccessService consultationAccessService;
    private final PrescriptionResponseMapper mapper;
    private final PrescriptionQrTokenGenerator tokenGenerator;
    private final PrescriptionQrTokenHasher tokenHasher;
    private final Clock clock;
    private final PrescriptionPaymentProperties paymentProperties;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    @Autowired
    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PrescriptionItemRepository itemRepository,
                               PrescriptionQrTokenRepository tokenRepository,
                               PrescriptionDispensationRepository dispensationRepository,
                               PrescriptionAccessService accessService,
                               ConsultationAccessService consultationAccessService,
                               PrescriptionResponseMapper mapper,
                               PrescriptionQrTokenGenerator tokenGenerator,
                               PrescriptionQrTokenHasher tokenHasher,
                               Clock clock,
                               PrescriptionPaymentProperties paymentProperties,
                               CurrentUserService currentUserService,
                               AuditService auditService) {
        this.prescriptionRepository = prescriptionRepository;
        this.itemRepository = itemRepository;
        this.tokenRepository = tokenRepository;
        this.dispensationRepository = dispensationRepository;
        this.accessService = accessService;
        this.consultationAccessService = consultationAccessService;
        this.mapper = mapper;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
        this.paymentProperties = paymentProperties;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    PrescriptionService(PrescriptionRepository prescriptionRepository,
                        PrescriptionItemRepository itemRepository,
                        PrescriptionQrTokenRepository tokenRepository,
                        PrescriptionDispensationRepository dispensationRepository,
                        PrescriptionAccessService accessService,
                        ConsultationAccessService consultationAccessService,
                        PrescriptionResponseMapper mapper,
                        PrescriptionQrTokenGenerator tokenGenerator,
                        PrescriptionQrTokenHasher tokenHasher,
                        Clock clock) {
        this(prescriptionRepository, itemRepository, tokenRepository, dispensationRepository, accessService,
                consultationAccessService, mapper, tokenGenerator, tokenHasher, clock,
                new PrescriptionPaymentProperties("LKR"), null, null);
    }

    @Transactional(readOnly = true)
    public PageResponse<DoctorPrescriptionResponse> doctorList(Jwt jwt, int page, int size) {
        var doctor = accessService.requireDoctor(jwt);
        return PageResponse.from(prescriptionRepository.findByDoctorId(doctor.getId(), page(page, size)),
                mapper::toDoctorResponse);
    }

    @Transactional(readOnly = true)
    public DoctorPrescriptionResponse doctorDetails(Jwt jwt, UUID prescriptionId) {
        return mapper.toDoctorResponse(accessService.requireDoctorPrescription(jwt, prescriptionId, false)
                .prescription());
    }

    @Transactional(readOnly = true)
    public List<DoctorPrescriptionResponse> consultationList(Jwt jwt, UUID consultationId) {
        var context = consultationAccessService.requireDoctor(jwt, consultationId, false);
        if (context.consultation().getStatus() == ConsultationStatus.CANCELLED) {
            return List.of();
        }
        return prescriptionRepository.findByConsultationIdOrderByCreatedAtDesc(consultationId).stream()
                .map(mapper::toDoctorResponse).toList();
    }

    @Transactional
    public DoctorPrescriptionResponse createDraft(Jwt jwt, UUID consultationId) {
        var context = consultationAccessService.requireDoctor(jwt, consultationId, true);
        requireDraftWritable(context.consultation().getStatus());
        var existing = prescriptionRepository.findByConsultationIdAndStatus(consultationId, PrescriptionStatus.DRAFT);
        Prescription prescription = existing.orElseGet(() -> prescriptionRepository.saveAndFlush(
                new Prescription(consultationId, context.doctor().getId(), context.patient().getId())));
        verifyOwnership(prescription, context.doctor().getId(), context.patient().getId());
        if (existing.isEmpty()) {
            audit(jwt, AuditActions.PRESCRIPTION_CREATED, prescription.getId(),
                    Map.of("prescriptionStatus", "DRAFT"));
        }
        return mapper.toDoctorResponse(prescription);
    }

    @Transactional
    public DoctorPrescriptionResponse updateDraft(Jwt jwt, UUID prescriptionId, PrescriptionDraftRequest request) {
        var access = accessService.requireDoctorPrescription(jwt, prescriptionId, true);
        var context = consultationAccessService.requireDoctor(jwt, access.prescription().getConsultationId(), true);
        requireDraftWritable(context.consultation().getStatus());
        verifyOwnership(access.prescription(), context.doctor().getId(), context.patient().getId());
        BigDecimal fee = request.doctorFeeAmount() == null ? BigDecimal.ZERO : request.doctorFeeAmount();
        access.prescription().updateDraft(request.validityDays(), optional(request.generalInstructions()), fee,
                paymentProperties.normalizedCurrency());
        prescriptionRepository.saveAndFlush(access.prescription());
        itemRepository.deleteByPrescriptionId(prescriptionId);
        itemRepository.flush();
        List<PrescriptionItem> items = java.util.stream.IntStream.range(0, request.items().size())
                .mapToObj(index -> item(prescriptionId, index + 1, request.items().get(index)))
                .toList();
        itemRepository.saveAllAndFlush(items);
        audit(jwt, AuditActions.PRESCRIPTION_DRAFT_SAVED, prescriptionId,
                Map.of("feeRequired", fee.signum() > 0, "prescriptionStatus", "DRAFT"));
        return mapper.toDoctorResponse(access.prescription());
    }

    @Transactional
    public DoctorPrescriptionResponse issue(Jwt jwt, UUID prescriptionId) {
        var access = accessService.requireDoctorPrescription(jwt, prescriptionId, true);
        var context = consultationAccessService.requireDoctor(jwt, access.prescription().getConsultationId(), true);
        verifyOwnership(access.prescription(), context.doctor().getId(), context.patient().getId());
        ConsultationStatus status = context.consultation().getStatus();
        if (status != ConsultationStatus.IN_PROGRESS) {
            throw new ResourceConflictException("A prescription can only be issued while the consultation is in progress");
        }
        long count = itemRepository.countByPrescriptionId(prescriptionId);
        if (count < 1 || count > 20) {
            throw new InvalidRequestException("An issued prescription must contain between 1 and 20 medicines");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        access.prescription().issue(now);
        prescriptionRepository.saveAndFlush(access.prescription());
        audit(jwt, AuditActions.PRESCRIPTION_ISSUED, prescriptionId,
                Map.of("paymentStatus", access.prescription().getDoctorFeeStatus().name(),
                        "feeRequired", access.prescription().getDoctorFeeAmount().signum() > 0));
        log.info("Prescription {} issued", prescriptionId);
        return mapper.toDoctorResponse(access.prescription());
    }

    @Transactional
    public DoctorPrescriptionResponse cancel(Jwt jwt, UUID prescriptionId, String reason) {
        var doctor = accessService.requireDoctor(jwt);
        var lockedToken = tokenRepository.findByPrescriptionIdForUpdate(prescriptionId);
        var access = accessService.requireDoctorPrescription(doctor, prescriptionId, true);
        var context = consultationAccessService.requireDoctor(jwt, access.prescription().getConsultationId(), true);
        verifyOwnership(access.prescription(), context.doctor().getId(), context.patient().getId());
        if (dispensationRepository.existsByPrescriptionId(prescriptionId)) {
            throw new ResourceConflictException("A dispensed prescription cannot be cancelled");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        String normalizedReason = required(reason);
        if (normalizedReason.length() < 3 || normalizedReason.length() > 1000) {
            throw new InvalidRequestException("Cancellation reason must be between 3 and 1000 characters");
        }
        access.prescription().cancel(normalizedReason, now);
        prescriptionRepository.saveAndFlush(access.prescription());
        audit(jwt, AuditActions.PRESCRIPTION_CANCELLED, prescriptionId,
                Map.of("reasonProvided", true, "prescriptionStatus", "CANCELLED"));
        lockedToken.or(() -> tokenRepository.findByPrescriptionId(prescriptionId)).ifPresent(token -> {
            token.revoke(now);
            tokenRepository.saveAndFlush(token);
        });
        log.info("Prescription {} cancelled", prescriptionId);
        return mapper.toDoctorResponse(access.prescription());
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientPrescriptionSummary> patientList(Jwt jwt, int page, int size) {
        var patient = accessService.requirePatient(jwt);
        return PageResponse.from(prescriptionRepository.findByPatientIdAndStatusIn(patient.getId(),
                        EnumSet.of(PrescriptionStatus.ISSUED, PrescriptionStatus.CANCELLED), page(page, size)),
                mapper::toPatientSummary);
    }

    @Transactional(readOnly = true)
    public PatientPrescriptionDetail patientDetails(Jwt jwt, UUID prescriptionId) {
        return mapper.toPatientDetail(accessService.requirePatientPrescription(jwt, prescriptionId).prescription());
    }

    @Transactional
    public PrescriptionQrResponse generatePatientQr(Jwt jwt, UUID prescriptionId) {
        var patient = accessService.requirePatient(jwt);
        var lockedToken = tokenRepository.findByPrescriptionIdForUpdate(prescriptionId);
        Prescription prescription = accessService.requirePatientPrescription(patient, prescriptionId, true).prescription();
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (prescription.getStatus() != PrescriptionStatus.ISSUED
                || prescription.getValidUntil() == null
                || !now.isBefore(prescription.getValidUntil())) {
            throw new ResourceConflictException("A QR can only be generated for an active issued prescription");
        }
        if (dispensationRepository.existsByPrescriptionId(prescriptionId)) {
            throw new ResourceConflictException("This prescription has already been dispensed");
        }
        if (!prescription.isQrPaymentEligible()) {
            throw new ResourceConflictException(
                    "The doctor must confirm the consultation fee before the prescription QR can be generated");
        }

        String rawToken = uniqueTokenHash();
        String tokenHash = tokenHasher.hash(rawToken);
        PrescriptionQrToken token = lockedToken.or(() -> tokenRepository.findByPrescriptionId(prescriptionId))
                .orElseGet(() -> new PrescriptionQrToken(prescriptionId, tokenHash, now,
                        prescription.getValidUntil()));
        token.rotate(tokenHash, now, prescription.getValidUntil());
        tokenRepository.saveAndFlush(token);
        audit(jwt, AuditActions.QR_TOKEN_CREATED, prescriptionId,
                Map.of("paymentStatus", prescription.getDoctorFeeStatus().name()));
        return new PrescriptionQrResponse(tokenGenerator.payload(rawToken), prescription.getValidUntil());
    }

    @Transactional
    public DoctorPrescriptionResponse confirmDoctorFee(Jwt jwt, UUID prescriptionId) {
        AppUser doctorUser = currentUserService.requireCurrentUser(jwt);
        var access = accessService.requireDoctorPrescription(jwt, prescriptionId, true);
        var context = consultationAccessService.requireDoctor(jwt, access.prescription().getConsultationId(), true);
        verifyOwnership(access.prescription(), context.doctor().getId(), context.patient().getId());
        if (context.consultation().getStatus() != ConsultationStatus.IN_PROGRESS
                && context.consultation().getStatus() != ConsultationStatus.COMPLETED) {
            throw new ResourceConflictException(
                    "Payment can only be confirmed for an in-progress or completed consultation");
        }
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (access.prescription().getValidUntil() == null
                || !now.isBefore(access.prescription().getValidUntil())) {
            throw new ResourceConflictException("Payment cannot be confirmed for an expired prescription");
        }
        if (dispensationRepository.existsByPrescriptionId(prescriptionId)) {
            throw new ResourceConflictException("Payment cannot be confirmed for a dispensed prescription");
        }
        access.prescription().confirmDoctorFee(doctorUser.getId(), now);
        prescriptionRepository.saveAndFlush(access.prescription());
        auditService.record(doctorUser, AuditActions.DOCTOR_FEE_CONFIRMED, "PRESCRIPTION", prescriptionId,
                Map.of("paymentStatus", access.prescription().getDoctorFeeStatus().name(), "feeRequired", true));
        return mapper.toDoctorResponse(access.prescription());
    }

    @Transactional
    public void discardDraft(Jwt jwt, UUID prescriptionId) {
        var access = accessService.requireDoctorPrescription(jwt, prescriptionId, true);
        var context = consultationAccessService.requireDoctor(jwt, access.prescription().getConsultationId(), true);
        verifyOwnership(access.prescription(), context.doctor().getId(), context.patient().getId());
        if (access.prescription().getStatus() != PrescriptionStatus.DRAFT) {
            throw new ResourceConflictException("Only a draft prescription can be discarded");
        }
        itemRepository.deleteByPrescriptionId(prescriptionId);
        itemRepository.flush();
        prescriptionRepository.delete(access.prescription());
        prescriptionRepository.flush();
        log.info("Draft prescription {} discarded", prescriptionId);
    }

    private PageRequest page(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidRequestException("Page must be non-negative and size must be between 1 and 50");
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PrescriptionItem item(UUID prescriptionId, int position, PrescriptionItemRequest request) {
        return new PrescriptionItem(prescriptionId, position, required(request.medicineName()),
                optional(request.strength()), required(request.dosage()), required(request.frequency()),
                required(request.duration()), optional(request.quantity()), optional(request.medicineForm()),
                optional(request.route()), optional(request.instructions()));
    }

    private String uniqueTokenHash() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String rawToken = tokenGenerator.generateToken();
            if (!tokenRepository.existsByTokenHash(tokenHasher.hash(rawToken))) {
                return rawToken;
            }
        }
        throw new ResourceConflictException("A secure prescription QR token could not be generated; please retry");
    }

    private void requireDraftWritable(ConsultationStatus status) {
        if (status != ConsultationStatus.SCHEDULED && status != ConsultationStatus.IN_PROGRESS) {
            throw new ResourceConflictException(
                    "Prescriptions can only be created or edited for a scheduled or in-progress consultation");
        }
    }

    private void verifyOwnership(Prescription prescription, UUID doctorId, UUID patientId) {
        if (!doctorId.equals(prescription.getDoctorId()) || !patientId.equals(prescription.getPatientId())) {
            throw new ResourceConflictException("Prescription and consultation ownership are inconsistent");
        }
    }

    private String required(String value) {
        String normalized = optional(value);
        if (normalized == null) {
            throw new InvalidRequestException("Required prescription text cannot be blank");
        }
        return normalized;
    }

    private String optional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void audit(Jwt jwt, String action, UUID prescriptionId, Map<String, ?> metadata) {
        if (auditService != null && currentUserService != null) {
            auditService.record(currentUserService.requireCurrentUser(jwt), action,
                    "PRESCRIPTION", prescriptionId, metadata);
        }
    }
}
