package com.medisync.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientUserId(UUID recipientUserId, Pageable pageable);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientUser.id = :recipientUserId AND n.read = false")
    long countUnreadByRecipientUserId(@Param("recipientUserId") UUID recipientUserId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.recipientUser.id = :recipientUserId AND n.read = false")
    int markAllAsRead(@Param("recipientUserId") UUID recipientUserId);
    
    boolean existsByDedupeKey(String dedupeKey);
}
