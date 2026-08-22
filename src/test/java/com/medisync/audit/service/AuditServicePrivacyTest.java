package com.medisync.audit.service;

import com.medisync.audit.entity.AuditEvent;
import com.medisync.audit.repository.AuditEventRepository;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditServicePrivacyTest {

    @Test
    void keepsOnlyAllowlistedNonSensitiveMetadata() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        AuditService service = new AuditService(repository);
        AppUser actor = new AppUser(UUID.randomUUID(), "admin@example.com", "Admin", "User", null,
                UserRole.ADMIN, AccountStatus.ACTIVE);

        service.record(actor, "TEST_ACTION", "APP_USER", UUID.randomUUID(), Map.of(
                "operation", "ban",
                "password", "must-never-be-recorded",
                "qrToken", "must-never-be-recorded"
        ));

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(event.capture());
        assertThat(event.getValue().getMetadata()).containsExactlyEntriesOf(Map.of("operation", "ban"));
    }
}
