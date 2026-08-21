# MediSync API

Spring Boot API for MediSync through Phase 4. MediSync is an online patient-care platform designed to connect patients with verified doctors for scheduled online consultations and reduce unnecessary hospital visits. The API owns application users, role authorization, professional verification, date-based doctor availability, doctor discovery, consultation booking, consultation lifecycle, persistent chat, private clinical notes, and digital prescriptions while Supabase Auth owns credentials and sessions.

Backend tables, Java types, enums, and API routes retain the established `appointment` terminology. In Phase 2 these records represent scheduled online consultations, not physical hospital visits.

## Requirements

- Java 17 or newer (Java 21 LTS is recommended for new development environments)
- Maven 3.6.3 or newer
- Access to the existing hosted Supabase PostgreSQL project
- A Supabase project configured with asymmetric JWT signing keys so its JWKS endpoint can verify access tokens

## Configuration

Copy `.env.example` into your preferred local environment manager and provide the missing developer-owned values. Spring reads these variables directly; it does not load `.env` files by itself.

| Variable | Purpose |
| --- | --- |
| `DB_HOST` | Hosted PostgreSQL hostname |
| `DB_PORT` | PostgreSQL port |
| `DB_NAME` | Database name |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password (required; never commit it) |
| `SUPABASE_URL` | Supabase project URL used to validate the JWT issuer |
| `SUPABASE_JWKS_URL` | Supabase Auth JSON Web Key Set endpoint |
| `SUPABASE_JWT_AUDIENCE` | Expected access-token audience, normally `authenticated` |
| `FRONTEND_URL` | Exact allowed CORS origin |
| `APPOINTMENT_MIN_LEAD_MINUTES` | Optional minimum lead time for a new booking; defaults to `0` |

## Run

```powershell
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. The public readiness check is:

```text
GET http://localhost:8080/api/health
```

Authenticated requests must include `Authorization: Bearer <Supabase access token>`.

## Phase 1 API

| Endpoint | Access | Purpose |
| --- | --- | --- |
| `GET /api/health` | Public | API status |
| `GET /api/users/me` | Authenticated | Current MediSync profile, or `ONBOARDING_REQUIRED` |
| `POST /api/users/onboarding` | Authenticated | Transactionally creates a user and role profile |
| `GET /api/{role}/profile` | Matching database role | Verifies role-specific access |

Public onboarding accepts only `PATIENT`, `DOCTOR`, and `PHARMACIST`. Patients become `ACTIVE`; doctors and pharmacists become `PENDING_VERIFICATION`. Pending professionals can open their own profile portal but the broader role route policy requires `ACTIVE`, ready for later clinical endpoints.

## Phase 2B online consultation booking API

| Endpoint | Access | Purpose |
| --- | --- | --- |
| `GET/POST /api/doctor/availability` | ACTIVE VERIFIED doctor | List or create a date-based window and generated slots |
| `PATCH /api/doctor/availability/{id}/deactivate` | Owning doctor | Safely deactivate a future window |
| `POST /api/doctor/availability/slots/{id}/block` | Owning doctor | Block an AVAILABLE slot |
| `POST /api/doctor/availability/slots/{id}/unblock` | Owning doctor | Restore a BLOCKED slot in an active window |
| `GET /api/patient/doctors` | ACTIVE patient | Paginated verified-doctor search |
| `GET /api/patient/doctors/{id}` | ACTIVE patient | Patient-safe doctor profile |
| `GET /api/patient/doctors/{id}/slots` | ACTIVE patient | AVAILABLE future slots in a maximum 31-day date range |
| `POST /api/patient/appointments` | ACTIVE patient | Atomically request a slot and submit symptoms |
| `GET /api/patient/appointments[/{id}]` | Owning patient | List or view own appointments |
| `POST /api/patient/appointments/{id}/cancel` | Owning patient | Cancel an eligible request/confirmation |
| `GET /api/doctor/appointments[/{id}]` | Assigned ACTIVE VERIFIED doctor | List or view assigned appointments |
| `POST /api/doctor/appointments/{id}/accept` | Assigned doctor | REQUESTED → CONFIRMED and RESERVED → BOOKED |
| `POST /api/doctor/appointments/{id}/reject` | Assigned doctor | Reject with a reason and release the slot |
| `POST /api/doctor/appointments/{id}/cancel` | Assigned doctor | Cancel a future CONFIRMED appointment with a reason |

Slot states are `AVAILABLE`, `RESERVED`, `BOOKED`, and `BLOCKED`. Appointment states are `REQUESTED`, `CONFIRMED`, `REJECTED`, `CANCELLED_BY_PATIENT`, and `CANCELLED_BY_DOCTOR`. The browser supplies only a `slotId`; the server derives the doctor and absolute timestamps from PostgreSQL.

Booking locks the patient profile and selected slot with `PESSIMISTIC_WRITE`. The patient lock serializes overlapping-appointment checks, the slot lock serializes competing requests, and PostgreSQL's `uk_appointments_active_slot` partial unique index independently limits a slot to one `REQUESTED` or `CONFIRMED` appointment.

## Phase 3 online consultation API

| Endpoint | Access | Purpose |
| --- | --- | --- |
| `GET /api/patient/consultations/{id}` | Owning ACTIVE patient | Patient-safe consultation details and lifecycle status |
| `GET/POST /api/patient/consultations/{id}/messages` | Owning ACTIVE patient | Paginated history or persistent plain-text message creation |
| `GET /api/doctor/consultations/{id}` | Assigned ACTIVE VERIFIED doctor | Consultation details and lifecycle status |
| `GET/POST /api/doctor/consultations/{id}/messages` | Assigned ACTIVE VERIFIED doctor | Paginated history or persistent plain-text message creation |
| `POST /api/doctor/consultations/{id}/start` | Assigned ACTIVE VERIFIED doctor | `SCHEDULED` to `IN_PROGRESS` |
| `POST /api/doctor/consultations/{id}/complete` | Assigned ACTIVE VERIFIED doctor | `IN_PROGRESS` to `COMPLETED` |
| `GET/PUT /api/doctor/consultations/{id}/clinical-note` | Assigned ACTIVE VERIFIED doctor | Read or save the doctor's private note |
| `STOMP /ws` | ACTIVE patient or ACTIVE VERIFIED doctor | Authenticated live consultation events |

The consultation lifecycle is `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, or `CANCELLED`. Accepting an appointment transactionally ensures exactly one `SCHEDULED` session. Cancelling an eligible confirmed appointment also marks its scheduled session `CANCELLED`; cancellation is rejected once the session is in progress or completed. Completion does not change the Phase 2 appointment status.

Chat is consultation-scoped and is never a generic patient-to-doctor channel. Messages are immutable, plain text, limited to 4,000 characters, and created and persisted through authenticated REST APIs. After the transaction commits, Spring publishes a STOMP event to each participant's authenticated `/user/queue/consultation-events` destination. REST history remains the source of truth, so reconnecting clients reconcile persisted history and a WebSocket outage cannot lose a successfully saved message. Chat remains writable after completion and becomes read-only after cancellation.

The STOMP `CONNECT` frame carries the existing Supabase access token in its native `Authorization: Bearer ...` header. The token is never placed in the WebSocket URL. The same configured `JwtDecoder`, issuer, audience, and JWKS validation used by REST authenticate the connection. Only the consultation event user queue may be subscribed to, and client `SEND` frames are rejected because all writes go through REST authorization and persistence.

Clinical notes use a separate doctor-only endpoint and response model. They are never included in patient consultation, message, or WebSocket payloads. The assigned doctor may edit the note while the session is scheduled or in progress; it becomes read-only after completion (and remains read-only for a cancelled session).

## Phase 4 digital prescription API

| Endpoint | Access | Purpose |
| --- | --- | --- |
| `GET /api/doctor/prescriptions[/{id}]` | Assigned ACTIVE VERIFIED doctor | Paginated history or prescription details |
| `GET/POST /api/doctor/consultations/{id}/prescriptions` | Assigned ACTIVE VERIFIED doctor | Consultation history or create/return its single draft while scheduled/in progress |
| `PUT /api/doctor/prescriptions/{id}` | Assigned doctor, writable DRAFT only | Save validity, instructions, and 0–20 structured medicines while scheduled/in progress |
| `DELETE /api/doctor/prescriptions/{id}` | Assigned doctor, DRAFT only | Discard a draft and its items, including a legacy completed-consultation draft |
| `POST /api/doctor/prescriptions/{id}/issue` | Assigned doctor, DRAFT only | Issue only while the consultation is `IN_PROGRESS` |
| `POST /api/doctor/prescriptions/{id}/cancel` | Assigned doctor, ISSUED only | Cancel with a reason and revoke the QR token |
| `GET /api/patient/prescriptions[/{id}]` | Owning ACTIVE patient | Read issued/cancelled prescriptions without returning a QR secret |
| `POST /api/patient/prescriptions/{id}/qr` | Owning ACTIVE patient | Generate/rotate an eligible issued prescription QR on demand |

Issued prescriptions are immutable. A consultation cannot be completed while it has an unresolved draft. Prescribing stops after completion, while consultation chat deliberately remains writable. Cancelled prescription responses retain cancellation metadata but omit medicine items and general instructions.

QR payloads have the form `MEDISYNC:RX:<opaque-token>` and contain no identity or clinical data. The patient generates them on demand from a 256-bit `SecureRandom` token. PostgreSQL stores only the lowercase SHA-256 hash; the raw token is returned once and is neither persisted nor reconstructed by detail endpoints. Regeneration replaces the current hash and invalidates the previous QR. Phase 4 has no public or pharmacist verification endpoint.

Availability creation and normal availability APIs use the injectable server `Clock`: a window must start strictly in the future, fully expired windows and elapsed doctor slots are filtered without deleting history, patient slots must also satisfy the configured lead time, and booking rechecks that rule while holding the slot lock.

## Database migrations

Flyway runs migrations on application startup before Hibernate validates the schema. Hibernate uses `ddl-auto=validate`; it never creates or updates production tables. The migrations are incremental:

- `V1__create_app_users.sql` and `V2__create_role_profiles.sql`: Phase 1 identity and role profiles
- `V3__phase_2a_doctor_verification_foundation.sql`: Phase 2A professional reference data and doctor verification
- `V4__phase_2b_availability_and_appointments.sql`: Phase 2B availability and online consultation booking
- `V5__phase_3_online_consultations_and_chat.sql`: Phase 3 consultation sessions, persistent chat, and doctor-only clinical notes
- `V6__phase_4_digital_prescriptions_and_qr.sql`: Phase 4 prescription lifecycle, ordered items, and opaque QR tokens
- `V7__phase_4_security_and_availability_hardening.sql`: revokes legacy QR credentials, removes raw-token storage, and adds unique SHA-256 hash storage

V5 additively creates `consultation_sessions`, `consultation_messages`, and `consultation_clinical_notes`, including lifecycle, ownership, content, and uniqueness constraints. It safely creates one `SCHEDULED` session for each existing `CONFIRMED` appointment that does not already have one, without changing the appointment. The first administrator is created only through the documented trusted bootstrap process in `docs/admin-bootstrap.md`.

Do not run destructive Flyway repair/clean operations against the hosted project.

## Product roadmap

- Phase 1: authentication, roles, and security (complete)
- Phase 2A: reference data, professional profiles, and administrator verification (complete)
- Phase 2B: availability, doctor discovery, online consultation booking, symptom submission, and booking transitions (complete)
- Phase 3: online consultation session, secure doctor-patient chat, private clinical notes, and consultation status (complete)
- Phase 4: digital prescriptions, patient prescription view, and QR support (current)
- Phase 5: Pharmacist QR Scanning, Prescription Verification, Medicine Dispensing, and Dispensing History (future)

Remote monitoring and formal follow-up scheduling are outside the core roadmap.

## Test and package

```powershell
mvn test
mvn package
```

Unit and MVC security tests do not require the production database. Running the full application requires `DB_PASSWORD` and network access to hosted PostgreSQL and the Supabase JWKS endpoint.

Use `docs/phase-2b-manual-verification.md` for the Phase 2 booking regression checklist and `docs/phase-3-manual-verification.md` for consultation lifecycle, live chat, ownership, clinical-note privacy, and post-completion checks.
