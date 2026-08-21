# Phase 5 manual verification

Use separate patient, doctor, pharmacist, and administrator browser sessions. Never paste a real QR payload into logs, URLs, tickets, or persistent browser storage.

## Pharmacist verification

1. Register/onboard a pharmacist and confirm the account is `PENDING_VERIFICATION`.
2. Complete registration number, pharmacy name, address, and optional professional fields.
3. Submit the profile and confirm editing/scanner access is locked while pending.
4. As admin, open **Pending pharmacists**, inspect the submission, and test rejection with a required reason.
5. Correct and resubmit as pharmacist, then approve as admin.
6. Refresh the pharmacist profile and confirm scanner/history access appears only when both account and professional profile are verified.

## Complete prescription and dispensing flow

1. As patient, book a future consultation with an active verified doctor and submit symptoms.
2. As doctor, accept and start the consultation; exchange messages and save a private clinical note.
3. Create and issue a prescription while `IN_PROGRESS`, complete the consultation, and confirm chat remains writable.
4. As patient, generate a QR. Confirm it contains only `MEDISYNC:RX:<opaque-token>` and keep it out of URLs/storage.
5. As verified pharmacist, scan with the camera and repeat with manual input. Confirm only dispensing-safe data appears—never symptoms, chat, or clinical notes.
6. Select **Dispense Prescription**, cancel once, then explicitly confirm whole-prescription dispensing.
7. Confirm success metadata, the owned dispensing-history entry, patient **Dispensed** status, and doctor **Dispensed** status.
8. Confirm the doctor cancellation action and patient QR generation action are unavailable.
9. Scan the original QR again and confirm no medication regimen or second dispensing is offered.

## Negative and concurrency checks

- Verify wrong-prefix, malformed, random, rotated, revoked, expired, and cancelled QR payloads cannot dispense.
- Confirm pending/rejected pharmacists and patient/doctor/admin roles receive `403` from pharmacy operations.
- Submit the same final dispense action in two verified-pharmacist sessions; exactly one succeeds and one dispensing row exists.
- Verify pharmacist A cannot open pharmacist B's history detail.
- Confirm raw QR payloads and token hashes do not appear in server/browser logs.

## Final regression

Quickly check authentication and role routing, doctor verification/reference data, future availability, booking and cancellation, consultation lifecycle, real-time/post-completion chat, private notes, draft/issue/discard/cancel prescription behavior, cancelled-regimen hiding, and hashed QR rotation.
