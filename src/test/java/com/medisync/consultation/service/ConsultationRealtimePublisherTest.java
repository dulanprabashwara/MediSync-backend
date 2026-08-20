package com.medisync.consultation.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.consultation.dto.ConsultationEvent;
import com.medisync.consultation.entity.ConsultationStatus;
import com.medisync.user.entity.AccountStatus;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.entity.UserRole;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationRealtimePublisherTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock PatientProfileRepository patientProfileRepository;
    @Mock DoctorProfileRepository doctorProfileRepository;
    @Mock AppUserRepository appUserRepository;

    @AfterEach
    void cleanSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void userQueueEventIsPublishedOnlyAfterCommit() {
        AppUser patientUser = new AppUser(UUID.randomUUID(), "patient@example.com", "Mala", "Silva", null,
                UserRole.PATIENT, AccountStatus.ACTIVE);
        AppUser doctorUser = new AppUser(UUID.randomUUID(), "doctor@example.com", "Nimal", "Perera", null,
                UserRole.DOCTOR, AccountStatus.ACTIVE);
        PatientProfile patient = new PatientProfile(patientUser.getId());
        DoctorProfile doctor = new DoctorProfile(doctorUser.getId());
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
        Appointment appointment = new Appointment(patient.getId(), doctor.getId(), UUID.randomUUID(), start,
                start.plusMinutes(30));
        UUID consultationId = UUID.randomUUID();
        ConsultationEvent event = ConsultationEvent.statusChanged(consultationId, ConsultationStatus.SCHEDULED);
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorProfileRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(appUserRepository.findById(patientUser.getId())).thenReturn(Optional.of(patientUser));
        when(appUserRepository.findById(doctorUser.getId())).thenReturn(Optional.of(doctorUser));
        ConsultationRealtimePublisher publisher = new ConsultationRealtimePublisher(
                messagingTemplate, patientProfileRepository, doctorProfileRepository, appUserRepository);
        TransactionSynchronizationManager.initSynchronization();

        publisher.publishAfterCommit(appointment, event);
        verify(messagingTemplate, never()).convertAndSendToUser(
                patientUser.getAuthUserId().toString(), "/queue/consultation-events", event);

        TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());

        verify(messagingTemplate).convertAndSendToUser(
                eq(patientUser.getAuthUserId().toString()), eq("/queue/consultation-events"), eq(event));
        verify(messagingTemplate).convertAndSendToUser(
                eq(doctorUser.getAuthUserId().toString()), eq("/queue/consultation-events"), eq(event));
    }
}
