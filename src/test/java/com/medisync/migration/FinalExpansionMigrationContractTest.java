package com.medisync.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FinalExpansionMigrationContractTest {

    @Test
    void v9ContainsRequiredAdditiveSecurityAndPaymentStructures() throws IOException {
        try (var stream = getClass().getResourceAsStream(
                "/db/migration/V9__final_admin_media_payment_expansion.sql")) {
            assertThat(stream).isNotNull();
            String migration = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(migration)
                    .contains("CREATE TABLE user_account_bans")
                    .contains("CREATE TABLE audit_events")
                    .contains("CREATE TABLE consultation_message_attachments")
                    .contains("doctor_fee_amount NUMERIC(10, 2)")
                    .contains("'AWAITING_CONFIRMATION'")
                    .contains("'BANNED'");
            assertThat(migration).doesNotContain("DROP TABLE");
        }
    }
}
