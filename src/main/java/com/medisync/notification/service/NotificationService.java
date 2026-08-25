package com.medisync.notification.service;

import com.medisync.notification.Notification;
import com.medisync.notification.NotificationRepository;
import com.medisync.notification.NotificationType;
import com.medisync.notification.dto.NotificationDto;
import com.medisync.notification.dto.NotificationPageDto;
import com.medisync.notification.dto.UnreadNotificationCountDto;
import com.medisync.notification.events.NotificationCreatedEvent;
import com.medisync.user.entity.AppUser;
import com.medisync.user.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(NotificationRepository notificationRepository, AppUserRepository userRepository, ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public NotificationPageDto getUserNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> notificationPage = notificationRepository.findByRecipientUserId(userId, pageable);
        
        List<NotificationDto> content = notificationPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new NotificationPageDto(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages(),
                notificationPage.isLast()
        );
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountDto getUnreadCount(UUID userId) {
        long count = notificationRepository.countUnreadByRecipientUserId(userId);
        return new UnreadNotificationCountDto(count);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
                
        if (!notification.getRecipientUser().getId().equals(userId)) {
            throw new SecurityException("Cannot access another user's notification");
        }
        
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(java.time.OffsetDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void createNotification(UUID recipientUserId, UUID actorUserId, NotificationType type,
                                   String title, String message, String actionUrl,
                                   String entityType, UUID entityId, String dedupeKey) {
                                   
        if (dedupeKey != null && notificationRepository.existsByDedupeKey(dedupeKey)) {
            log.debug("Skipping duplicate notification with key: {}", dedupeKey);
            return;
        }

        AppUser recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient user not found"));

        AppUser actor = null;
        if (actorUserId != null) {
            actor = userRepository.findById(actorUserId).orElse(null);
        }

        Notification notification = new Notification();
        notification.setRecipientUser(recipient);
        notification.setActorUser(actor);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setDedupeKey(dedupeKey);

        Notification savedNotification = notificationRepository.save(notification);
        
        NotificationDto dto = mapToDto(savedNotification);
        eventPublisher.publishEvent(new NotificationCreatedEvent(this, recipientUserId, dto));
    }

    private NotificationDto mapToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setMessage(notification.getMessage());
        dto.setActionUrl(notification.getActionUrl());
        dto.setEntityType(notification.getEntityType());
        dto.setEntityId(notification.getEntityId());
        dto.setRead(notification.isRead());
        dto.setReadAt(notification.getReadAt());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
