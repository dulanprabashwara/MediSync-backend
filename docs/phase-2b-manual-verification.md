# Phase 2B online consultation booking manual verification

Use the hosted development project and existing ACTIVE patient and ACTIVE VERIFIED doctor accounts. Do not reset or delete data. Choose online consultation dates far enough in the future to complete every transition. The API, database, and status checks below deliberately retain the established internal `appointment` terminology.

## Availability and generated slots

1. Sign in as the verified doctor and open `/doctor/availability`.
2. Create a future 09:00–11:00 window with 30-minute online consultations.
3. Confirm exactly four AVAILABLE slots appear: 09:00, 09:30, 10:00, and 10:30.
4. Create 09:00–10:20 with 30-minute online consultations on another date and confirm the partial 10:00–10:20 period is not generated.
5. Attempt an overlapping window and confirm `409 Conflict` with an understandable message.
6. Block and unblock an AVAILABLE slot.
7. Deactivate a window with no requests and confirm its AVAILABLE slots become BLOCKED.
8. Attempt to deactivate a window containing a RESERVED or BOOKED slot and confirm it is rejected without changing the appointment.

## Discovery and booking

1. Sign in as the patient and open `/patient/doctors`.
2. Confirm only ACTIVE VERIFIED doctors with active hospital, department, and specialization data appear.
3. Exercise name, hospital, department, and specialization filters and pagination.
4. Open the verified doctor and query a date range no longer than 31 days.
5. Confirm only AVAILABLE future slots from active windows appear; BLOCKED, RESERVED, and BOOKED slots remain hidden.
6. Select one online consultation time, enter the reason for consultation and symptoms plus optional duration/notes, and submit.
7. Confirm the appointment is REQUESTED, the slot becomes RESERVED, and the slot immediately disappears from discovery.
8. Attempt an overlapping appointment for the same patient and confirm `409 Conflict`.

## Concurrency check

Using two different ACTIVE patient sessions, submit `POST /api/patient/appointments` for the same `slotId` as close together as possible. Exactly one request must return `201 Created`; the other must return `409 Conflict`. Query PostgreSQL and confirm only one `REQUESTED` or `CONFIRMED` appointment exists for that slot.

## Doctor transitions

1. Sign in as the assigned doctor and open `/doctor/appointments`.
2. Confirm the request shows only the assigned patient's identity and submitted symptom description.
3. Accept it and confirm appointment CONFIRMED, `confirmed_at` populated, and slot BOOKED.
4. Submit another patient request, reject it with a meaningful reason, and confirm appointment REJECTED, `rejected_at` populated, and slot AVAILABLE.
5. Confirm the patient sees the rejection reason and the released slot is discoverable again.
6. Confirm another future appointment, then doctor-cancel it with a reason. Confirm CANCELLED_BY_DOCTOR, `cancelled_at` populated, and slot AVAILABLE.

## Patient ownership and cancellation

1. Open `/patient/appointments` and confirm pending, future confirmed, and historical groups show the correct doctor, professional references, time, status, and submitted symptoms.
2. Cancel the patient's own REQUESTED appointment and confirm CANCELLED_BY_PATIENT plus an AVAILABLE slot.
3. Repeat with a CONFIRMED future appointment.
4. Attempt to read or cancel another patient's appointment ID and confirm `403 Forbidden`.
5. Attempt to read or process another doctor's appointment ID and confirm `403 Forbidden`.

## Direct role authorization

1. With a patient JWT, call `/api/doctor/availability`, `/api/doctor/appointments`, and `/api/admin/hospitals`; expect `403`.
2. With a doctor JWT, call `POST /api/patient/appointments`; expect `403`.
3. With a pharmacist JWT, call `/api/patient/doctors`, `/api/doctor/appointments`, and `/api/admin/hospitals`; expect `403`.
4. With an admin JWT, call `POST /api/patient/appointments`; expect `403`.
5. Confirm pending, rejected, suspended, and disabled doctors cannot publish availability or appear in discovery.

## Regression

Repeat login and role redirect checks for patient, doctor, pharmacist, and admin. Confirm doctor verification and hospital, department, and specialization management still work. Run `mvn test`, `mvn package`, `npm run typecheck`, `npm run lint`, and `npm run build` before marking Phase 2B complete.
