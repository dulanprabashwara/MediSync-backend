package com.medisync.audit.service;

import com.medisync.audit.dto.AuditEventResponse;
import com.medisync.audit.entity.AuditEvent;
import com.medisync.audit.repository.AuditEventRepository;
import com.medisync.exception.InvalidRequestException;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AuditService {

    private static final Set<String> ALLOWED_METADATA_KEYS = Set.of(
            "previousStatus", "newStatus", "reasonProvided", "attachmentCount",
            "prescriptionStatus", "paymentStatus", "feeRequired", "verificationStatus",
            "appointmentStatus", "consultationStatus", "entityType", "operation"
    );

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(AppUser actor, String action, String targetType, UUID targetId) {
        record(actor, action, targetType, targetId, Map.of());
    }

    @Transactional
    public void record(AppUser actor, String action, String targetType, UUID targetId,
                       Map<String, ?> requestedMetadata) {
        Map<String, Object> safeMetadata = new LinkedHashMap<>();
        if (requestedMetadata != null) {
            requestedMetadata.forEach((key, value) -> {
                if (value != null && ALLOWED_METADATA_KEYS.contains(key) && isSafeScalar(value)) {
                    safeMetadata.put(key, value);
                }
            });
        }
        repository.save(new AuditEvent(
                OffsetDateTime.now(ZoneOffset.UTC),
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getRole(),
                action,
                targetType,
                targetId,
                Map.copyOf(safeMetadata)
        ));
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(int page, int size, String action, UUID actorUserId,
                                           UserRole actorRole, String targetType, UUID targetId,
                                           OffsetDateTime from, OffsetDateTime to) {
        if (size < 1 || size > 100 || page < 0) {
            throw new InvalidRequestException("Page size must be between 1 and 100");
        }
        Specification<AuditEvent> specification = (root, query, builder) -> {
            Predicate predicate = builder.conjunction();
            if (action != null && !action.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("action"), action.trim()));
            }
            if (actorUserId != null) predicate = builder.and(predicate, builder.equal(root.get("actorUserId"), actorUserId));
            if (actorRole != null) predicate = builder.and(predicate, builder.equal(root.get("actorRole"), actorRole));
            if (targetType != null && !targetType.isBlank()) {
                predicate = builder.and(predicate, builder.equal(root.get("targetType"), targetType.trim()));
            }
            if (targetId != null) predicate = builder.and(predicate, builder.equal(root.get("targetId"), targetId));
            if (from != null) predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("occurredAt"), to));
            return predicate;
        };
        return repository.findAll(specification, PageRequest.of(page, size,
                        Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> recentForUser(UUID userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 25));
        Specification<AuditEvent> specification = (root, query, builder) -> builder.or(
                builder.equal(root.get("actorUserId"), userId),
                builder.and(builder.equal(root.get("targetType"), "APP_USER"),
                        builder.equal(root.get("targetId"), userId))
        );
        return repository.findAll(specification, PageRequest.of(0, safeLimit,
                        Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))))
                .stream().map(this::toResponse).toList();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getOccurredAt(), event.getActorUserId(),
                event.getActorRole(), event.getAction(), event.getTargetType(), event.getTargetId(),
                event.getMetadata());
    }

    private boolean isSafeScalar(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Enum<?>;
    }

}
