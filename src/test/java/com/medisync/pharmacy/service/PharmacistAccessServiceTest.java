package com.medisync.pharmacy.service;

import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.PharmacistProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.PharmacistProfileRepository;
import com.medisync.user.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PharmacistAccessServiceTest {

    @Test
    void onlyActiveVerifiedCompletePharmacistIsAccepted() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        PharmacistProfileRepository repository = mock(PharmacistProfileRepository.class);
        PharmacistAccessService service = new PharmacistAccessService(currentUserService, repository);
        AppUser user = new AppUser(UUID.randomUUID(), "pharmacist@example.com", "Saman", "Perera", null,
                UserRole.PHARMACIST, AccountStatus.ACTIVE);
        PharmacistProfile profile = new PharmacistProfile(user.getId());
        profile.updateProfessionalProfile("PH-1", "City Pharmacy", null, "1 Main Street", null);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "none").subject(user.getAuthUserId().toString()).build();
        when(currentUserService.requireRole(any(), any(), any(AccountStatus[].class))).thenReturn(user);
        when(repository.findByUserId(user.getId())).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> service.requireVerifiedPharmacist(jwt)).isInstanceOf(AccessDeniedException.class);

        profile.verify(UUID.randomUUID());
        assertThat(service.requireVerifiedPharmacist(jwt)).isSameAs(profile);
    }
}
