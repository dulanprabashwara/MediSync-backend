# MediSync API

Spring Boot REST API for MediSync through Phase 2B. It owns application users, role authorization, professional verification, date-based doctor availability, doctor discovery, appointment requests, and patient-submitted symptoms while Supabase Auth owns credentials and sessions.

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

## Phase 2B API

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

## Database migrations

Flyway runs migrations on application startup before Hibernate validates the schema. Hibernate uses `ddl-auto=validate`; it never creates or updates production tables. Migrations create only Phase 1 tables:

- `app_users`
- `patient_profiles`
- `doctor_profiles`
- `pharmacist_profiles`

Phase 2A adds professional reference data and doctor verification. Phase 2B migration `V4__phase_2b_availability_and_appointments.sql` additively creates `doctor_availability_windows`, `appointment_slots`, `appointments`, and `appointment_symptoms`, including status checks, foreign keys, scheduling indexes, duplicate-slot protection, and active-appointment uniqueness. The first administrator is created only through the documented trusted bootstrap process in `docs/admin-bootstrap.md`.

Do not run destructive Flyway repair/clean operations against the hosted project.

## Test and package

```powershell
mvn test
mvn package
```

Unit and MVC security tests do not require the production database. Running the full application requires `DB_PASSWORD` and network access to hosted PostgreSQL and the Supabase JWKS endpoint.

Use `docs/phase-2b-manual-verification.md` for the complete availability, booking, transition, concurrency, and authorization checklist.
