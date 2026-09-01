# Participant Profiles

[Back to focused views](README.md)

<a href="../assets/participant-profile-schema.svg"><img src="../assets/participant-profile-schema.svg" alt="Participant profile relational schema"></a>

[High-resolution PNG fallback](../assets/participant-profile-schema.png)

`profile`, `startup`, and `investor` are separate account-owned tables; the
schema does not define a direct foreign key from `profile` to either participant
table.

| Rel | Parent | Cardinality | Child | Schema basis |
|---|---|---:|---|---|
| `R1` | `account` | `1 -> 0..1` | `profile` | `profile.account_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_profile_account`; `ON DELETE RESTRICT` |
| `R2` | `account` | `1 -> 0..1` | `startup` | `startup.account_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_startup_account`; `ON DELETE RESTRICT` |
| `R3` | `account` | `1 -> 0..1` | `investor` | `investor.account_id` is `FK`, `NOT NULL`, `UNIQUE`; `fk_investor_account`; `ON DELETE RESTRICT` |
| `R4` | `startup` | `1 -> 0..N` | `startup_legal_registration` | `startup_legal_registration.startup_id` is `FK`, `NOT NULL`; `fk_startup_registration`; `ON DELETE CASCADE` |
| `R5` | `startup` | `1 -> 0..N` | `startup_web_presence` | `startup_web_presence.startup_id` is `FK`, `NOT NULL`; `fk_startup_web_presence`; `ON DELETE CASCADE` |
| `R6` | `startup` | `1 -> 0..N` | `startup_classification` | `startup_classification.startup_id` is `FK`, `NOT NULL`; `fk_startup_classification`; `ON DELETE CASCADE`; `UNIQUE (startup_id, classification_type, classification_value)` |
| `R7` | `investor` | `1 -> 0..N` | `investor_web_presence` | `investor_web_presence.investor_id` is `FK`, `NOT NULL`; `fk_investor_web_presence`; `ON DELETE CASCADE` |
| `R8` | `investor` | `1 -> 0..N` | `investor_preference` | `investor_preference.investor_id` is `FK`, `NOT NULL`; `fk_investor_preference`; `ON DELETE CASCADE`; `UNIQUE (investor_id, preference_type, preference_value)` |
