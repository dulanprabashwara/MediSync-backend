package com.medisync.consultation.service;

import com.medisync.appointment.entity.Appointment;
import com.medisync.consultation.dto.ConsultationEvent;
import com.medisync.exception.ResourceNotFoundException;
import com.medisync.user.entity.AppUser;
import com.medisync.user.entity.DoctorProfile;
import com.medisync.user.entity.PatientProfile;
import com.medisync.user.repository.AppUserRepository;
import com.medisync.user.repository.DoctorProfileRepository;
import com.medisync.user.repository.PatientProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ConsultationRealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(ConsultationRealtimePublisher.class);
    private static final String USER_QUEUE = "/queue/consultation-events";

    private final SimpMessagingTemplate messagingTemplate;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppUserRepository appUserRepository;

    public ConsultationRealtimePublisher(SimpMessagingTemplate messagingTemplate,
                                         PatientProfileRepository patientProfileRepository,
                                         DoctorProfileRepository doctorProfileRepository,
                                         AppUserRepository appUserRepository) {
        this.messagingTemplate = messagingTemplate;
        this.patientProfileRepository = patientProfileRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appUserRepository = appUserRepository;
    }

    public void publishAfterCommit(Appointment appointment, ConsultationEvent event) {
        PatientProfile patient = patientProfileRepository.findById(appointment.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        DoctorProfile doctor = doctorProfileRepository.findById(appointment.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found"));
        AppUser patientUser = appUserRepository.findById(patient.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient account not found"));
        AppUser doctorUser = appUserRepository.findById(doctor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor account not found"));

        Runnable publish = () -> {
            try {
                messagingTemplate.convertAndSendToUser(patientUser.getAuthUserId().toString(), USER_QUEUE, event);
                messagingTemplate.convertAndSendToUser(doctorUser.getAuthUserId().toString(), USER_QUEUE, event);
            } catch (RuntimeException exception) {
                log.warn("Consultation event delivery failed after database commit for consultation {}",
                        event.consultationId(), exception);
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Consultation events must be registered inside a transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }
}
