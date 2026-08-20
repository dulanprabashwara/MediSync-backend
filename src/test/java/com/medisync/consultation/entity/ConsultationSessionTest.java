package com.medisync.consultation.entity;

import com.medisync.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultationSessionTest {

    @Test
    void followsScheduledInProgressCompletedLifecycle() {
        ConsultationSession consultation = new ConsultationSession(UUID.randomUUID());

        consultation.start();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
        assertThat(consultation.getStartedAt()).isNotNull();

        consultation.complete();
        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.COMPLETED);
        assertThat(consultation.getCompletedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidAndDuplicateTransitions() {
        ConsultationSession scheduled = new ConsultationSession(UUID.randomUUID());
        assertThatThrownBy(scheduled::complete).isInstanceOf(ResourceConflictException.class);

        scheduled.start();
        assertThatThrownBy(scheduled::start).isInstanceOf(ResourceConflictException.class);
        scheduled.complete();
        assertThatThrownBy(scheduled::complete).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(scheduled::start).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(scheduled::cancel).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void scheduledConsultationCanBeCancelledAndCannotRestart() {
        ConsultationSession consultation = new ConsultationSession(UUID.randomUUID());
        consultation.cancel();

        assertThat(consultation.getStatus()).isEqualTo(ConsultationStatus.CANCELLED);
        assertThat(consultation.getCancelledAt()).isNotNull();
        assertThatThrownBy(consultation::start).isInstanceOf(ResourceConflictException.class);
    }
}
