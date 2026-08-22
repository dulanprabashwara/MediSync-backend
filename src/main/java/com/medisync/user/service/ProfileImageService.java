package com.medisync.user.service;

import com.medisync.audit.AuditActions;
import com.medisync.audit.service.AuditService;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.media.ImageUploadValidator;
import com.medisync.media.MediaStorageService;
import com.medisync.media.MediaUrlService;
import com.medisync.media.ValidatedImage;
import com.medisync.user.dto.UserResponse;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ProfileImageService {

    private static final Logger log = LoggerFactory.getLogger(ProfileImageService.class);

    private final CurrentUserService currentUserService;
    private final AppUserRepository userRepository;
    private final ImageUploadValidator validator;
    private final MediaStorageService storageService;
    private final MediaUrlService mediaUrlService;
    private final AuditService auditService;

    public ProfileImageService(CurrentUserService currentUserService, AppUserRepository userRepository,
                               ImageUploadValidator validator, MediaStorageService storageService,
                               MediaUrlService mediaUrlService, AuditService auditService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.validator = validator;
        this.storageService = storageService;
        this.mediaUrlService = mediaUrlService;
        this.auditService = auditService;
    }

    @Transactional
    public UserResponse replace(Jwt jwt, MultipartFile file) {
        AppUser actor = lockAllowedUser(jwt);
        ValidatedImage image = validator.validate(file);
        String newKey = "profiles/" + actor.getId() + "/" + UUID.randomUUID() + "." + image.extension();
        String oldKey = actor.getProfileImageKey();
        storageService.upload(newKey, image);
        try {
            actor.replaceProfileImage(newKey, OffsetDateTime.now(ZoneOffset.UTC));
            userRepository.saveAndFlush(actor);
            auditService.record(actor, AuditActions.PROFILE_IMAGE_UPDATED, "APP_USER", actor.getId());
        } catch (RuntimeException exception) {
            bestEffortDelete(newKey);
            throw exception;
        }
        bestEffortDelete(oldKey);
        return UserResponse.from(actor, mediaUrlService.signedUrlOrNull(newKey));
    }

    @Transactional
    public UserResponse remove(Jwt jwt) {
        AppUser actor = lockAllowedUser(jwt);
        String oldKey = actor.getProfileImageKey();
        actor.removeProfileImage();
        userRepository.saveAndFlush(actor);
        auditService.record(actor, AuditActions.PROFILE_IMAGE_REMOVED, "APP_USER", actor.getId());
        bestEffortDelete(oldKey);
        return UserResponse.from(actor, null);
    }

    private AppUser lockAllowedUser(Jwt jwt) {
        AppUser current = currentUserService.requireCurrentUser(jwt);
        if (current.getStatus() != AccountStatus.ACTIVE
                && current.getStatus() != AccountStatus.PENDING_VERIFICATION) {
            throw new AccessDeniedException("This account cannot update a profile image");
        }
        return userRepository.findByIdForUpdate(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User account was not found"));
    }

    private void bestEffortDelete(String key) {
        if (key == null || key.isBlank()) return;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                storageService.delete(key);
            } catch (Exception exception) {
                log.warn("A superseded private media object could not be removed: {}", exception.getMessage());
            }
        });
    }

    public void deleteProfileImage(String key) {
        bestEffortDelete(key);
    }
}
