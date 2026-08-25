package com.medisync.notification.realtime;

import com.medisync.notification.events.NotificationCreatedEvent;
import com.medisync.user.entity.AppUser;
import com.medisync.user.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Component
public class NotificationRealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationRealtimePublisher.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    private final AppUserRepository appUserRepository;

    public NotificationRealtimePublisher(SimpMessagingTemplate messagingTemplate, AppUserRepository appUserRepository) {
        this.messagingTemplate = messagingTemplate;
        this.appUserRepository = appUserRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreatedEvent(NotificationCreatedEvent event) {
        log.debug("Publishing notification to user {}", event.getRecipientUserId());
        try {
            Optional<AppUser> userOpt = appUserRepository.findById(event.getRecipientUserId());
            if (userOpt.isPresent()) {
                messagingTemplate.convertAndSendToUser(
                        userOpt.get().getAuthUserId().toString(),
                        "/queue/notifications",
                        event.getNotificationDto()
                );
            }
        } catch (Exception e) {
            log.error("Failed to publish real-time notification to user {}", event.getRecipientUserId(), e);
        }
    }
}
