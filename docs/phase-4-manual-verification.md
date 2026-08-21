# Phase 4 manual verification

Run these checks with an ACTIVE VERIFIED doctor, an ACTIVE patient assigned to that doctor, and an unrelated patient/doctor. Record actual results; do not mark a check complete without performing it.

## Migration and startup

- Start the backend against hosted PostgreSQL and confirm Flyway applies V6 once.
- Confirm Hibernate schema validation succeeds and `GET /api/health` returns `UP`.
- Confirm V1–V5 checksums remain valid and no Flyway clean/repair was used.

## Doctor lifecycle

- Open a scheduled consultation and create a draft; repeat and confirm the same draft opens.
- Save zero medicines as a draft, then add, remove, reorder, and save up to 20 medicines.
- Confirm issuing a scheduled-consultation draft is rejected.
- Start the consultation, issue a draft with at least one medicine, and confirm it becomes immutable.
- Confirm a second issue and any edit of the issued prescription are rejected.
- Complete another consultation and confirm its valid draft may be issued.
- Cancel an issued prescription with a reason and confirm its QR is revoked.
- Confirm prescriptions cannot be created or edited for a cancelled consultation.

## Patient and privacy

- Confirm the owning patient sees issued/cancelled prescriptions but never drafts.
- Confirm patient list responses contain no QR token or payload.
- Confirm an active detail page renders a QR while an expired/cancelled one does not.
- Confirm the QR decodes only to `MEDISYNC:RX:<opaque-token>` and contains no identity or clinical data.
- Confirm unrelated patients and doctors cannot access the prescription.
- Confirm patient, pharmacist, and admin accounts cannot access doctor prescription routes.
- Confirm no public or pharmacist QR verification/scanning endpoint exists.

## Regression

- Re-run Phase 1 authentication, Phase 2 booking, and Phase 3 lifecycle/chat/clinical-note checks.
- Confirm the pharmacist portal still presents Phase 5 functionality only as unavailable placeholders.
