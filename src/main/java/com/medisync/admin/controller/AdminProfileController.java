package com.medisync.admin.controller;

import com.medisync.admin.dto.AdminSelfProfileUpdateRequest;
import com.medisync.admin.service.AdminUserService;
import com.medisync.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminProfileController {

    private final AdminUserService adminUserService;

    public AdminProfileController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PatchMapping("/profile")
    public UserResponse updateSelfProfile(@AuthenticationPrincipal Jwt jwt,
                                          @Valid @RequestBody AdminSelfProfileUpdateRequest request) {
        return adminUserService.updateSelfProfile(jwt, request);
    }
}
