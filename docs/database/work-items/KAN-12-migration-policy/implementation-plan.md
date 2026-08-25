# KAN-12 Database Migration Policy Implementation Plan

**Goal:** Publish one accurate operational guide for authoring and applying
OptraBidz Flyway migrations, then make it discoverable from the existing
database documentation and root README.

**Architecture:** `docs/database/migrations.md` will be the single source of
truth for migration policy. `docs/database/README.md` and `README.md` will
contain short entry points that link to it instead of duplicating its rules.
No Java, SQL, application configuration, or database state will change.

**Technology:** Markdown, Mermaid, Flyway, PostgreSQL 16, Hibernate validation,
Maven Wrapper, JUnit 5, Testcontainers, GitHub Actions with Temurin Java 21.

## Global Constraints

- Work only on `docs/KAN-12-migration-policy`.
- Do not modify `src/main/resources/db/migration/V1__baseline.sql`.
- Do not add a new migration or change Flyway/Hibernate configuration.
- Do not run SQL manually against a database.
- Keep the detailed policy in `docs/database/migrations.md`; link to it from
  other documents instead of copying it.
- Label destructive reset instructions as local-only and data-deleting.
- Preserve the distinction between disposable and populated databases.
- Keep task-execution and approval-process instructions out of the published
  database guide and README files.

---

### Task 1: Publish the Migration Policy

**Files:**

- Create: `docs/database/migrations.md`
- Reference: `docs/database/work-items/KAN-12-migration-policy/design.md`
- Must not modify: `src/main/resources/db/migration/V1__baseline.sql`

**Produces:** The canonical migration guide consumed by Task 2.

- [ ] **Step 1: Record the protected baseline before editing**

Run:

```powershell
git rev-parse develop:src/main/resources/db/migration/V1__baseline.sql
git hash-object src/main/resources/db/migration/V1__baseline.sql
```

Expected: both commands print
`8784c468aa169952a87e726303d03abae4376add`.

- [ ] **Step 2: Create the canonical guide**

Create `docs/database/migrations.md` with these sections and decisions:

1. `# Database Migration Guide`
2. `## Ownership and Startup Order`
   - Flyway creates and upgrades the schema from
     `src/main/resources/db/migration`.
   - Hibernate runs afterward with `ddl-auto=validate`.
   - A migration or mapping mismatch must stop startup.
3. `## Versioned Migration Rules`
   - Never edit, rename, delete, or reorder an applied migration.
   - Use the next unused version and a descriptive name, illustrated by
     `V2__add_account_phone_number.sql`.
   - Do not run a versioned file directly with `psql`.
   - Do not use environment-specific or user data as migration seed data.
4. `## Fresh PostgreSQL 16 Database`
   - Create an empty database, configure the datasource, and start the
     application.
   - Explain that Flyway applies all versions and records their checksums in
     `flyway_schema_history` before Hibernate validates mappings.
5. `## Resetting the Disposable Local Database`
   - State before the command that it permanently deletes all data in the
     named `optrabidz-postgres` local container.
   - Stop the application, run
     `docker rm -f optrabidz-postgres`, recreate the exact container using the
     root README command, and start the application.
   - State that this procedure must not be used for a database whose data must
     be preserved.
6. `## Populated Database Upgrade Gate`
   - Require verified backup/restore, schema and history capture, drift
     comparison, reconciliation, rehearsal on a restored copy, integrity
     checks, recovery steps, and separate release approval.
   - State that a legacy populated database without Flyway history needs a
     dedicated reconciliation task.
7. `## Expand, Migrate, Contract`
   - Explain the three release phases.
   - Use the safe replacement of `funding_listing.title` with
     `display_title` as the concrete example.
8. `## Failure and Recovery Rules`
   - Investigate checksum mismatches; do not routinely use `flyway repair`.
   - Do not globally enable `baseline-on-migrate`.
   - Prefer a new corrective migration after release.
   - Do not assume every PostgreSQL operation can be rolled back.
9. `## Author and Reviewer Checklist`
   - Include migration purpose, compatibility, data preservation/backfill,
     constraints/indexes, local integration verification, and release recovery.
10. `## Verification Commands`
    - Include the stale-guidance scans and Maven commands from Task 3.

Include the environment decision flow from the approved design as a Mermaid
diagram. Keep node labels about database state and required actions; do not add
implementation workflow, branch, or merge workflow nodes.

- [ ] **Step 3: Review the guide against current configuration**

Run:

```powershell
rg -n "spring\.flyway|spring\.jpa\.hibernate\.ddl-auto|spring\.jpa\.generate-ddl" src/main/resources/application.properties
rg -n "temurin|java-version|integration-tests" .github/workflows/backend-ci.yml
```

Expected configuration evidence:

- Flyway is enabled with validation on migrate.
- automatic baselining is false;
- Flyway clean is disabled;
- Hibernate uses `ddl-auto=validate` and generate-DDL is false;
- CI uses Temurin Java 21 and the `integration-tests` profile.

- [ ] **Step 4: Check the documentation diff**

Run:

```powershell
git diff --check
git diff -- docs/database/migrations.md
```

Expected: one new documentation file, no whitespace errors, no executable or
configuration changes.

- [ ] **Step 5: Commit the canonical guide**

```powershell
git add -- docs/database/migrations.md
git commit -m "docs: define database migration policy (KAN-12)"
```

---

### Task 2: Make the Policy Discoverable

**Files:**

- Modify: `README.md:182-188`
- Modify: `README.md:227-288`
- Modify: `docs/database/README.md:5-14`
- Modify: `docs/database/README.md:29-55`
- Consume: `docs/database/migrations.md`

**Produces:** Short, accurate entry points to the canonical guide.

- [ ] **Step 1: Update the root database-design index**

Add this row to the table under `## Database Design`:

```markdown
| [`docs/database/migrations.md`](docs/database/migrations.md) | Flyway migration authoring, upgrade, and recovery policy |
```

- [ ] **Step 2: Clarify fresh-database startup**

In `## Running Locally`, replace the sentence saying only that the schema
reference exists with text that says an empty PostgreSQL 16 database is enough:
Flyway applies the versioned migrations automatically and Hibernate validates
the result. Link operational details to `docs/database/migrations.md`.

Replace the existing three-line Flyway warning near line 285 with one concise
paragraph that says Flyway owns migrations, `V1__baseline.sql` must not be run
directly, and the complete authoring/reset/upgrade policy is in the migration
guide.

- [ ] **Step 3: Update the database documentation index**

In `docs/database/README.md`:

- add `migrations.md` to `## Start Here`;
- add it to the `## Files` table with the purpose
  `Migration authoring, environment upgrade, and recovery policy`;
- replace the short `## Migration Ownership` section with a link to the
  canonical guide and a one-sentence ownership summary.

- [ ] **Step 4: Validate links and avoid duplicated policy**

Run:

```powershell
rg -n "docs/database/migrations\.md|migrations\.md" README.md docs/database/README.md
rg -n "Applied migrations|baseline-on-migrate|flyway repair|Expand, Migrate, Contract" README.md docs/database/README.md docs/database/migrations.md
```

Expected:

- both index documents link to the guide;
- detailed rules occur in `docs/database/migrations.md`, not as duplicated
  policy sections in the indexes.

- [ ] **Step 5: Check and commit navigation changes**

```powershell
git diff --check
git diff -- README.md docs/database/README.md
git add -- README.md docs/database/README.md
git commit -m "docs: link database migration guide (KAN-12)"
```

---

### Task 3: Prove Documentation and Runtime Consistency

**Files:**

- Verify only: `README.md`
- Verify only: `docs/database/README.md`
- Verify only: `docs/database/migrations.md`
- Verify only: `src/main/resources/application.properties`
- Verify only: `src/main/resources/db/migration/V1__baseline.sql`
- Verify only: `.github/workflows/backend-ci.yml`

**Produces:** Review evidence for the KAN-12 pull request; no repository file is
created solely to store command output.

- [ ] **Step 1: Scan for obsolete initialization instructions**

Run:

```powershell
rg -n "optrabidz-schema\.sql|ddl-auto=update|manual schema initialization" README.md docs/database/README.md docs/database/er-diagram.md docs/database/er-diagram-source.md src
rg -n "spring\.sql\.init" README.md docs/database/README.md docs/database/er-diagram.md docs/database/er-diagram-source.md src/main
rg -n "spring\.sql\.init" src/test
```

Expected:

- the first two commands return no matches;
- the third command returns only the assertions in
  `DatabaseMigrationIT` that confirm Spring SQL initialization is absent.

- [ ] **Step 2: Prove V1 and configuration were not changed**

Run:

```powershell
git diff develop...HEAD -- src/main/resources/db/migration/V1__baseline.sql src/main/resources/application.properties
git rev-parse develop:src/main/resources/db/migration/V1__baseline.sql
git hash-object src/main/resources/db/migration/V1__baseline.sql
```

Expected:

- the diff is empty;
- both hashes equal `8784c468aa169952a87e726303d03abae4376add`.

- [ ] **Step 3: Run clean unit verification**

```powershell
.\mvnw.cmd -B clean test
```

Expected: `BUILD SUCCESS`; 64 tests, 0 failures, 0 errors.

- [ ] **Step 4: Run clean PostgreSQL integration verification**

Ensure Docker Engine is running, then run:

```powershell
.\mvnw.cmd -B clean verify -Pintegration-tests
```

Expected: `BUILD SUCCESS`; 64 unit tests and 64 integration tests, with 0
failures and 0 errors.

- [ ] **Step 5: Review final branch scope**

```powershell
git status --short
git diff --check develop...HEAD
git diff --name-status develop...HEAD
git log --oneline develop..HEAD
```

Expected changed files:

```text
A docs/design/KAN-12-migration-policy-design.md
A docs/design/KAN-12-migration-policy-implementation-plan.md
A docs/database/migrations.md
M README.md
M docs/database/README.md
```

Expected: a clean working tree and commits referencing KAN-12. Create a pull
request from `docs/KAN-12-migration-policy` into `develop` for review.
