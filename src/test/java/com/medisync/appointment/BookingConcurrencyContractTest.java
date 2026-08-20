package com.medisync.appointment;

import com.medisync.appointment.entity.AppointmentStatus;
import com.medisync.appointment.repository.AppointmentRepository;
import com.medisync.availability.entity.SlotStatus;
import com.medisync.availability.repository.AppointmentSlotRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BookingConcurrencyContractTest {

    @Test
    void slotBookingRepositoryUsesPessimisticWriteLock() throws Exception {
        Method method = AppointmentSlotRepository.class.getMethod("findByIdForUpdate", UUID.class);
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void patientConflictChecksAreSerializedWithPessimisticWriteLock() throws Exception {
        Method method = com.medisync.user.repository.PatientProfileRepository.class
                .getMethod("findByUserIdForUpdate", UUID.class);
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void migrationEnforcesOneActiveAppointmentPerSlotWhileAllowingHistory() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/"
                + "V4__phase_2b_availability_and_appointments.sql"));
        assertThat(migration).contains("CREATE UNIQUE INDEX uk_appointments_active_slot")
                .contains("WHERE status IN ('REQUESTED', 'CONFIRMED')")
                .doesNotContain("CONSTRAINT uk_appointments_slot UNIQUE");
    }

    @Test
    void allDatabaseStateValuesAreExplicitStrings() {
        assertThat(List.of(SlotStatus.values()).stream().map(Enum::name))
                .containsExactly("AVAILABLE", "RESERVED", "BOOKED", "BLOCKED");
        assertThat(List.of(AppointmentStatus.values()).stream().map(Enum::name))
                .containsExactly("REQUESTED", "CONFIRMED", "REJECTED", "CANCELLED_BY_PATIENT",
                        "CANCELLED_BY_DOCTOR");
    }

    @Test
    void appointmentTransitionsLockAppointmentRows() throws Exception {
        Method method = AppointmentRepository.class.getMethod("findByIdForUpdate", UUID.class);
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
