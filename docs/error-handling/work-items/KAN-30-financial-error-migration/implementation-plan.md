# KAN-30 Financial Error Migration Design Delivery Plan

**Status:** Approved implementation plan

**Goal:** Publish the approved financial error-migration design, create the six
independent Jira delivery stories it defines, and prepare a documentation-only
pull request.

**Architecture:** KAN-30 remains a design and planning story. It records the
security and error-contract decisions once, while each production subsystem is
delivered later through its own story, specification, implementation plan,
branch, tests, and pull request.

**Tech stack:** Markdown, Mermaid CLI, Git, GitHub pull requests, Jira

**Spec:**
`docs/error-handling/work-items/KAN-30-financial-error-migration/design.md`

## Global constraints

- Change documentation only; do not modify `src/main`, `src/test`, Flyway,
  Maven configuration, runtime properties, or CI workflows in KAN-30.
- Use branch `docs/KAN-30-financial-exception-design` targeting `develop`.
- Keep the approved `.mmd` source and rendered `.png` together under `assets`.
- Create six separate Jira stories because webhook security, webhook
  persistence, three capability migrations, and legacy cleanup are
  independently reviewable changes.
- Assign every new story to the project owner and place it in To Do.
- Use epic KAN-16 as the parent of every new Story.
- Link every new story to KAN-30 with Jira's `Relates` link and create the
  approved sequential `Blocks` dependency chain.
- Do not include internal tooling identity, private filesystem paths,
  credentials, secrets, or informal approval instructions in repository, Jira,
  commit, or pull-request text.
- Do not merge the pull request until review is complete and required checks
  pass at the exact head.

---

### Task 1: Finalize the approved documentation record

**Files:**

- Modify: `docs/error-handling/README.md`
- Modify: `docs/error-handling/work-items/KAN-30-financial-error-migration/design.md`
- Create:
  `docs/error-handling/work-items/KAN-30-financial-error-migration/implementation-plan.md`
- Verify:
  `docs/error-handling/work-items/KAN-30-financial-error-migration/assets/financial-error-flow.mmd`
- Verify:
  `docs/error-handling/work-items/KAN-30-financial-error-migration/assets/financial-error-flow.png`

**Produces:** An indexed, approved design plus a reviewable execution record.

- [ ] **Step 1: Confirm the design records the approved gate**

Run:

```powershell
rg -n "Approved written specification|## 13. Follow-up delivery stories|## 14. Verification gates|## 15. Legacy deletion conditions" docs/error-handling/work-items/KAN-30-financial-error-migration/design.md
```

Expected: one status match and all three required sections.

- [ ] **Step 2: Render the editable diagram**

Run:

```powershell
npx --yes @mermaid-js/mermaid-cli `
  -i docs/error-handling/work-items/KAN-30-financial-error-migration/assets/financial-error-flow.mmd `
  -o docs/error-handling/work-items/KAN-30-financial-error-migration/assets/financial-error-flow.png `
  -b white -w 1600
```

Expected: exit code 0 and `Generating single mermaid chart`.

- [ ] **Step 3: Verify local document references**

Run:

```powershell
$design = 'docs/error-handling/work-items/KAN-30-financial-error-migration/design.md'
$content = Get-Content -Raw $design
$refs = [regex]::Matches($content, 'assets/[A-Za-z0-9._/-]+') |
  ForEach-Object Value | Sort-Object -Unique
$missing = @($refs | Where-Object {
  -not (Test-Path (Join-Path (Split-Path $design) $_))
})
if ($missing.Count -gt 0) {
  throw "Missing local references: $($missing -join ', ')"
}
```

Expected: exit code 0 with no missing references.

- [ ] **Step 4: Confirm the documentation portal links both records**

Run:

```powershell
rg -n "KAN-30.*Financial error migration design.*implementation plan" docs/error-handling/README.md
```

Expected: one KAN-30 history-row match.

### Task 2: Create the six independent delivery stories

**Files:** None.

**Consumes:** The approved delivery order in design section 13.

**Produces:** Six assigned Jira stories with explicit scope, exclusions, and
acceptance criteria.

- [ ] **Step 1: Create the secure webhook-ingress story**

Create a Jira Story with summary:

```text
Harden financial runtime configuration and payment webhook ingress
```

Its description must include:

- remove the implicit `dev` profile fallback;
- validate enabled-provider secret configuration at startup;
- accept bounded exact raw bytes;
- retain only allowlisted protocol headers;
- verify provider signature and signed timestamp before parsing;
- use uniform `PAYMENT_WEBHOOK_REJECTED` 400 behavior;
- parse a strict allowlisted schema only after authentication and use
  `PAYMENT_WEBHOOK_PAYLOAD_INVALID` 400 for authenticated invalid content;
- emit sanitized webhook security audit events;
- exclude replay persistence, financial error migration, provider replacement,
  JWT/OAuth2, and payment-rule redesign; and
- require focused security/configuration tests plus full regression suites.

Acceptance requires invalid, altered, stale, unknown-provider, and oversized
requests to cause no parsing side effect or business mutation, while no secret,
signature, raw body, or configuration fact is disclosed.

- [ ] **Step 2: Create the replay and disclosure story**

Create a Jira Story with summary:

```text
Add payment webhook replay protection and safe acknowledgement
```

Its description must include:

- map and use the existing `payment_webhook_event` table;
- atomically claim `(provider_code, provider_event_id)`;
- bind event identity to authenticated canonical payload hash;
- return minimal 2xx for identical authenticated duplicates;
- reject and safely audit event-ID reuse with different immutable content;
- produce at most one financial transition, outbox event, and business audit
  event under concurrency;
- replace full payment-attempt webhook responses with a minimal acknowledgement;
- store only bounded normalized provider fields; and
- exclude payment/settlement/repayment exception migration.

Acceptance requires PostgreSQL concurrency, rollback, duplicate, collision,
response-disclosure, and exactly-once side-effect tests.

- [ ] **Step 3: Create the payment error-migration story**

Create a Jira Story with summary:

```text
Migrate payment intent and attempt errors to the neutral contract
```

Its description must include the approved payment descriptors:

```text
PAYMENT_INTENT_NOT_FOUND
PAYMENT_ATTEMPT_NOT_FOUND
PAYMENT_INTENT_EXPIRED
PAYMENT_INTENT_NOT_ACTIVE
PAYMENT_ALREADY_CONFIRMED
PAYMENT_STATE_CONFLICT
PAYMENT_METHOD_UNSUPPORTED
PAYMENT_PROVIDER_MISMATCH
```

Acceptance requires financial-owned typed `ApplicationException` classes,
ownership-private 404 behavior, approved 409/422 mappings, protected
diagnostics, unexpected-failure preservation, focused API tests, architecture
enforcement, and full regression suites.

- [ ] **Step 4: Create the settlement error-migration story**

Create a Jira Story with summary:

```text
Migrate settlement errors to the neutral contract
```

Its description must include:

```text
SETTLEMENT_NOT_FOUND
SETTLEMENT_NOT_PAYABLE
SETTLEMENT_STATE_CONFLICT
FINANCIAL_OPERATION_NOT_ALLOWED
```

Acceptance requires ownership-private settlement lookup, role/action 403 only
when existence is not disclosed, preserved settlement rules and conditional
transitions, protected diagnostics, focused PostgreSQL/API tests, architecture
enforcement, and full regression suites.

- [ ] **Step 5: Create the repayment error-migration story**

Create a Jira Story with summary:

```text
Migrate repayment and installment errors to the neutral contract
```

Its description must include:

```text
REPAYMENT_NOT_FOUND
REPAYMENT_INSTALLMENT_NOT_FOUND
REPAYMENT_INSTALLMENT_NOT_PAYABLE
REPAYMENT_STATE_CONFLICT
FINANCIAL_OPERATION_NOT_ALLOWED
```

Acceptance requires ownership-private repayment/progress/installment lookup,
preserved payable and conditional-transition rules, protected diagnostics,
focused PostgreSQL/API tests, architecture enforcement, and full regression
suites.

- [ ] **Step 6: Create the legacy-cleanup story**

Create a Jira Story with summary:

```text
Remove the legacy exception stack after all module migrations
```

Its description must copy all seven deletion gates from design section 15 and
exclude any business, security, provider, database, or API redesign.

Acceptance requires zero production references to legacy `ApiException`,
`ErrorCode`, `GlobalExceptionHandler`, the legacy error envelope, and their
obsolete helpers; migrated architecture enforcement for every module; and full
unit, architecture, API, security, Flyway, and PostgreSQL integration success.

### Task 3: Establish Jira hierarchy and dependencies

**Files:** None.

**Consumes:** The six Jira keys returned by Task 2.

**Produces:** A navigable delivery chain whose ordering is visible from Jira.

- [ ] **Step 1: Assign and place the stories**

For each created story:

- set the assignee to the project owner;
- keep status To Do;
- set epic KAN-16 as parent; and
- link it to KAN-30 using Jira link type `Relates`.

- [ ] **Step 2: Create the blocking chain**

Create Jira `Blocks` links in this exact order:

```text
secure webhook ingress
  -> webhook replay/disclosure
  -> payment error migration
  -> settlement error migration
  -> repayment error migration
  -> legacy cleanup
```

The outward issue is the predecessor and the inward issue is its immediate
successor. Do not link later stories as already completed or in progress.

- [ ] **Step 3: Verify Jira state through a read-back query**

Read all six created issues and verify:

- issue type is Story;
- assignee is the project owner;
- status is To Do;
- each story references KAN-30;
- parent KAN-16 is present; and
- the five sequential `Blocks` relationships are present.

Correct Jira data before continuing if any read-back check fails.

### Task 4: Link the delivery stories from the design

**Files:**

- Modify:
  `docs/error-handling/work-items/KAN-30-financial-error-migration/design.md`

**Consumes:** The six verified Jira keys from Task 3.

**Produces:** A design whose delivery section links directly to each executable
story.

- [ ] **Step 1: Add each Jira key to its matching delivery-story heading**

Replace each unlinked numbered heading in design section 13 with its returned
Jira key and Jira URL. Preserve the approved order, descriptions, and scope.

- [ ] **Step 2: Verify section 13 contains six unique delivery-story keys**

Run:

```powershell
$design = 'docs/error-handling/work-items/KAN-30-financial-error-migration/design.md'
$content = Get-Content -Raw $design
$section = [regex]::Match(
  $content,
  '(?s)## 13\. Follow-up delivery stories.*?(?=## 14\.)'
).Value
$keys = @([regex]::Matches($section, 'KAN-\d+') |
  ForEach-Object Value | Sort-Object -Unique)
if ($keys.Count -ne 6) {
  throw "Expected six unique delivery-story keys, found $($keys.Count)"
}
$keys
```

Expected: six unique Jira keys and exit code 0.

- [ ] **Step 3: Record the delivery map on KAN-30**

Add one Jira comment listing the six keys, summaries, dependency order, and
documentation branch. Do not duplicate the complete written specification in
the comment.

### Task 5: Verify and prepare the documentation pull request

**Files:** All KAN-30 documentation records from Tasks 1 and 4.

**Produces:** One documentation-only commit and a reviewable PR to `develop`.

- [ ] **Step 1: Prove that no production or test file changed**

Run:

```powershell
$changed = @(git diff --name-only; git ls-files --others --exclude-standard)
$outsideDocs = @($changed | Where-Object { $_ -notlike 'docs/*' })
if ($outsideDocs.Count -gt 0) {
  throw "Out-of-scope files changed: $($outsideDocs -join ', ')"
}
$changed
```

Expected: only the KAN-30 design, plan, two diagram assets, and error-handling
index.

- [ ] **Step 2: Run whitespace and artifact verification**

Run:

```powershell
git diff --check
Test-Path docs/error-handling/work-items/KAN-30-financial-error-migration/assets/financial-error-flow.mmd
Test-Path docs/error-handling/work-items/KAN-30-financial-error-migration/assets/financial-error-flow.png
```

Expected: `git diff --check` exits 0 and both path checks print `True`.

- [ ] **Step 3: Review the exact diff**

Run:

```powershell
git diff -- docs/error-handling
git status --short --branch
```

Expected: approved documentation only, with no secrets, private paths,
internal-tooling references, clipboard filenames, or unrelated edits.

- [ ] **Step 4: Commit the documentation**

Run:

```powershell
git add docs/error-handling/README.md `
  docs/error-handling/work-items/KAN-30-financial-error-migration
git commit -m "docs(KAN-30): define financial error migration"
```

Expected: one documentation-only commit on
`docs/KAN-30-financial-exception-design`.

- [ ] **Step 5: Push the branch and open the pull request**

Run:

```powershell
git push --set-upstream origin docs/KAN-30-financial-exception-design
gh pr create `
  --base develop `
  --head docs/KAN-30-financial-exception-design `
  --title "KAN-30: Define secure financial error migration" `
  --body "## Objective`nRecord the approved secure financial error-migration architecture and delivery slices.`n`n## Scope`nDocumentation, rendered architecture, Jira delivery map, verification gates, and legacy deletion conditions.`n`n## Verification`n- Mermaid source rendered successfully`n- Local documentation references resolve`n- git diff --check passes`n- No production or test file changed`n`n## Out of scope`nNo runtime configuration, webhook, database, financial business logic, exception implementation, JWT/OAuth2, or provider change is implemented in this PR.`n`nJira: KAN-30"
```

- [ ] **Step 6: Synchronize the review state**

After the PR exists:

- verify Jira development data associates it with KAN-30;
- add the PR URL to KAN-30 if automatic association is absent; and
- transition KAN-30 to In Review.

Stop before merge. Merge, Done transition, and branch deletion occur only after
the PR is reviewed, required checks pass at its exact head, and the review gate
is explicitly completed.
