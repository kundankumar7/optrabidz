# Identity and Access

[Back to focused views](README.md)

<a href="../assets/identity-access-schema.svg"><img src="../assets/identity-access-schema.svg" alt="Identity and access relational schema"></a>

[High-resolution PNG fallback](../assets/identity-access-schema.png)

This slice includes account-owned access records and the standalone
`login_attempt` security log. A login attempt stores the submitted email value
but has no database foreign key to `account` or `credential`; update and delete
triggers make these records immutable.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `account` | `1 -> 0..1` | `role` | `role.account_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_role_account`; `ON DELETE RESTRICT` |
| `R2` | `account` | `1 -> 0..1` | `credential` | `credential.account_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_credential_account`; `ON DELETE RESTRICT` |
| `R3` | `account` | `1 -> 0..N` | `session` | `session.account_id` is `FK`, `NOT NULL`; `fk_session_account`; `ON DELETE CASCADE` |
| `R4` | `account` | `1 -> 0..1` | `admin` | `admin.account_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_admin_account`; `ON DELETE RESTRICT` |
| `R5` | `account` | `0..1 -> 0..N` | `admin` | `admin.revoked_by_account_id` is nullable `FK`; `fk_admin_revoked_by_account`; `ON DELETE SET NULL` |
