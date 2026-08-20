# Phase 2A manual verification

Use the hosted development project and distinct test email addresses. Do not reset the database.

## Administrator

1. Complete the one-time process in `docs/admin-bootstrap.md`.
2. Sign in at `/login` and confirm routing to `/admin/dashboard`.
3. Create a hospital.
4. Create a department under that hospital.
5. Create a specialization.
6. Deactivate and reactivate each item once to verify reference filtering without deletion.

## Doctor approval

1. Sign in as an existing pending doctor.
2. Complete the professional profile using the administrator-created reference data.
3. Save the profile, then submit it for verification.
4. Confirm the doctor portal shows the submitted/pending state and locks the form.
5. Sign in as the administrator and open the pending-doctor review.
6. Confirm the name, contact details, registration number, qualifications, experience, hospital, department, specialization, bio, and submission time.
7. Approve the doctor.
8. Sign in again as the doctor or reload the profile.
9. Confirm the portal shows `Verified doctor` and the application account is active.

## Doctor rejection and resubmission

1. Submit a different pending doctor profile.
2. Reject it as the administrator with a meaningful reason.
3. Sign in as that doctor and confirm the reason appears.
4. Edit the professional profile and resubmit it.
5. Confirm the doctor returns to the administrator's pending queue and the previous rejection reason is cleared.

## Authorization regression

1. While signed in as a patient, doctor, and pharmacist, attempt to open `/admin/dashboard`; each user must be redirected to their own portal.
2. Call an `/api/admin/**` endpoint with each non-admin access token; each response must be `403 Forbidden`.
3. Confirm public onboarding still offers only Patient, Doctor, and Pharmacist.
4. Confirm existing patient and pharmacist dashboards still open normally.

