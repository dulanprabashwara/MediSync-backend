# MediSync API

Spring Boot REST API for MediSync Phase 2A. It owns application user data, onboarding, account status, role authorization, healthcare reference data, doctor professional profiles, and administrator verification while Supabase Auth owns credentials and sessions.

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

## Database migrations

Flyway runs migrations on application startup before Hibernate validates the schema. Hibernate uses `ddl-auto=validate`; it never creates or updates production tables. Migrations create only Phase 1 tables:

- `app_users`
- `patient_profiles`
- `doctor_profiles`
- `pharmacist_profiles`

Phase 2A adds hospitals, departments, specializations, and additive professional-verification fields to `doctor_profiles`. The first administrator is created only through the documented trusted bootstrap process in `docs/admin-bootstrap.md`.

Do not run destructive Flyway repair/clean operations against the hosted project.

## Test and package

```powershell
mvn test
mvn package
```

Unit and MVC security tests do not require the production database. Running the full application requires `DB_PASSWORD` and network access to hosted PostgreSQL and the Supabase JWKS endpoint.
