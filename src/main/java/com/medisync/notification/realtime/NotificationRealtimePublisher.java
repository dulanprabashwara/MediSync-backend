package com.medisync.notification.realtime;

import com.medisync.notification.events.NotificationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationRealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationRealtimePublisher.class);
    
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        log.debug("Publishing notification to user {}", event.getRecipientUserId());
        try {
            messagingTemplate.convertAndSendToUser(
                    event.getRecipientUserId().toString(),
                    "/queue/notifications",
                    event.getNotificationDto()
            );
        } catch (Exception e) {
            log.error("Failed to publish real-time notification to user {}", event.getRecipientUserId(), e);
        }
    }
}
