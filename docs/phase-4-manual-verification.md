# Phase 4 manual verification

Run these checks with an ACTIVE VERIFIED doctor, an ACTIVE patient assigned to that doctor, and an unrelated patient/doctor. Record actual results; do not mark a check complete without performing it.

## Migration and startup

- Start the backend against hosted PostgreSQL and confirm Flyway applies V7 once.
- Confirm Hibernate schema validation succeeds and `GET /api/health` returns `UP`.
- Confirm V1–V6 checksums remain valid and no Flyway clean/repair was used.
- Confirm `prescription_qr_tokens` has `token_hash` but no raw `token` column and all legacy rows were revoked.

## Doctor lifecycle

- Open a scheduled consultation and create a draft; repeat and confirm the same draft opens.
- Save zero medicines as a draft, then add, remove, reorder, and save up to 20 medicines.
- Confirm issuing a scheduled-consultation draft is rejected.
- Start the consultation, issue a draft with at least one medicine, and confirm it becomes immutable.
- Confirm a second issue and any edit of the issued prescription are rejected.
- Attempt to complete a consultation with a draft and confirm completion is rejected until the draft is issued or discarded.
- Confirm only an `IN_PROGRESS` consultation can issue, then complete it and verify prescribing controls disappear while chat remains writable.
- Confirm a legacy draft attached to a completed consultation is read-only but can be discarded.
- Cancel an issued prescription with a reason and confirm its QR is revoked.
- Confirm prescriptions cannot be created or edited for a cancelled consultation.

## Patient and privacy

- Confirm the owning patient sees issued/cancelled prescriptions but never drafts.
- Confirm patient list responses contain no QR token or payload.
- Confirm an active detail page shows Generate QR, returns/renders a QR only after the patient clicks, and shows Generate QR again after refresh.
- Generate twice and confirm the second QR differs and replaces the database hash for the first.
- Confirm an expired/cancelled prescription has no QR generation control and the endpoint rejects generation.
- Confirm cancelled patient and doctor details show cancellation metadata but no medicine regimen or general instructions.
- Confirm the QR decodes only to `MEDISYNC:RX:<opaque-token>` and contains no identity or clinical data.
- Confirm unrelated patients and doctors cannot access the prescription.
- Confirm patient, pharmacist, and admin accounts cannot access doctor prescription routes.
- Confirm no public or pharmacist QR verification/scanning endpoint exists.

## Availability hardening

- Confirm past or exactly-current availability starts are rejected and a future start succeeds.
- Confirm fully expired windows and elapsed slots disappear without deleting database history.
- Confirm patient slots are AVAILABLE, future, and beyond the configured booking lead time.
- Leave a booking page open until a slot crosses its eligibility boundary and confirm the locked backend booking attempt is rejected.

## Regression

- Re-run Phase 1 authentication, Phase 2 booking, and Phase 3 lifecycle/chat/clinical-note checks.
- Confirm the pharmacist portal still presents Phase 5 functionality only as unavailable placeholders.
