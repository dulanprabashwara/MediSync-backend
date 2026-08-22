package com.medisync.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_account_bans")
public class UserAccountBan {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "banned_by", nullable = false)
    private UUID bannedBy;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 32)
    private AccountStatus previousStatus;

    @Column(name = "banned_at", nullable = false)
    private OffsetDateTime bannedAt;

    @Column(name = "unbanned_by")
    private UUID unbannedBy;

    @Column(name = "unbanned_at")
    private OffsetDateTime unbannedAt;

    protected UserAccountBan() {
    }

    public UserAccountBan(UUID userId, UUID bannedBy, String reason,
                          AccountStatus previousStatus, OffsetDateTime bannedAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.bannedBy = bannedBy;
        this.reason = reason;
        this.previousStatus = previousStatus;
        this.bannedAt = bannedAt;
    }

    public void close(UUID adminUserId, OffsetDateTime now) {
        this.unbannedBy = adminUserId;
        this.unbannedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getBannedBy() { return bannedBy; }
    public String getReason() { return reason; }
    public AccountStatus getPreviousStatus() { return previousStatus; }
    public OffsetDateTime getBannedAt() { return bannedAt; }
    public UUID getUnbannedBy() { return unbannedBy; }
    public OffsetDateTime getUnbannedAt() { return unbannedAt; }
}
