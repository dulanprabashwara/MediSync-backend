package com.medisync.admin.controller;

import com.medisync.admin.dto.AdminBanHistoryResponse;

import com.medisync.admin.dto.AdminAccountDeletionRequest;
import com.medisync.admin.dto.AdminUserDetail;
import com.medisync.admin.dto.AdminUserSummary;
import com.medisync.admin.dto.BanUserRequest;
import com.medisync.admin.service.AdminUserService;
import com.medisync.common.dto.PageResponse;
import com.medisync.user.service.UserDeletionService;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.UserRole;
import com.medisync.user.entity.VerificationStatus;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final AdminUserService service;
    private final UserDeletionService userDeletionService;

    public AdminUserController(AdminUserService service, UserDeletionService userDeletionService) {
        this.service = service;
        this.userDeletionService = userDeletionService;
    }

    @GetMapping("/users")
    public PageResponse<AdminUserSummary> users(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime createdTo) {
        return service.list(page, size, q, role, status, verificationStatus, createdFrom, createdTo);
    }

    @GetMapping("/doctors")
    public PageResponse<AdminUserSummary> doctors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) VerificationStatus verificationStatus,
            @RequestParam(required = false) UUID hospitalId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID specializationId) {
        return service.listDoctors(page, size, q, status, verificationStatus,
                hospitalId, departmentId, specializationId);
    }

    @GetMapping("/users/{userId}")
    public AdminUserDetail details(@PathVariable UUID userId) {
        return service.details(userId);
    }

    @PostMapping("/users/{userId}/ban")
    public AdminUserDetail ban(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                               @Valid @RequestBody BanUserRequest request) {
        return service.ban(jwt, userId, request.reason());
    }

    @PostMapping("/users/{userId}/unban")
    public AdminUserDetail unban(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        return service.unban(jwt, userId);
    }

    @DeleteMapping("/users/{userId}")
    public AdminUserDetail deleteAccount(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId,
                                         @Valid @RequestBody AdminAccountDeletionRequest request) {
        userDeletionService.deleteAccountByAdmin(jwt, userId, request.reason());
        return service.details(userId);
    }


}
