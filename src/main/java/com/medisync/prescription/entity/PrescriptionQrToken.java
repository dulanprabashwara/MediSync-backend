package com.medisync.prescription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescription_qr_tokens")
public class PrescriptionQrToken {

    @Id
    private UUID id;

    @Column(name = "prescription_id", nullable = false, unique = true)
    private UUID prescriptionId;

    @Column(name = "token_hash", unique = true, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String tokenHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected PrescriptionQrToken() {
    }

    public PrescriptionQrToken(UUID prescriptionId, String tokenHash, OffsetDateTime createdAt,
                               OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.prescriptionId = prescriptionId;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public void rotate(String tokenHash, OffsetDateTime createdAt, OffsetDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.revokedAt = null;
    }

    public void revoke(OffsetDateTime now) {
        if (revokedAt == null) {
            revokedAt = now;
        }
    }

    public UUID getId() { return id; }
    public UUID getPrescriptionId() { return prescriptionId; }
    public String getTokenHash() { return tokenHash; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
}
