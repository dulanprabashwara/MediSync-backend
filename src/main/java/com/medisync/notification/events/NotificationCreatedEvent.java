package com.medisync.notification.events;

import com.medisync.notification.dto.NotificationDto;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

public class NotificationCreatedEvent extends ApplicationEvent {
    
    private final UUID recipientUserId;
    private final NotificationDto notificationDto;

    public NotificationCreatedEvent(Object source, UUID recipientUserId, NotificationDto notificationDto) {
        super(source);
        this.recipientUserId = recipientUserId;
        this.notificationDto = notificationDto;
    }

    public UUID getRecipientUserId() {
        return recipientUserId;
    }

    public NotificationDto getNotificationDto() {
        return notificationDto;
    }
}
