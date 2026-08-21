package com.medisync.prescription.entity;

import com.medisync.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrescriptionTest {
    @Test
    void followsDraftIssuedCancelledLifecycleAndDerivesExpiry() {
        Prescription value = new Prescription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        value.updateDraft(14, "After food");
        OffsetDateTime issued = OffsetDateTime.of(2026, 8, 21, 8, 0, 0, 0, ZoneOffset.UTC);
        value.issue(issued);
        assertThat(value.getStatus()).isEqualTo(PrescriptionStatus.ISSUED);
        assertThat(value.getValidUntil()).isEqualTo(issued.plusDays(14));
        value.cancel("Treatment changed", issued.plusHours(1));
        assertThat(value.getStatus()).isEqualTo(PrescriptionStatus.CANCELLED);
        assertThat(value.getCancellationReason()).isEqualTo("Treatment changed");
    }

    @Test
    void issuedPrescriptionIsImmutableAndCannotBeIssuedTwice() {
        Prescription value = new Prescription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        value.issue(OffsetDateTime.now(ZoneOffset.UTC));
        assertThatThrownBy(() -> value.updateDraft(10, null)).isInstanceOf(ResourceConflictException.class);
        assertThatThrownBy(() -> value.issue(OffsetDateTime.now(ZoneOffset.UTC))).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void draftCannotBeCancelled() {
        Prescription value = new Prescription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        assertThatThrownBy(() -> value.cancel("Not required", OffsetDateTime.now(ZoneOffset.UTC)))
                .isInstanceOf(ResourceConflictException.class);
    }
}
