package com.medisync.appointment.entity;

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
@Table(name = "appointment_symptoms")
public class AppointmentSymptoms {

    @Id
    private UUID id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private UUID appointmentId;

    @Column(name = "reason_for_visit", nullable = false, length = 300)
    private String reasonForVisit;

    @Column(nullable = false, length = 2000)
    private String symptoms;

    @Column(name = "symptom_duration", length = 200)
    private String symptomDuration;

    @Column(name = "additional_notes", length = 2000)
    private String additionalNotes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected AppointmentSymptoms() {
    }

    public AppointmentSymptoms(UUID appointmentId, String reasonForVisit, String symptoms,
                               String symptomDuration, String additionalNotes) {
        this.id = UUID.randomUUID();
        this.appointmentId = appointmentId;
        this.reasonForVisit = reasonForVisit;
        this.symptoms = symptoms;
        this.symptomDuration = symptomDuration;
        this.additionalNotes = additionalNotes;
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

    public UUID getId() { return id; }
    public UUID getAppointmentId() { return appointmentId; }
    public String getReasonForVisit() { return reasonForVisit; }
    public String getSymptoms() { return symptoms; }
    public String getSymptomDuration() { return symptomDuration; }
    public String getAdditionalNotes() { return additionalNotes; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
