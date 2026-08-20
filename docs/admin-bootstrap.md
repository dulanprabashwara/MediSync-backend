# First administrator bootstrap

MediSync deliberately has no public administrator registration flow. Use this one-time process only for a trusted development administrator.

1. In the Supabase Dashboard, create a normal email/password Auth user for the trusted administrator and mark the email confirmed.
2. Copy the Auth user's UUID and email. Do not copy or store the password in SQL.
3. In the Supabase SQL Editor, replace every angle-bracket placeholder in the template below.
4. Review the values, run the statement once, and confirm that one `app_users` row was created.
5. Sign in through the normal MediSync `/login` page. The application will route the user to `/admin/dashboard`.

```sql
INSERT INTO app_users (
    id,
    auth_user_id,
    email,
    first_name,
    last_name,
    phone,
    role,
    status,
    created_at,
    updated_at
)
VALUES (
    gen_random_uuid(),
    '<AUTH_USER_UUID>'::uuid,
    '<ADMIN_EMAIL>',
    '<FIRST_NAME>',
    '<LAST_NAME>',
    NULL,
    'ADMIN',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
```

Before running it, verify that the Auth UUID belongs to the intended trusted person. Never expose this SQL as an HTTP endpoint, never add ADMIN to public onboarding, and never place real credentials in repository files.

To verify the bootstrap without exposing private data:

```sql
SELECT role, status
FROM app_users
WHERE auth_user_id = '<AUTH_USER_UUID>'::uuid;
```

The result must be exactly `ADMIN` and `ACTIVE`.
