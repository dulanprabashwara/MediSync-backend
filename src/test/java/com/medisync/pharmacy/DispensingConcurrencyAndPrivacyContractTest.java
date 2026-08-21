package com.medisync.pharmacy;

import com.medisync.pharmacy.dto.PharmacyPrescriptionVerificationResponse;
import com.medisync.prescription.repository.PrescriptionQrTokenRepository;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DispensingConcurrencyAndPrivacyContractTest {

    @Test
    void dispensingLocksQrBeforeResolvingPrescription() throws Exception {
        Method method = PrescriptionQrTokenRepository.class.getMethod("findByTokenHashForUpdate", String.class);
        assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void v8EnforcesOneDispensingPerPrescriptionAndKeepsEarlierMigrationsUntouchedByDesign() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/"
                + "V8__phase_5_pharmacist_verification_and_dispensing.sql"));
        assertThat(migration).contains("uk_prescription_dispensations_prescription UNIQUE (prescription_id)")
                .contains("idx_prescription_dispensations_pharmacist_time")
                .doesNotContain("DROP TABLE")
                .doesNotContain("TRUNCATE")
                .doesNotContain("DELETE FROM");
    }

    @Test
    void pharmacistVerificationDtoCannotExposeClinicalOrTokenData() {
        Set<String> fields = Arrays.stream(PharmacyPrescriptionVerificationResponse.class.getRecordComponents())
                .map(component -> component.getName().toLowerCase()).collect(Collectors.toSet());
        assertThat(fields).doesNotContain("symptoms", "reasonforvisit", "chat", "clinicalnote", "tokenhash",
                "qrpayload", "patientemail", "patientphone", "patientid", "doctorid", "prescriptionid");
    }
}
