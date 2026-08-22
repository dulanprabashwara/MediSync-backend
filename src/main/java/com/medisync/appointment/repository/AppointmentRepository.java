package com.medisync.appointment.repository;

import com.medisync.appointment.entity.Appointment;
import com.medisync.appointment.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") UUID id);

    Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);
    Page<Appointment> findByDoctorId(UUID doctorId, Pageable pageable);
    Page<Appointment> findByDoctorIdAndStatus(UUID doctorId, AppointmentStatus status, Pageable pageable);

    @Query("""
            select count(appointment) > 0 from Appointment appointment
            where appointment.patientId = :patientId
              and appointment.status in (
                  com.medisync.appointment.entity.AppointmentStatus.REQUESTED,
                  com.medisync.appointment.entity.AppointmentStatus.CONFIRMED)
              and appointment.scheduledStart < :endsAt
              and appointment.scheduledEnd > :startsAt
            """)
    boolean existsActivePatientOverlap(@Param("patientId") UUID patientId,
                                       @Param("startsAt") OffsetDateTime startsAt,
                                       @Param("endsAt") OffsetDateTime endsAt);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.patientId = :patientId
            and (
                a.status = com.medisync.appointment.entity.AppointmentStatus.REQUESTED
                or (a.status = com.medisync.appointment.entity.AppointmentStatus.CONFIRMED and not exists (
                    select 1 from ConsultationSession c
                    where c.appointmentId = a.id and c.status in (com.medisync.consultation.entity.ConsultationStatus.COMPLETED, com.medisync.consultation.entity.ConsultationStatus.CANCELLED)
                ))
            )
            """)
    boolean hasActiveWorkflowsForPatient(@Param("patientId") UUID patientId);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.doctorId = :doctorId
            and (
                a.status = com.medisync.appointment.entity.AppointmentStatus.REQUESTED
                or (a.status = com.medisync.appointment.entity.AppointmentStatus.CONFIRMED and not exists (
                    select 1 from ConsultationSession c
                    where c.appointmentId = a.id and c.status in (com.medisync.consultation.entity.ConsultationStatus.COMPLETED, com.medisync.consultation.entity.ConsultationStatus.CANCELLED)
                ))
            )
            """)
    boolean hasActiveWorkflowsForDoctor(@Param("doctorId") UUID doctorId);
}
