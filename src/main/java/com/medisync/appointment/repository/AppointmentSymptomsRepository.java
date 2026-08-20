package com.medisync.appointment.repository;

import com.medisync.appointment.entity.AppointmentSymptoms;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppointmentSymptomsRepository extends JpaRepository<AppointmentSymptoms, UUID> {
    Optional<AppointmentSymptoms> findByAppointmentId(UUID appointmentId);
}
