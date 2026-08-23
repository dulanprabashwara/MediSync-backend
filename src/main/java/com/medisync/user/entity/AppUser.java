package com.medisync.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    private UUID id;

    @Column(name = "auth_user_id", nullable = false, unique = true)
    private UUID authUserId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "profile_image_key", length = 500)
    private String profileImageKey;

    @Column(name = "profile_image_updated_at")
    private OffsetDateTime profileImageUpdatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deleted_by_user_id")
    private UUID deletedByUserId;

    @Column(name = "deletion_reason")
    private String deletionReason;

    @Column(name = "deletion_source", length = 50)
    private String deletionSource;

    protected AppUser() {
    }

    public AppUser(UUID authUserId, String email, String firstName, String lastName, String phone,
                   UserRole role, AccountStatus status) {
        this.id = UUID.randomUUID();
        this.authUserId = authUserId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.role = role;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }

    public void updateProfile(String firstName, String lastName, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
    }

    public void ban() {
        this.status = AccountStatus.BANNED;
    }

    public void restoreStatus(AccountStatus previousStatus) {
        if (previousStatus == null || previousStatus == AccountStatus.BANNED) {
            throw new IllegalArgumentException("A valid pre-ban account status is required");
        }
        this.status = previousStatus;
    }

    public void replaceProfileImage(String storageKey, OffsetDateTime updatedAt) {
        this.profileImageKey = storageKey;
        this.profileImageUpdatedAt = updatedAt;
    }

    public void removeProfileImage() {
        this.profileImageKey = null;
        this.profileImageUpdatedAt = null;
    }

    public void delete(String anonymizedFirstName, String anonymizedLastName, UUID deletedByUserId, String deletionReason, String deletionSource) {
        this.status = AccountStatus.DELETED;
        this.deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.deletedByUserId = deletedByUserId;
        this.deletionReason = deletionReason;
        this.deletionSource = deletionSource;
        this.email = "deleted+" + this.id + "@medisync.invalid";
        this.firstName = anonymizedFirstName;
        this.lastName = anonymizedLastName;
        this.phone = null;
        this.profileImageKey = null;
        this.profileImageUpdatedAt = null;
    }

    public UUID getId() { return id; }
    public UUID getAuthUserId() { return authUserId; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPhone() { return phone; }
    public UserRole getRole() { return role; }
    public AccountStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public String getProfileImageKey() { return profileImageKey; }
    public OffsetDateTime getProfileImageUpdatedAt() { return profileImageUpdatedAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public UUID getDeletedByUserId() { return deletedByUserId; }
    public String getDeletionReason() { return deletionReason; }
    public String getDeletionSource() { return deletionSource; }
}
