package com.medisync.user.controller;

import com.medisync.user.dto.OnboardingRequest;
import com.medisync.user.dto.UserResponse;
import com.medisync.user.dto.AccountStatusResponse;
import com.medisync.user.service.ProfileImageService;
import com.medisync.user.service.UserDeletionService;
import com.medisync.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserDeletionService userDeletionService;
    private ProfileImageService profileImageService;

    public UserController(UserService userService, UserDeletionService userDeletionService) {
        this.userService = userService;
        this.userDeletionService = userDeletionService;
    }

    @Autowired(required = false)
    void setProfileImageService(ProfileImageService profileImageService) {
        this.profileImageService = profileImageService;
    }

    @GetMapping("/me/account-status")
    public AccountStatusResponse accountStatus(@AuthenticationPrincipal Jwt jwt) {
        return userService.getAccountStatus(jwt);
    }

    @PostMapping(value = "/me/profile-image", consumes = "multipart/form-data")
    public UserResponse replaceProfileImage(@AuthenticationPrincipal Jwt jwt,
                                            @RequestPart("file") MultipartFile file) {
        return profileImageService.replace(jwt, file);
    }

    @DeleteMapping("/me/profile-image")
    public UserResponse removeProfileImage(@AuthenticationPrincipal Jwt jwt) {
        return profileImageService.remove(jwt);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrentUser(jwt);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSelfAccount(@AuthenticationPrincipal Jwt jwt) {
        userDeletionService.deleteSelfAccount(jwt);
    }

    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse onboard(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OnboardingRequest request) {
        return userService.onboard(jwt, request);
    }
}
