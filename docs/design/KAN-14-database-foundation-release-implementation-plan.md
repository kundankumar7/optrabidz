# KAN-14 Database Foundation Release Implementation Plan

**Goal:** Promote the verified database-foundation milestone from `develop`
to protected `main`, while preserving the old `main` revision and producing
complete release evidence.

**Architecture:** The release uses two pull requests and two annotated tags.
The first pull request publishes the KAN-14 release documents to `develop`.
After the old `main` is archived and protected, a release pull request merges
the verified `develop` revision into `main`. Post-merge verification must pass
before the milestone tag is created.

**Technology:** Git, GitHub CLI, GitHub REST API, GitHub Actions, PowerShell,
Maven Wrapper, Temurin Java 21, Docker Engine, Testcontainers, PostgreSQL 16,
Flyway, Hibernate validation, JUnit 5.

## Global Constraints

- Repository: `kundankumar7/optrabidz`.
- Release design:
  `docs/design/KAN-14-database-foundation-release-design.md`.
- Release documentation branch:
  `release/KAN-14-database-foundation`.
- Old `main` revision:
  `10b0d93791ae7dd90a5e3d1aca90b61b1aee3945`.
- Pre-documentation `develop` revision:
  `fee3e0710b598d1d6bb6b01c53ce34f59c66035a`.
- Flyway V1 blob ID:
  `8784c468aa169952a87e726303d03abae4376add`.
- Archive tag: `archive-pre-database-foundation`.
- Milestone tag: `milestone-database-foundation-v1`.
- Required checks: `Unit Tests` and `PostgreSQL Integration Tests`.
- GitHub CLI executable:
  `C:\Program Files\GitHub CLI\gh.exe`.
- Never commit or push directly to `main`.
- Never use `--admin`, force push, rebase, reset, or history rewriting to
  bypass a release gate.
- Do not delete or rewrite `develop`.
- Do not change application code, SQL migrations, configuration, CI, or
  database content during KAN-14.
- A failed verification stops the release. Fixes use a separate reviewed
  branch and the affected verification starts again.
- An annotated tag is verified locally before it is pushed. A published tag
  is never moved or reused for another commit.
- The release pull request is merged only after release review is recorded.
- The milestone tag is created only after post-merge local and GitHub
  verification succeeds on the exact `main` revision.

## Evidence Model

Command output is reviewed during execution and summarized in KAN-14 and the
release pull request. Test logs and temporary API responses are not committed.
The repository changes produced by the documentation pull request are exactly:

```text
docs/design/KAN-14-database-foundation-release-design.md
docs/design/KAN-14-database-foundation-release-implementation-plan.md
```

All commands run from:

```text
C:\Users\kumar\IdeaProjects\optrabidz
```

At the start of every GitHub task, define:

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
```

---

### Task 1: Publish the Release Documents to Develop

**Files:**

- Existing:
  `docs/design/KAN-14-database-foundation-release-design.md`
- Existing:
  `docs/design/KAN-14-database-foundation-release-implementation-plan.md`
- Must not modify: application, migration, configuration, CI, and test files

**Interfaces:**

- Consumes: approved KAN-14 design and plan commits on
  `release/KAN-14-database-foundation`.
- Produces: a reviewed documentation-only pull request merged into `develop`
  and `$releaseHead`, the exact `origin/develop` revision evaluated by Tasks
  2 through 7.

- [ ] **Step 1: Confirm the documentation branch scope**

```powershell
git status --short --branch
git fetch --prune origin
git diff --check origin/develop...HEAD
git diff --name-status origin/develop...HEAD
git log --oneline origin/develop..HEAD
```

Expected:

- the current branch is `release/KAN-14-database-foundation`;
- the working tree is clean;
- the diff contains only the two KAN-14 Markdown files listed above;
- the design commit is
  `bcba279 docs: define database foundation release design (KAN-14)`;
- the plan commit references KAN-14;
- no whitespace error is reported.

- [ ] **Step 2: Confirm branch CI passed on the exact head**

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
$documentationHead = git rev-parse HEAD
$run = & $ghCli run list --repo $repoSlug --workflow backend-ci.yml `
  --branch release/KAN-14-database-foundation --event push --limit 1 `
  --json databaseId,headSha,status,conclusion,url | ConvertFrom-Json
$run
if ($run.headSha -ne $documentationHead) { throw 'CI run does not match the branch head.' }
& $ghCli run watch $run.databaseId --repo $repoSlug --exit-status
& $ghCli run view $run.databaseId --repo $repoSlug `
  --json headSha,status,conclusion,jobs,url
```

Expected: the run targets `$documentationHead`; both `Unit Tests` and
`PostgreSQL Integration Tests` finish with `success`.

- [ ] **Step 3: Open the documentation pull request into develop**

```powershell
$documentationBody = @'
## Outcome

Publish the approved KAN-14 release design and implementation plan before any
repository protection, tagging, or main-branch promotion is performed.

## Scope

- release architecture and legacy checkpoint decision;
- main branch protection requirements;
- exact release, verification, recovery, and tagging procedure.

## Non-goals

- no application or database change;
- no branch protection change;
- no tag creation;
- no merge to main.

## Verification

- documentation-only branch diff;
- Unit Tests passed;
- PostgreSQL Integration Tests passed.
'@

$documentationPrUrl = $documentationBody | & $ghCli pr create `
  --repo $repoSlug `
  --base develop `
  --head release/KAN-14-database-foundation `
  --title 'KAN-14: Define database foundation release procedure' `
  --body-file -
$documentationPrUrl
```

Expected: GitHub returns a new pull-request URL with base `develop` and head
`release/KAN-14-database-foundation`.

- [ ] **Step 4: Validate the pull request and pause for review**

```powershell
$documentationPr = & $ghCli pr view `
  release/KAN-14-database-foundation --repo $repoSlug `
  --json number,url,state,baseRefName,headRefName,headRefOid,files,statusCheckRollup |
  ConvertFrom-Json
$documentationPr
& $ghCli pr diff $documentationPr.number --repo $repoSlug --name-only
& $ghCli pr checks $documentationPr.number --repo $repoSlug --watch --fail-fast
```

Expected:

- state is `OPEN`;
- base is `develop` and head is the KAN-14 release branch;
- the two required checks pass;
- only the two KAN-14 documents appear.

Stop here for review. Do not merge during the same checkpoint that creates the
pull request.

- [ ] **Step 5: Merge the documentation pull request after review**

After release-document review is recorded, re-read the PR head and checks:

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
$documentationPr = & $ghCli pr view `
  release/KAN-14-database-foundation --repo $repoSlug `
  --json number,state,headRefOid,mergeable,mergeStateStatus | ConvertFrom-Json
$documentationPr
$documentationChecks = @(& $ghCli pr checks $documentationPr.number `
  --repo $repoSlug --json name,state,bucket,workflow | ConvertFrom-Json)
foreach ($checkName in @('Unit Tests', 'PostgreSQL Integration Tests')) {
  $matchingCheck = @($documentationChecks | Where-Object { $_.name -eq $checkName })
  if ($matchingCheck.Count -ne 1 -or $matchingCheck[0].bucket -ne 'pass') {
    throw "$checkName is not passing."
  }
}
```

Expected: state `OPEN`, mergeability `MERGEABLE`, and both CI checks passing.
Then merge without deleting the branch:

```powershell
& $ghCli pr merge $documentationPr.number --repo $repoSlug `
  --merge `
  --match-head-commit $documentationPr.headRefOid `
  --subject 'Merge KAN-14 release documentation into develop'
git fetch --prune origin
$releaseHead = git rev-parse origin/develop
$releaseHead
```

Expected: the PR is `MERGED`, and `$releaseHead` is a new `origin/develop`
revision containing both KAN-14 documents. Record this exact SHA in KAN-14.

---

### Task 2: Freeze the Release Inputs and Archive Old Main

**Files:** Verify only; no file changes.

**Interfaces:**

- Consumes: `$releaseHead` from Task 1, old `main` SHA, and approved V1 blob
  ID.
- Produces: verified release inputs and the pushed annotated tag
  `archive-pre-database-foundation` at `$oldMain`.

- [ ] **Step 1: Verify authentication and immutable inputs**

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
$oldMain = '10b0d93791ae7dd90a5e3d1aca90b61b1aee3945'
$archiveTag = 'archive-pre-database-foundation'
$v1Blob = '8784c468aa169952a87e726303d03abae4376add'

& $ghCli auth status
git status --short --branch
git fetch --prune origin
if ((git rev-parse origin/main) -ne $oldMain) { throw 'origin/main moved; stop and reassess KAN-14.' }
$releaseHead = git rev-parse origin/develop
git merge-base --is-ancestor origin/main origin/develop
if ($LASTEXITCODE -ne 0) { throw 'main is not an ancestor of develop.' }
git rev-list --left-right --count origin/main...origin/develop
```

Expected: authentication uses `kundankumar7`; the working tree is clean;
`origin/main` equals `$oldMain`; the comparison reports zero commits unique to
`main` and one or more commits unique to `develop`.

- [ ] **Step 2: Confirm repository state has no competing work**

```powershell
& $ghCli repo view $repoSlug --json nameWithOwner,defaultBranchRef,isPrivate,url
& $ghCli pr list --repo $repoSlug --state open `
  --json number,title,headRefName,baseRefName,url
git tag --list
git log --oneline --decorate origin/main..origin/develop
git diff --name-status origin/main...origin/develop
git diff --check origin/main...origin/develop
```

Expected:

- the default branch is `main`;
- no open pull request depends on `develop`;
- neither KAN-14 tag exists;
- the commit and file lists contain the reviewed KAN-7, KAN-9, KAN-10,
  KAN-11, KAN-13, KAN-12, and KAN-14 work;
- no whitespace error appears.

Any unrelated commit, file, tag, or pull request stops the release for review.

- [ ] **Step 3: Verify Flyway V1 before creating the archive tag**

```powershell
$developV1 = git rev-parse "origin/develop:src/main/resources/db/migration/V1__baseline.sql"
$mainV1Path = git ls-tree -r --name-only origin/main -- `
  src/main/resources/db/migration/V1__baseline.sql
$developV1
$mainV1Path
if ($developV1 -ne $v1Blob) { throw 'Flyway V1 does not match the approved blob.' }
```

Expected: `develop` resolves V1 to `$v1Blob`. The old `main` does not contain
that migration path because Flyway ownership was introduced by KAN-7.

- [ ] **Step 4: Prove the archive tag name is unused**

```powershell
git show-ref --verify --quiet "refs/tags/$archiveTag"
if ($LASTEXITCODE -eq 0) { throw 'Archive tag already exists locally.' }
git ls-remote --exit-code --tags origin "refs/tags/$archiveTag"
if ($LASTEXITCODE -eq 0) { throw 'Archive tag already exists on origin.' }
```

Expected: both lookups report the tag as absent. The nonzero lookup status is
expected here and must not be treated as a release failure.

- [ ] **Step 5: Create, inspect, and push the annotated archive tag**

```powershell
git tag -a $archiveTag $oldMain `
  -m 'Archive main before database foundation milestone'
if ((git cat-file -t $archiveTag) -ne 'tag') { throw 'Archive reference is not an annotated tag.' }
if ((git rev-list -n 1 $archiveTag) -ne $oldMain) { throw 'Archive tag targets the wrong commit.' }
git show --no-patch --decorate $archiveTag
git push origin "refs/tags/$archiveTag"
git ls-remote --tags origin "refs/tags/$archiveTag" "refs/tags/$archiveTag^{}"
```

Expected: the local tag object is annotated; its peeled commit and the remote
`^{}` entry resolve to `$oldMain`. Record the remote evidence in KAN-14.

---

### Task 3: Protect Main Before Opening the Release Pull Request

**Files:** Verify only; no repository file changes.

**Interfaces:**

- Consumes: exact CI job names from `.github/workflows/backend-ci.yml` and a
  recent successful GitHub Actions run.
- Produces: effective GitHub protection requiring pull requests, strict CI,
  resolved conversations, admin enforcement, and no force push or deletion.

- [ ] **Step 1: Confirm the two check names from a recent successful run**

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
$recentRun = & $ghCli run list --repo $repoSlug --workflow backend-ci.yml `
  --limit 1 --json databaseId,headSha,status,conclusion,url | ConvertFrom-Json
& $ghCli run view $recentRun.databaseId --repo $repoSlug `
  --json headSha,status,conclusion,jobs,url
```

Expected: the successful jobs are named exactly `Unit Tests` and
`PostgreSQL Integration Tests`. Stop if either name differs.

- [ ] **Step 2: Apply the main branch protection rule**

```powershell
$protectionBody = @'
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "Unit Tests",
      "PostgreSQL Integration Tests"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": false,
    "require_code_owner_reviews": false,
    "required_approving_review_count": 0,
    "require_last_push_approval": false
  },
  "restrictions": null,
  "required_linear_history": false,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "block_creations": false,
  "required_conversation_resolution": true,
  "lock_branch": false,
  "allow_fork_syncing": true
}
'@

$protectionBody | & $ghCli api --method PUT `
  -H 'Accept: application/vnd.github+json' `
  -H 'X-GitHub-Api-Version: 2026-03-10' `
  "repos/$repoSlug/branches/main/protection" --input -
```

Expected: HTTP 200. Zero required approving reviews avoids locking out the
single pull-request author; pull-request use is still mandatory.

- [ ] **Step 3: Read back and assert the effective protection**

```powershell
$protection = & $ghCli api `
  -H 'Accept: application/vnd.github+json' `
  -H 'X-GitHub-Api-Version: 2026-03-10' `
  "repos/$repoSlug/branches/main/protection" | ConvertFrom-Json

$contexts = @($protection.required_status_checks.contexts)
if (-not $protection.required_status_checks.strict) { throw 'Strict checks are disabled.' }
if ('Unit Tests' -notin $contexts) { throw 'Unit Tests is not required.' }
if ('PostgreSQL Integration Tests' -notin $contexts) { throw 'Integration tests are not required.' }
if (-not $protection.enforce_admins.enabled) { throw 'Admin enforcement is disabled.' }
if ($protection.required_pull_request_reviews.required_approving_review_count -ne 0) { throw 'Unexpected review count.' }
if (-not $protection.required_conversation_resolution.enabled) { throw 'Conversation resolution is disabled.' }
if ($protection.allow_force_pushes.enabled) { throw 'Force pushes are allowed.' }
if ($protection.allow_deletions.enabled) { throw 'Branch deletion is allowed.' }
if ($protection.required_linear_history.enabled) { throw 'Linear history would block the planned merge commit.' }
if ($protection.lock_branch.enabled) { throw 'main is locked rather than protected for PR merges.' }
$protection | ConvertTo-Json -Depth 8
```

Expected: every assertion passes. Add the summarized settings to KAN-14.

---

### Task 4: Verify the Exact Develop Revision

**Files:** Verify only; no repository file changes.

**Interfaces:**

- Consumes: `$releaseHead`, `$oldMain`, `$v1Blob`, the archive tag from Task 2,
  and protection from Task 3.
- Produces: local and GitHub evidence that the exact release head passes all
  tests and contains the intended milestone only.

- [ ] **Step 1: Check out the exact remote develop revision cleanly**

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
$oldMain = '10b0d93791ae7dd90a5e3d1aca90b61b1aee3945'
$v1Blob = '8784c468aa169952a87e726303d03abae4376add'
git switch develop
git pull --ff-only origin develop
git status --short --branch
$releaseHead = git rev-parse HEAD
if ($releaseHead -ne (git rev-parse origin/develop)) { throw 'Local develop does not match origin/develop.' }
if ((git rev-parse origin/main) -ne $oldMain) { throw 'main moved before the release PR.' }
$releaseHead
```

Expected: clean `develop`, equal to `origin/develop`; `origin/main` still
equals `$oldMain`. Record `$releaseHead`.

- [ ] **Step 2: Verify schema ownership and protected V1 content**

```powershell
if ((git hash-object src/main/resources/db/migration/V1__baseline.sql) -ne $v1Blob) {
  throw 'Working-tree V1 differs from the approved blob.'
}
rg -n "spring\.flyway|spring\.jpa\.hibernate\.ddl-auto|spring\.jpa\.generate-ddl" `
  src/main/resources/application.properties
rg -n "temurin|java-version|integration-tests" .github/workflows/backend-ci.yml
```

Expected:

- V1 equals `$v1Blob`;
- Flyway is enabled with validation, automatic baselining false, and clean
  disabled;
- Hibernate uses `ddl-auto=validate` and does not generate DDL;
- CI uses Temurin Java 21 and the `integration-tests` profile.

- [ ] **Step 3: Run clean unit verification**

```powershell
.\mvnw.cmd -B clean test
```

Expected: `BUILD SUCCESS`; 64 unit tests, zero failures and zero errors.

- [ ] **Step 4: Run clean PostgreSQL integration verification**

Confirm Docker Engine is running, then execute:

```powershell
.\mvnw.cmd -B clean verify -Pintegration-tests
```

Expected: `BUILD SUCCESS`; 64 unit tests and 64 integration tests, with zero
failures and zero errors. Testcontainers uses PostgreSQL 16.

- [ ] **Step 5: Recheck repository and release scope after tests**

```powershell
git status --short --branch
git diff --check origin/main...HEAD
git diff --name-status origin/main...HEAD
git log --oneline --decorate origin/main..HEAD
git rev-list --left-right --count origin/main...HEAD
```

Expected: the working tree remains clean; zero commits are unique to `main`;
the `develop` side contains only the reviewed database-foundation and release
documentation history.

- [ ] **Step 6: Verify GitHub CI for the exact release head**

```powershell
$developRun = & $ghCli run list --repo $repoSlug --workflow backend-ci.yml `
  --branch develop --event push --limit 1 `
  --json databaseId,headSha,status,conclusion,url | ConvertFrom-Json
if ($developRun.headSha -ne $releaseHead) { throw 'Latest develop CI does not target the release head.' }
& $ghCli run watch $developRun.databaseId --repo $repoSlug --exit-status
& $ghCli run view $developRun.databaseId --repo $repoSlug `
  --json headSha,status,conclusion,jobs,url
```

Expected: both required jobs pass for `$releaseHead`.

---

### Task 5: Open and Review the Develop-to-Main Release Pull Request

**Files:** Verify only; no repository file changes.

**Interfaces:**

- Consumes: unchanged `$oldMain`, verified `$releaseHead`, `$v1Blob`, archive
  tag, branch protection, and Task 4 test evidence.
- Produces: one exact develop-to-main release pull request with passing
  required checks, held open for release review.

- [ ] **Step 1: Reconfirm the base, head, archive, and protection immediately before PR creation**

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
$oldMain = '10b0d93791ae7dd90a5e3d1aca90b61b1aee3945'
$archiveTag = 'archive-pre-database-foundation'
$v1Blob = '8784c468aa169952a87e726303d03abae4376add'
git fetch --prune origin
$releaseHead = git rev-parse origin/develop
if ((git rev-parse origin/main) -ne $oldMain) { throw 'main moved; stop before creating the release PR.' }
if ((git rev-parse origin/develop) -ne $releaseHead) { throw 'develop moved; repeat Task 4.' }
if ((git rev-list -n 1 $archiveTag) -ne $oldMain) { throw 'Archive tag mismatch.' }
git ls-remote --tags origin "refs/tags/$archiveTag^{}"
& $ghCli api "repos/$repoSlug/branches/main/protection" `
  --jq '{strict:.required_status_checks.strict,contexts:.required_status_checks.contexts,enforce_admins:.enforce_admins.enabled,conversation_resolution:.required_conversation_resolution.enabled,force_pushes:.allow_force_pushes.enabled,deletions:.allow_deletions.enabled}'
```

Expected: the base and head are unchanged, the remote peeled archive tag is
`$oldMain`, and protection matches Task 3.

- [ ] **Step 2: Create the release pull request**

```powershell
$releaseBody = @"
## Outcome

Promote the verified database foundation from develop to main.

## Exact revisions

- Base main: $oldMain
- Release develop: $releaseHead
- Archive tag: archive-pre-database-foundation
- Flyway V1 blob: $v1Blob

## Included work

- KAN-7: establish the Flyway V1 baseline;
- KAN-9: run Flyway before Hibernate validation;
- KAN-10: make integration tests use the Flyway-managed schema;
- KAN-11: run PostgreSQL integration tests in CI;
- KAN-13: isolate outbox retry integration-test state;
- KAN-12: document migration ownership and safe upgrades;
- KAN-14: define and execute this controlled promotion.

## Verification

- 64 unit tests passed locally;
- 64 unit and 64 PostgreSQL integration tests passed locally;
- Unit Tests passed in GitHub Actions;
- PostgreSQL Integration Tests passed in GitHub Actions;
- V1__baseline.sql matches the approved blob;
- main branch protection requires this PR and both checks.

## Database compatibility

This release establishes Flyway ownership for fresh PostgreSQL 16 databases.
It does not migrate or preserve a populated production database. Current
development data is disposable, and V1 remains unchanged from its reviewed
baseline.

## Recovery

The archive tag preserves the old source revision. Any source rollback uses a
reviewed revert or corrective PR; main and published tags are never moved.
The archive tag is not a database backup.

## Non-goals

- no production deployment;
- no new schema migration;
- no claim that the complete application is production-ready;
- no deletion or rewrite of develop.
"@

$releasePrUrl = $releaseBody | & $ghCli pr create `
  --repo $repoSlug `
  --base main `
  --head develop `
  --title 'KAN-14: Promote verified database foundation to main' `
  --body-file -
$releasePrUrl
```

Expected: GitHub returns the release pull-request URL.

- [ ] **Step 3: Validate the exact pull-request range**

```powershell
$releasePr = & $ghCli pr view $releasePrUrl --repo $repoSlug `
  --json number,url,state,baseRefName,baseRefOid,headRefName,headRefOid,mergeable,mergeStateStatus,commits,files |
  ConvertFrom-Json
$releasePr
if ($releasePr.baseRefName -ne 'main') { throw 'Release PR base is not main.' }
if ($releasePr.baseRefOid -ne $oldMain) { throw 'Release PR base SHA changed.' }
if ($releasePr.headRefName -ne 'develop') { throw 'Release PR head is not develop.' }
if ($releasePr.headRefOid -ne $releaseHead) { throw 'Release PR head SHA changed.' }
& $ghCli pr diff $releasePr.number --repo $repoSlug --name-only
```

Expected: base `$oldMain`, head `$releaseHead`, and the same reviewed files as
Task 4.

- [ ] **Step 4: Wait for required checks and pause for release review**

```powershell
& $ghCli pr checks $releasePr.number --repo $repoSlug --watch --fail-fast
& $ghCli pr checks $releasePr.number --repo $repoSlug --required `
  --json name,state,bucket,workflow,link
```

Expected: `Unit Tests` and `PostgreSQL Integration Tests` are in the `pass`
bucket.

Stop here. Review the complete pull request and record the release decision.
Do not merge during the PR-creation checkpoint.

---

### Task 6: Merge the Reviewed Release and Verify Exact Main

**Files:** Verify only; no repository file changes.

**Interfaces:**

- Consumes: the reviewed release PR, unchanged `$oldMain` and `$releaseHead`,
  and passing required checks.
- Produces: `$finalMain`, a protected two-parent merge commit, followed by
  clean local and GitHub verification for that exact commit.

- [ ] **Step 1: Revalidate the reviewed PR immediately before merge**

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
$oldMain = '10b0d93791ae7dd90a5e3d1aca90b61b1aee3945'
$v1Blob = '8784c468aa169952a87e726303d03abae4376add'
git fetch --prune origin
$releaseHead = git rev-parse origin/develop
$releaseCandidates = @(& $ghCli pr list --repo $repoSlug --state open `
  --base main --head develop `
  --json number,state,baseRefName,baseRefOid,headRefName,headRefOid,mergeable,mergeStateStatus,url |
  ConvertFrom-Json)
if ($releaseCandidates.Count -ne 1) { throw 'Expected exactly one open develop-to-main release PR.' }
$releasePr = $releaseCandidates[0]
$releasePr = & $ghCli pr view $releasePr.number --repo $repoSlug `
  --json number,state,baseRefName,baseRefOid,headRefName,headRefOid,mergeable,mergeStateStatus,url |
  ConvertFrom-Json
$releasePr
if ($releasePr.state -ne 'OPEN') { throw 'Release PR is not open.' }
if ($releasePr.baseRefOid -ne $oldMain) { throw 'Release base changed after review.' }
if ($releasePr.headRefOid -ne $releaseHead) { throw 'Release head changed after review.' }
& $ghCli pr checks $releasePr.number --repo $repoSlug --required
```

Expected: the reviewed base and head are unchanged, the PR is mergeable, and
both required checks pass. If any value changed, return to Task 4 and repeat
review.

- [ ] **Step 2: Merge with GitHub's merge-commit strategy**

```powershell
& $ghCli pr merge $releasePr.number --repo $repoSlug `
  --merge `
  --match-head-commit $releaseHead `
  --subject 'Promote database foundation to main (KAN-14)' `
  --body 'Merge the reviewed develop milestone after required verification.'
```

Expected: GitHub merges without `--admin`, squash, rebase, force push, or
branch deletion.

- [ ] **Step 3: Prove the final main commit has the expected parents**

```powershell
git fetch --prune origin
$finalMain = git rev-parse origin/main
$parentLine = git rev-list --parents -n 1 $finalMain
$parents = $parentLine -split ' '
$parentLine
if ($parents.Count -ne 3) { throw 'Final main is not a two-parent merge commit.' }
if ($parents[1] -ne $oldMain) { throw 'First parent is not the old main revision.' }
if ($parents[2] -ne $releaseHead) { throw 'Second parent is not the reviewed develop revision.' }
git merge-base --is-ancestor $releaseHead origin/main
if ($LASTEXITCODE -ne 0) { throw 'Reviewed develop is not contained in main.' }
```

Expected: `$finalMain` is a new two-parent merge commit with old `main` first
and reviewed `develop` second.

- [ ] **Step 4: Confirm GitHub recorded the same merge commit**

```powershell
$mergedPr = & $ghCli pr view $releasePr.number --repo $repoSlug `
  --json state,mergedAt,mergeCommit,url | ConvertFrom-Json
$mergedPr
if ($mergedPr.state -ne 'MERGED') { throw 'GitHub does not report the release PR as merged.' }
if ($mergedPr.mergeCommit.oid -ne $finalMain) { throw 'GitHub merge commit differs from origin/main.' }
```

Expected: state `MERGED`; GitHub merge commit equals `$finalMain`.

- [ ] **Step 5: Run post-merge local verification on exact main**

```powershell
git switch main
git pull --ff-only origin main
if ((git rev-parse HEAD) -ne $finalMain) { throw 'Local main does not match the merged revision.' }
git status --short --branch
if ((git hash-object src/main/resources/db/migration/V1__baseline.sql) -ne $v1Blob) {
  throw 'V1 changed on final main.'
}
.\mvnw.cmd -B clean test
.\mvnw.cmd -B clean verify -Pintegration-tests
git status --short --branch
```

Expected: clean `main`; 64 unit tests pass; 64 unit and 64 integration tests
pass; zero failures and errors; V1 remains `$v1Blob`.

- [ ] **Step 6: Verify GitHub Actions on exact final main**

```powershell
$mainRun = & $ghCli run list --repo $repoSlug --workflow backend-ci.yml `
  --branch main --event push --limit 1 `
  --json databaseId,headSha,status,conclusion,url | ConvertFrom-Json
if ($mainRun.headSha -ne $finalMain) { throw 'Latest main CI does not target final main.' }
& $ghCli run watch $mainRun.databaseId --repo $repoSlug --exit-status
& $ghCli run view $mainRun.databaseId --repo $repoSlug `
  --json headSha,status,conclusion,jobs,url
```

Expected: both required jobs pass for `$finalMain`.

If either local or GitHub verification fails, stop. Do not create the milestone
tag. Open a corrective or revert pull request against protected `main`.

---

### Task 7: Mark the Milestone and Close the Release

**Files:** Verify only; no repository file changes.

**Interfaces:**

- Consumes: verified `$finalMain`, `$releaseHead`, `$oldMain`, branch
  protection, archive tag, and post-merge test evidence.
- Produces: a pushed annotated milestone tag and final evidence that `main` is
  protected, default, verified, and ready to be the source for future work.

- [ ] **Step 1: Prove the milestone tag name is unused**

```powershell
$ghCli = 'C:\Program Files\GitHub CLI\gh.exe'
$repoSlug = 'kundankumar7/optrabidz'
$oldMain = '10b0d93791ae7dd90a5e3d1aca90b61b1aee3945'
$archiveTag = 'archive-pre-database-foundation'
$v1Blob = '8784c468aa169952a87e726303d03abae4376add'
$milestoneTag = 'milestone-database-foundation-v1'
git fetch --prune origin
$releaseHead = git rev-parse origin/develop
$finalMain = git rev-parse origin/main
git show-ref --verify --quiet "refs/tags/$milestoneTag"
if ($LASTEXITCODE -eq 0) { throw 'Milestone tag already exists locally.' }
git ls-remote --exit-code --tags origin "refs/tags/$milestoneTag"
if ($LASTEXITCODE -eq 0) { throw 'Milestone tag already exists on origin.' }
```

Expected: the tag is absent locally and remotely.

- [ ] **Step 2: Create, inspect, and push the annotated milestone tag**

```powershell
git tag -a $milestoneTag $finalMain `
  -m 'Verified database foundation milestone v1'
if ((git cat-file -t $milestoneTag) -ne 'tag') { throw 'Milestone reference is not annotated.' }
if ((git rev-list -n 1 $milestoneTag) -ne $finalMain) { throw 'Milestone tag targets the wrong commit.' }
git show --no-patch --decorate $milestoneTag
git push origin "refs/tags/$milestoneTag"
git ls-remote --tags origin "refs/tags/$milestoneTag" "refs/tags/$milestoneTag^{}"
```

Expected: the peeled local and remote milestone tag resolves to `$finalMain`.

- [ ] **Step 3: Run the final repository audit**

```powershell
$repo = & $ghCli repo view $repoSlug `
  --json nameWithOwner,defaultBranchRef,isPrivate,url | ConvertFrom-Json
if ($repo.defaultBranchRef.name -ne 'main') { throw 'main is not the default branch.' }

$openDevelopPrs = @(& $ghCli pr list --repo $repoSlug --state open `
  --json number,title,headRefName,baseRefName,url | ConvertFrom-Json | `
  Where-Object { $_.headRefName -eq 'develop' -or $_.baseRefName -eq 'develop' })
if ($openDevelopPrs.Count -ne 0) { throw 'An open pull request still depends on develop.' }

if ((git rev-parse origin/develop) -ne $releaseHead) { throw 'develop changed during the release.' }
if ((git rev-parse origin/main) -ne $finalMain) { throw 'main changed after verification.' }
if ((git rev-list -n 1 $archiveTag) -ne $oldMain) { throw 'Archive checkpoint changed.' }
if ((git rev-list -n 1 $milestoneTag) -ne $finalMain) { throw 'Milestone checkpoint changed.' }

& $ghCli api "repos/$repoSlug/branches/main/protection" `
  --jq '{strict:.required_status_checks.strict,contexts:.required_status_checks.contexts,enforce_admins:.enforce_admins.enabled,conversation_resolution:.required_conversation_resolution.enabled,force_pushes:.allow_force_pushes.enabled,deletions:.allow_deletions.enabled}'
git status --short --branch
```

Expected:

- `main` remains default and protected;
- no open PR depends on `develop`;
- `develop` still equals `$releaseHead` and was not deleted or rewritten;
- archive and milestone tags still resolve to their intended commits;
- local `main` is clean and equals `$finalMain`.

- [ ] **Step 4: Record the KAN-14 completion evidence**

Generate the final Jira comment from the recorded values:

```powershell
$releasePrUrl = (& $ghCli pr list --repo $repoSlug --state merged `
  --base main --head develop --limit 1 --json url | ConvertFrom-Json).url
$completionComment = @"
Release PR: $releasePrUrl
Old main: $oldMain
Release develop: $releaseHead
Final main: $finalMain
Archive tag: $archiveTag -> $oldMain
Milestone tag: $milestoneTag -> $finalMain
Branch protection: PR required; strict Unit Tests and PostgreSQL Integration Tests; conversations resolved; admins enforced; force pushes and deletion blocked
Local verification: 64 unit tests; 64 unit + 64 integration tests; all passed
GitHub verification: both required checks passed on release PR and final main
Default branch: main
Develop: retained unchanged after promotion; no open PR depends on it
Production deployment: not part of this milestone
"@
$completionComment
```

Copy the generated text without alteration into KAN-14. Confirm the printed URL
and SHAs match the earlier evidence, then move KAN-14 from `In Progress` to
`Done`.

- [ ] **Step 5: Establish the next-work starting point**

```powershell
git switch main
git pull --ff-only origin main
git status --short --branch
```

Expected: future Jira task branches are created from this clean, verified
`main`. `develop` remains present but inactive; its deletion is not part of
KAN-14.

## Official References

- [GitHub protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [GitHub branch protection REST API](https://docs.github.com/en/rest/branches/branch-protection)
- [GitHub CLI pull-request creation](https://cli.github.com/manual/gh_pr_create)
- [GitHub CLI required-check monitoring](https://cli.github.com/manual/gh_pr_checks)
- [GitHub CLI merge command](https://cli.github.com/manual/gh_pr_merge)
