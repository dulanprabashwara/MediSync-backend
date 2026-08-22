package com.medisync.user.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppUserRestrictionTest {

    @Test
    void banAndRestorePreserveThePreBanStatus() {
        AppUser user = new AppUser(UUID.randomUUID(), "doctor@example.com", "A", "Doctor", null,
                UserRole.DOCTOR, AccountStatus.PENDING_VERIFICATION);
        user.ban();
        assertThat(user.getStatus()).isEqualTo(AccountStatus.BANNED);

        user.restoreStatus(AccountStatus.PENDING_VERIFICATION);
        assertThat(user.getStatus()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    }

    @Test
    void bannedCannotBeUsedAsRestoreStatus() {
        AppUser user = new AppUser(UUID.randomUUID(), "patient@example.com", "A", "Patient", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        assertThatThrownBy(() -> user.restoreStatus(AccountStatus.BANNED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
