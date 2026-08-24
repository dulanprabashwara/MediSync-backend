package com.medisync.consultation.dto;

import com.medisync.consultation.entity.ConsultationStatus;

import java.util.UUID;

public record ConsultationEvent(
        ConsultationEventType eventType,
        UUID consultationId,
        ConsultationMessageResponse message,
        ConsultationStatus status
) {
    public static ConsultationEvent newMessage(ConsultationMessageResponse message) {
        return new ConsultationEvent(ConsultationEventType.NEW_MESSAGE, message.consultationId(), message, null);
    }

    public static ConsultationEvent messageDeleted(ConsultationMessageResponse message) {
        return new ConsultationEvent(ConsultationEventType.MESSAGE_DELETED, message.consultationId(), message, null);
    }

    public static ConsultationEvent statusChanged(UUID consultationId, ConsultationStatus status) {
        return new ConsultationEvent(ConsultationEventType.CONSULTATION_STATUS_CHANGED,
                consultationId, null, status);
    }

    public static ConsultationEvent paymentStatusChanged(UUID consultationId) {
        return new ConsultationEvent(ConsultationEventType.PAYMENT_STATUS_CHANGED,
                consultationId, null, null);
    }
}
