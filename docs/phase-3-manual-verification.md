# Phase 3 manual verification

Use separate patient and doctor browser profiles so each keeps an independent Supabase session. Never copy access tokens into URLs or logs.

## Setup

1. Start the backend and frontend with the hosted development database.
2. Sign in as an ACTIVE patient in one browser profile.
3. Sign in as the assigned ACTIVE VERIFIED doctor in another profile.
4. Use an existing confirmed appointment or request and accept a new future consultation.
5. Confirm both appointment lists show one `SCHEDULED` consultation and open the same consultation room.

## Lifecycle and live chat

1. Send `Hello doctor` from the patient room. Confirm the REST request succeeds, the message appears immediately in the doctor room without refresh, and refreshing both rooms preserves it.
2. Reply from the doctor room. Confirm the patient receives it without refresh and neither sender sees a duplicate.
3. Temporarily stop the backend or disconnect the network, then restore it. Confirm the UI reports the live disconnect, reconnects with the current session, and reloads REST history.
4. Start the consultation as the doctor. Confirm both rooms change to `IN_PROGRESS` and an invalid second start is rejected.
5. Save a private clinical note. Confirm it survives a doctor-page refresh and no patient API response, screen, or WebSocket event contains it.
6. Complete the consultation. Confirm both rooms show `COMPLETED`, the clinical note is read-only, and both participants can still exchange and refresh chat messages.

## Cancellation

1. For a different `SCHEDULED` confirmed consultation, cancel through either existing appointment workflow.
2. Confirm the appointment cancellation and consultation `CANCELLED` state happen together.
3. Confirm existing history remains visible and both chat inputs are read-only.
4. Confirm cancellation of an `IN_PROGRESS` or `COMPLETED` consultation returns a conflict and leaves both appointment and slot states unchanged.

## Authorization and privacy

1. As another patient, try the first patient's consultation URL and REST endpoints. Expect `403`.
2. As another doctor, try the assigned doctor's consultation, message, lifecycle, and clinical-note endpoints. Expect `403`.
3. As the assigned patient, call the doctor's clinical-note route. Expect `403` and no note data.
4. Repeat the clinical-note request as a pharmacist and administrator. Expect `403`.
5. Attempt STOMP `CONNECT` without a token and with an invalid or expired token. Expect rejection.
6. Attempt STOMP `CONNECT` as a pharmacist or administrator. Expect rejection.
7. Confirm there is no arbitrary conversation-creation endpoint and no way to message a doctor without a confirmed consultation.

## Regression

Run the Phase 2B availability, booking, overlap, transition, and authorization checks in `docs/phase-2b-manual-verification.md`. Also confirm existing onboarding, role routing, administrator verification, and pharmacist portal behavior remain unchanged.
