package com.medisync.audit;

public final class AuditActions {
    public static final String USER_ONBOARDED = "USER_ONBOARDED";
    public static final String USER_BANNED = "USER_BANNED";
    public static final String USER_UNBANNED = "USER_UNBANNED";
    public static final String PROFILE_IMAGE_UPDATED = "PROFILE_IMAGE_UPDATED";
    public static final String PROFILE_IMAGE_REMOVED = "PROFILE_IMAGE_REMOVED";
    public static final String CHAT_MESSAGE_SENT = "CHAT_MESSAGE_SENT";
    public static final String CHAT_IMAGES_SENT = "CHAT_IMAGE_SENT";
    public static final String PRESCRIPTION_CREATED = "PRESCRIPTION_CREATED";
    public static final String PRESCRIPTION_DRAFT_SAVED = "PRESCRIPTION_DRAFT_SAVED";
    public static final String PRESCRIPTION_ISSUED = "PRESCRIPTION_ISSUED";
    public static final String PRESCRIPTION_CANCELLED = "PRESCRIPTION_CANCELLED";
    public static final String DOCTOR_FEE_CONFIRMED = "DOCTOR_PAYMENT_CONFIRMED";
    public static final String QR_TOKEN_CREATED = "QR_GENERATED";
    public static final String PRESCRIPTION_DISPENSED = "PRESCRIPTION_DISPENSED";
    public static final String DOCTOR_VERIFICATION_SUBMITTED = "DOCTOR_VERIFICATION_SUBMITTED";
    public static final String DOCTOR_APPROVED = "DOCTOR_APPROVED";
    public static final String DOCTOR_REJECTED = "DOCTOR_REJECTED";
    public static final String PHARMACIST_VERIFICATION_SUBMITTED = "PHARMACIST_VERIFICATION_SUBMITTED";
    public static final String PHARMACIST_APPROVED = "PHARMACIST_APPROVED";
    public static final String PHARMACIST_REJECTED = "PHARMACIST_REJECTED";
    public static final String HOSPITAL_CREATED = "HOSPITAL_CREATED";
    public static final String HOSPITAL_STATUS_CHANGED = "HOSPITAL_STATUS_CHANGED";
    public static final String DEPARTMENT_CREATED = "DEPARTMENT_CREATED";
    public static final String DEPARTMENT_STATUS_CHANGED = "DEPARTMENT_STATUS_CHANGED";
    public static final String SPECIALIZATION_CREATED = "SPECIALIZATION_CREATED";
    public static final String SPECIALIZATION_STATUS_CHANGED = "SPECIALIZATION_STATUS_CHANGED";
    public static final String AVAILABILITY_CREATED = "AVAILABILITY_CREATED";
    public static final String AVAILABILITY_SLOT_UPDATED = "AVAILABILITY_SLOT_UPDATED";
    public static final String APPOINTMENT_REQUESTED = "APPOINTMENT_REQUESTED";
    public static final String APPOINTMENT_ACCEPTED = "APPOINTMENT_ACCEPTED";
    public static final String APPOINTMENT_REJECTED = "APPOINTMENT_REJECTED";
    public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
    public static final String CONSULTATION_STARTED = "CONSULTATION_STARTED";
    public static final String CONSULTATION_COMPLETED = "CONSULTATION_COMPLETED";
    public static final String CLINICAL_NOTE_UPDATED = "CLINICAL_NOTE_UPDATED";

    private AuditActions() {
    }
}
