package com.medisync.notification.controller;

import com.medisync.notification.dto.NotificationPageDto;
import com.medisync.notification.dto.UnreadNotificationCountDto;
import com.medisync.notification.service.NotificationService;
import com.medisync.user.entity.AppUser;
import com.medisync.user.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    public NotificationController(NotificationService notificationService, CurrentUserService currentUserService) {
        this.notificationService = notificationService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<NotificationPageDto> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        AppUser user = currentUserService.requireCurrentUser(jwt);
        
        if (page < 0) page = 0;
        if (size < 1) size = 1;
        if (size > 50) size = 50;
            
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getId(), page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountDto> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        AppUser user = currentUserService.requireCurrentUser(jwt);
        return ResponseEntity.ok(notificationService.getUnreadCount(user.getId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        AppUser user = currentUserService.requireCurrentUser(jwt);
        notificationService.markAsRead(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal Jwt jwt) {
        AppUser user = currentUserService.requireCurrentUser(jwt);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }
}
