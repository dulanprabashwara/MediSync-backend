package com.medisync.user.service;

import com.medisync.audit.service.AuditService;
import com.medisync.media.ImageUploadValidator;
import com.medisync.media.MediaStorageService;
import com.medisync.media.MediaUrlService;
import com.medisync.media.ValidatedImage;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceTest {

    @Mock CurrentUserService currentUserService;
    @Mock AppUserRepository userRepository;
    @Mock ImageUploadValidator validator;
    @Mock MediaStorageService storageService;
    @Mock MediaUrlService mediaUrlService;
    @Mock AuditService auditService;

    private ProfileImageService service;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        service = new ProfileImageService(currentUserService, userRepository, validator, storageService,
                mediaUrlService, auditService);
        jwt = Jwt.withTokenValue("token").header("alg", "none").subject(UUID.randomUUID().toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(300)).build();
    }

    @Test
    void activeOwnerUploadUsesRandomPrivateKeyInsteadOfFilename() {
        AppUser user = user(AccountStatus.ACTIVE);
        byte[] bytes = new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff};
        MockMultipartFile file = new MockMultipartFile("file", "patient-name.jpg", "image/jpeg", bytes);
        when(currentUserService.requireCurrentUser(jwt)).thenReturn(user);
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(validator.validate(file)).thenReturn(new ValidatedImage(bytes, "image/jpeg", "jpg",
                "patient-name.jpg"));
        when(mediaUrlService.signedUrlOrNull(any())).thenReturn("https://signed.example/profile");

        var response = service.replace(jwt, file);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(storageService).upload(key.capture(), any(ValidatedImage.class));
        assertThat(key.getValue()).startsWith("profiles/" + user.getId() + "/")
                .doesNotContain("patient-name");
        assertThat(response.profileImageUrl()).isEqualTo("https://signed.example/profile");
    }

    @Test
    void bannedAccountCannotUploadProfileImage() {
        AppUser user = user(AccountStatus.BANNED);
        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", "image/jpeg",
                new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff});
        when(currentUserService.requireCurrentUser(jwt)).thenReturn(user);

        assertThatThrownBy(() -> service.replace(jwt, file)).isInstanceOf(AccessDeniedException.class);
        verify(validator, never()).validate(any());
        verify(storageService, never()).upload(any(), any());
    }

    private AppUser user(AccountStatus status) {
        return new AppUser(UUID.randomUUID(), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, status);
    }
}
