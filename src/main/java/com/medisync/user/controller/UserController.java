package com.medisync.user.controller;

import com.medisync.user.dto.OnboardingRequest;
import com.medisync.user.dto.UserResponse;
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

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrentUser(jwt);
    }

    @PostMapping("/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse onboard(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OnboardingRequest request) {
        return userService.onboard(jwt, request);
    }
}
