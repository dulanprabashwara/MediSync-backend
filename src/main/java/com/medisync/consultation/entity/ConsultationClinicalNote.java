package com.medisync.consultation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "consultation_clinical_notes")
public class ConsultationClinicalNote {

    @Id
    private UUID id;

    @Column(name = "consultation_id", nullable = false, unique = true)
    private UUID consultationId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "note_text", nullable = false, columnDefinition = "TEXT")
    private String noteText;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ConsultationClinicalNote() {
    }

    public ConsultationClinicalNote(UUID consultationId, UUID doctorId, String noteText) {
        this.id = UUID.randomUUID();
        this.consultationId = consultationId;
        this.doctorId = doctorId;
        this.noteText = noteText;
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

    public void update(String noteText) {
        this.noteText = noteText;
    }

    public UUID getId() { return id; }
    public UUID getConsultationId() { return consultationId; }
    public UUID getDoctorId() { return doctorId; }
    public String getNoteText() { return noteText; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
