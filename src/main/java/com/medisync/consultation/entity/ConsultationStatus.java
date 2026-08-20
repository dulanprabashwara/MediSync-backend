package com.medisync.consultation.entity;

public enum ConsultationStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    public boolean allowsMessages() {
        return this != CANCELLED;
    }

    public boolean allowsClinicalNoteEditing() {
        return this == SCHEDULED || this == IN_PROGRESS;
    }
}
