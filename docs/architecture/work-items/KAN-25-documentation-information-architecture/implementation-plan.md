# KAN-25 Documentation Information Architecture Implementation Plan

**Goal:** Make repository documentation easy to navigate by engineering subject while preserving Jira-linked delivery history.

**Architecture:** `docs/README.md` becomes the portal. Stable current-system references live directly under their subject; historical designs, plans, and diagrams live under subject-specific `work-items/<Jira-key>-<slug>/` directories. An existing-JUnit validator protects all local Markdown and HTML documentation targets without introducing another runtime or dependency.

**Tech stack:** Git, Markdown, Java 21, JUnit 5, Maven Wrapper, GitHub Actions

## Global Constraints

- Execute only after KAN-24 has merged into `develop`.
- Rebase this branch onto the latest verified `origin/develop` before moving files.
- Move tracked files with `git mv`; do not recreate them at new paths.
- Keep stable references separate from Jira work-item history.
- Keep rendered diagrams and editable sources beside their owning document.
- Do not create empty subject directories.
- Do not test external URL availability.
- Do not change production Java behavior, database schema, Flyway migrations, application configuration, runtime dependencies, API contracts, CI workflow definitions, or deployment behavior.
- Do not merge into `develop` until the pull request is reviewed and approved.

## File Map

### Create

| File | Responsibility |
|---|---|
| `docs/README.md` | Documentation portal by subject, task, and Jira key. |
| `docs/architecture/README.md` | Current architecture entry point. |
| `docs/error-handling/README.md` | Current error-handling entry point and work-item history. |
| `src/test/java/com/project/optrabidz/documentation/DocumentationLinkValidator.java` | Test-only local link and image target validator. |
| `src/test/java/com/project/optrabidz/documentation/DocumentationLinkValidatorTest.java` | Focused parser and path-safety tests. |
| `src/test/java/com/project/optrabidz/documentation/DocumentationLinksTest.java` | Repository-wide documentation integrity test. |

### Move

| Current path | Destination |
|---|---|
| `docs/architecture.mmd` | `docs/architecture/overview.mmd` |
| `docs/assets/optrabidz-architecture-overview.svg` | `docs/architecture/assets/optrabidz-architecture-overview.svg` |
| `docs/design/KAN-12-migration-policy-design.md` | `docs/database/work-items/KAN-12-migration-policy/design.md` |
| `docs/design/KAN-12-migration-policy-implementation-plan.md` | `docs/database/work-items/KAN-12-migration-policy/implementation-plan.md` |
| `docs/design/KAN-14-database-foundation-release-design.md` | `docs/database/work-items/KAN-14-database-foundation-release/design.md` |
| `docs/design/KAN-14-database-foundation-release-implementation-plan.md` | `docs/database/work-items/KAN-14-database-foundation-release/implementation-plan.md` |
| `docs/design/KAN-17-exception-handling-foundation-design.md` | `docs/error-handling/work-items/KAN-17-foundation/design.md` |
| `docs/design/KAN-20-neutral-error-contract-implementation-plan.md` | `docs/error-handling/work-items/KAN-20-neutral-contract/implementation-plan.md` |
| `docs/design/KAN-21-rfc9457-rest-error-adapter-implementation-plan.md` | `docs/error-handling/work-items/KAN-21-rest-adapter/implementation-plan.md` |
| `docs/design/KAN-22-mvc-problem-details-implementation-plan.md` | `docs/error-handling/work-items/KAN-22-mvc-adapter/implementation-plan.md` |
| `docs/design/KAN-23-security-problem-details-implementation-plan.md` | `docs/error-handling/work-items/KAN-23-security-adapter/implementation-plan.md` |
| `docs/design/KAN-24-module-error-migration-design.md` | `docs/error-handling/work-items/KAN-24-module-migration/design.md` |
| `docs/design/KAN-24-module-error-migration-implementation-plan.md` | `docs/error-handling/work-items/KAN-24-module-migration/implementation-plan.md` |
| `docs/assets/KAN-24-module-error-architecture.mmd` | `docs/error-handling/work-items/KAN-24-module-migration/assets/architecture.mmd` |
| `docs/assets/KAN-24-module-error-architecture.png` | `docs/error-handling/work-items/KAN-24-module-migration/assets/architecture.png` |
| `docs/assets/KAN-24-login-disclosure.mmd` | `docs/error-handling/work-items/KAN-24-module-migration/assets/login-flow.mmd` |
| `docs/assets/KAN-24-login-disclosure.png` | `docs/error-handling/work-items/KAN-24-module-migration/assets/login-flow.png` |

### Modify

| File | Change |
|---|---|
| `README.md` | Link to the portal and relocated architecture image. |
| `docs/database/README.md` | Separate current references from KAN-12 and KAN-14 history. |
| Moved Markdown files | Repair relative links and stale canonical paths. |

---

### Task 1: Synchronize With the KAN-24 Baseline

**Files:** No content changes.

**Produces:** A clean KAN-25 branch whose history contains the merged KAN-24 documents and assets.

- [ ] **Step 1: Verify both worktrees are clean**

```powershell
git -C C:\Users\kumar\IdeaProjects\optrabidz status --short
git status --short
```

Expected: both commands produce no file entries.

- [ ] **Step 2: Update the remote baseline**

```powershell
git fetch origin develop
git rebase origin/develop
```

Expected: rebase succeeds without losing the KAN-25 design commit.

- [ ] **Step 3: Verify the required KAN-24 inputs**

```powershell
$required = @(
  'docs/design/KAN-24-module-error-migration-design.md',
  'docs/design/KAN-24-module-error-migration-implementation-plan.md',
  'docs/assets/KAN-24-module-error-architecture.mmd',
  'docs/assets/KAN-24-module-error-architecture.png',
  'docs/assets/KAN-24-login-disclosure.mmd',
  'docs/assets/KAN-24-login-disclosure.png'
)
$missing = $required | Where-Object { -not (Test-Path -LiteralPath $_) }
if ($missing) { throw "KAN-24 is not ready: $($missing -join ', ')" }
```

Expected: no exception.

- [ ] **Step 4: Re-run the clean baseline**

```powershell
.\mvnw.cmd -B test
```

Expected: `BUILD SUCCESS` with zero failures and errors.

### Task 2: Add the Documentation Integrity Test

**Files:**

- Create: `src/test/java/com/project/optrabidz/documentation/DocumentationLinkValidator.java`
- Create: `src/test/java/com/project/optrabidz/documentation/DocumentationLinkValidatorTest.java`
- Create: `src/test/java/com/project/optrabidz/documentation/DocumentationLinksTest.java`

**Interfaces:**

- Produces: `DocumentationLinkValidator.findBrokenTargets(Path repositoryRoot)` returning `List<BrokenTarget>`.
- Produces: `BrokenTarget(Path source, String target, String reason)` with a readable `toString()` supplied by the record.

- [ ] **Step 1: Write focused failing tests**

Create `DocumentationLinkValidatorTest.java` with tests that require Markdown image/link parsing, HTML `src`/`href` parsing, query and fragment removal, percent-decoding, external-link exclusion, and repository-boundary enforcement:

```java
package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentationLinkValidatorTest {

    @TempDir
    Path repository;

    @Test
    void reportsBrokenMarkdownAndHtmlTargets() throws Exception {
        Files.createDirectories(repository.resolve("docs"));
        Files.writeString(repository.resolve("README.md"),
                "[missing](docs/missing.md)\n<img src=\"docs/missing.png\">\n");

        assertThat(DocumentationLinkValidator.findBrokenTargets(repository))
                .extracting(DocumentationLinkValidator.BrokenTarget::target)
                .containsExactlyInAnyOrder("docs/missing.md", "docs/missing.png");
    }

    @Test
    void acceptsExistingEncodedTargetsAndIgnoresExternalTargets() throws Exception {
        Files.createDirectories(repository.resolve("docs/a folder"));
        Files.writeString(repository.resolve("docs/a folder/design.md"), "# Design\n");
        Files.writeString(repository.resolve("README.md"), """
                [design](docs/a%20folder/design.md#decision)
                [section](#local-section)
                [website](https://example.com)
                [email](mailto:team@example.com)
                """);

        assertThat(DocumentationLinkValidator.findBrokenTargets(repository)).isEmpty();
    }

    @Test
    void rejectsTargetsOutsideRepository() throws Exception {
        Files.createDirectories(repository.resolve("docs"));
        Files.writeString(repository.resolve("docs/index.md"), "[escape](../../secret.md)\n");

        assertThat(DocumentationLinkValidator.findBrokenTargets(repository))
                .singleElement()
                .satisfies(target -> assertThat(target.reason()).isEqualTo("escapes repository root"));
    }

    @Test
    void ignoresTargetsInsideFencedCodeExamples() throws Exception {
        Files.createDirectories(repository.resolve("docs"));
        String fence = "`".repeat(3);
        Files.writeString(repository.resolve("docs/plan.md"),
                fence + "markdown\n[example](missing-example.md)\n" + fence + "\n");

        assertThat(DocumentationLinkValidator.findBrokenTargets(repository)).isEmpty();
    }
}
```

- [ ] **Step 2: Add the repository-wide failing test**

Create `DocumentationLinksTest.java`:

```java
package com.project.optrabidz.documentation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DocumentationLinksTest {

    @Test
    void localDocumentationTargetsResolve() throws Exception {
        Path repositoryRoot = Path.of("").toAbsolutePath().normalize();

        assertThat(DocumentationLinkValidator.findBrokenTargets(repositoryRoot))
                .as("broken local documentation targets")
                .isEmpty();
    }
}
```

- [ ] **Step 3: Run the tests to verify RED**

```powershell
.\mvnw.cmd -B -Dtest=DocumentationLinkValidatorTest,DocumentationLinksTest test
```

Expected: compilation fails because `DocumentationLinkValidator` does not exist.

- [ ] **Step 4: Implement the test-only validator**

Create `DocumentationLinkValidator.java` with these rules:

```java
package com.project.optrabidz.documentation;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class DocumentationLinkValidator {

    private static final Pattern MARKDOWN_TARGET = Pattern.compile(
            "!?\\[[^]]*]\\((?:<([^>]+)>|([^\\s)]+))(?:\\s+[\\\"'][^\\\"']*[\\\"'])?\\)");
    private static final Pattern HTML_TARGET = Pattern.compile(
            "(?:href|src)\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FENCED_CODE = Pattern.compile("(?ms)^```.*?^```\\s*$");

    private DocumentationLinkValidator() {
    }

    static List<BrokenTarget> findBrokenTargets(Path repositoryRoot) throws IOException {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        List<Path> markdownFiles = new ArrayList<>();
        Path rootReadme = root.resolve("README.md");
        if (Files.isRegularFile(rootReadme)) {
            markdownFiles.add(rootReadme);
        }
        Path docs = root.resolve("docs");
        if (Files.isDirectory(docs)) {
            try (Stream<Path> paths = Files.walk(docs)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".md"))
                        .forEach(markdownFiles::add);
            }
        }

        List<BrokenTarget> broken = new ArrayList<>();
        for (Path source : markdownFiles) {
            String content = FENCED_CODE.matcher(Files.readString(source)).replaceAll("");
            collectTargets(MARKDOWN_TARGET.matcher(content), source, root, broken, true);
            collectTargets(HTML_TARGET.matcher(content), source, root, broken, false);
        }
        return List.copyOf(broken);
    }

    private static void collectTargets(
            Matcher matcher,
            Path source,
            Path root,
            List<BrokenTarget> broken,
            boolean markdown) {
        while (matcher.find()) {
            String target = markdown
                    ? (matcher.group(1) != null ? matcher.group(1) : matcher.group(2))
                    : matcher.group(1);
            validateTarget(source, root, target, broken);
        }
    }

    private static void validateTarget(
            Path source, Path root, String rawTarget, List<BrokenTarget> broken) {
        String lower = rawTarget.toLowerCase(Locale.ROOT);
        if (rawTarget.startsWith("#") || lower.startsWith("http://")
                || lower.startsWith("https://") || lower.startsWith("mailto:")
                || lower.startsWith("data:")) {
            return;
        }

        String pathOnly = rawTarget.split("[?#]", 2)[0];
        Path resolved = source.getParent()
                .resolve(URLDecoder.decode(pathOnly, StandardCharsets.UTF_8))
                .normalize()
                .toAbsolutePath();
        if (!resolved.startsWith(root)) {
            broken.add(new BrokenTarget(root.relativize(source), rawTarget, "escapes repository root"));
        } else if (!Files.exists(resolved)) {
            broken.add(new BrokenTarget(root.relativize(source), rawTarget, "target does not exist"));
        }
    }

    record BrokenTarget(Path source, String target, String reason) {
    }
}
```

- [ ] **Step 5: Run focused and complete unit tests**

```powershell
.\mvnw.cmd -B -Dtest=DocumentationLinkValidatorTest,DocumentationLinksTest test
.\mvnw.cmd -B test
```

Expected: both commands report `BUILD SUCCESS`.

- [ ] **Step 6: Commit the integrity test**

```powershell
git add -- src/test/java/com/project/optrabidz/documentation
git commit -m "test: validate documentation targets (KAN-25)"
```

### Task 3: Establish the Portal and Architecture Topic

**Files:**

- Create: `docs/README.md`
- Create: `docs/architecture/README.md`
- Move: `docs/architecture.mmd`
- Move: `docs/assets/optrabidz-architecture-overview.svg`
- Modify: `README.md`

**Produces:** Working subject/task/Jira navigation and canonical architecture paths.

- [ ] **Step 1: Move architecture assets without changing their content**

```powershell
New-Item -ItemType Directory -Force docs/architecture/assets | Out-Null
git mv docs/architecture.mmd docs/architecture/overview.mmd
git mv docs/assets/optrabidz-architecture-overview.svg docs/architecture/assets/optrabidz-architecture-overview.svg
```

- [ ] **Step 2: Run the repository link test to expose stale paths**

```powershell
.\mvnw.cmd -B -Dtest=DocumentationLinksTest test
```

Expected: FAIL because the root README still points at the former SVG path.

- [ ] **Step 3: Add the portal and architecture index**

Create `docs/README.md` with these sections and links:

```markdown
# OptraBidz Documentation

## Start Here

- [System architecture](architecture/README.md)
- [Database design](database/README.md)
- [Error handling](error-handling/README.md)

## Find Documentation by Task

| Task | Start here |
|---|---|
| Understand system boundaries | [Architecture](architecture/README.md) |
| Change the database safely | [Database migrations](database/migrations.md) |
| Add or change an API error | [Error handling](error-handling/README.md) |
| Review why a change was made | [Work-item index](#work-item-index) |

## Work-item Index

The final index lists KAN-12, KAN-14, KAN-17, and KAN-20 through KAN-25.
```

Create `docs/architecture/README.md` linking the editable overview and rendered image, then listing KAN-25 separately under `Work-item History`.

- [ ] **Step 4: Repair root README navigation**

Change the architecture image/link target to:

```text
docs/architecture/assets/optrabidz-architecture-overview.svg
```

Add a visible `Documentation` link to `docs/README.md` near the onboarding sections.

- [ ] **Step 5: Verify links and moved-file identity**

```powershell
.\mvnw.cmd -B -Dtest=DocumentationLinksTest test
git diff --summary
git diff --check
```

Expected: test passes; the diff summary reports two renames; diff check is clean.

- [ ] **Step 6: Commit the portal and architecture topic**

```powershell
git add -- README.md docs/README.md docs/architecture
git commit -m "docs: add topic-first documentation portal (KAN-25)"
```

### Task 4: Move Database Work-item History

**Files:**

- Move the four KAN-12/KAN-14 files listed in the file map.
- Modify: `docs/database/README.md`
- Modify: moved KAN-12/KAN-14 files where they name canonical paths.
- Modify: `docs/README.md`

- [ ] **Step 1: Create destinations and move tracked files**

```powershell
New-Item -ItemType Directory -Force docs/database/work-items/KAN-12-migration-policy | Out-Null
New-Item -ItemType Directory -Force docs/database/work-items/KAN-14-database-foundation-release | Out-Null
git mv docs/design/KAN-12-migration-policy-design.md docs/database/work-items/KAN-12-migration-policy/design.md
git mv docs/design/KAN-12-migration-policy-implementation-plan.md docs/database/work-items/KAN-12-migration-policy/implementation-plan.md
git mv docs/design/KAN-14-database-foundation-release-design.md docs/database/work-items/KAN-14-database-foundation-release/design.md
git mv docs/design/KAN-14-database-foundation-release-implementation-plan.md docs/database/work-items/KAN-14-database-foundation-release/implementation-plan.md
```

- [ ] **Step 2: Find stale canonical paths**

```powershell
rg -n "docs/design/KAN-(12|14)|design/KAN-(12|14)" README.md docs
```

Expected: matches identify text that must refer to the new canonical locations.

- [ ] **Step 3: Repair paths and split reference from history**

Update `docs/database/README.md` so `Start Here` contains only current database references and a separate `Work-item History` table links KAN-12 and KAN-14. Update `docs/README.md` Jira index and all stale canonical path statements in moved documents.

- [ ] **Step 4: Verify the database move**

```powershell
if (rg -n "docs/design/KAN-(12|14)|design/KAN-(12|14)" README.md docs) { throw 'stale database documentation path' }
.\mvnw.cmd -B -Dtest=DocumentationLinksTest test
git diff --summary
git diff --check
```

Expected: no stale-path exception, test passes, four moves appear as renames, and diff check is clean.

- [ ] **Step 5: Commit database history**

```powershell
git add -- docs/README.md docs/database
git commit -m "docs: organize database work-item history (KAN-25)"
```

### Task 5: Move Error-handling History Through KAN-23

**Files:**

- Create: `docs/error-handling/README.md`
- Move: KAN-17 and KAN-20 through KAN-23 files listed in the file map.
- Modify: `docs/README.md`
- Modify: moved files where they name canonical paths.

- [ ] **Step 1: Create destinations and move tracked files**

```powershell
$destinations = @(
  'docs/error-handling/work-items/KAN-17-foundation',
  'docs/error-handling/work-items/KAN-20-neutral-contract',
  'docs/error-handling/work-items/KAN-21-rest-adapter',
  'docs/error-handling/work-items/KAN-22-mvc-adapter',
  'docs/error-handling/work-items/KAN-23-security-adapter'
)
$destinations | ForEach-Object { New-Item -ItemType Directory -Force $_ | Out-Null }
git mv docs/design/KAN-17-exception-handling-foundation-design.md docs/error-handling/work-items/KAN-17-foundation/design.md
git mv docs/design/KAN-20-neutral-error-contract-implementation-plan.md docs/error-handling/work-items/KAN-20-neutral-contract/implementation-plan.md
git mv docs/design/KAN-21-rfc9457-rest-error-adapter-implementation-plan.md docs/error-handling/work-items/KAN-21-rest-adapter/implementation-plan.md
git mv docs/design/KAN-22-mvc-problem-details-implementation-plan.md docs/error-handling/work-items/KAN-22-mvc-adapter/implementation-plan.md
git mv docs/design/KAN-23-security-problem-details-implementation-plan.md docs/error-handling/work-items/KAN-23-security-adapter/implementation-plan.md
```

- [ ] **Step 2: Write the current-system topic index**

Create `docs/error-handling/README.md` with:

- the public RFC 9457 response boundary;
- the neutral `ErrorDescriptor`/`ApplicationException` core boundary;
- MVC and Spring Security adapter boundaries;
- a link to the KAN-24 current module migration design; and
- a separate chronological work-item history for KAN-17 and KAN-20 through KAN-24.

- [ ] **Step 3: Repair stale canonical paths**

```powershell
rg -n "docs/design/KAN-(17|20|21|22|23)|design/KAN-(17|20|21|22|23)" README.md docs
```

Update every result that identifies a canonical repository file, then update the Jira index in `docs/README.md`.

- [ ] **Step 4: Verify links and stale paths**

```powershell
if (rg -n "docs/design/KAN-(17|20|21|22|23)|design/KAN-(17|20|21|22|23)" README.md docs) { throw 'stale error-handling documentation path' }
.\mvnw.cmd -B -Dtest=DocumentationLinksTest test
git diff --summary
git diff --check
```

Expected: validator passes, five moves appear as renames, and no stale path remains.

- [ ] **Step 5: Commit error-handling history**

```powershell
git add -- docs/README.md docs/error-handling
git commit -m "docs: organize error-handling history (KAN-25)"
```

### Task 6: Move KAN-24 Documents and Owned Diagrams

**Files:**

- Move: both KAN-24 Markdown files and four assets listed in the file map.
- Modify: moved KAN-24 Markdown image/source links.
- Modify: `docs/README.md`
- Modify: `docs/error-handling/README.md`

- [ ] **Step 1: Move the work item as one unit**

```powershell
New-Item -ItemType Directory -Force docs/error-handling/work-items/KAN-24-module-migration/assets | Out-Null
git mv docs/design/KAN-24-module-error-migration-design.md docs/error-handling/work-items/KAN-24-module-migration/design.md
git mv docs/design/KAN-24-module-error-migration-implementation-plan.md docs/error-handling/work-items/KAN-24-module-migration/implementation-plan.md
git mv docs/assets/KAN-24-module-error-architecture.mmd docs/error-handling/work-items/KAN-24-module-migration/assets/architecture.mmd
git mv docs/assets/KAN-24-module-error-architecture.png docs/error-handling/work-items/KAN-24-module-migration/assets/architecture.png
git mv docs/assets/KAN-24-login-disclosure.mmd docs/error-handling/work-items/KAN-24-module-migration/assets/login-flow.mmd
git mv docs/assets/KAN-24-login-disclosure.png docs/error-handling/work-items/KAN-24-module-migration/assets/login-flow.png
```

- [ ] **Step 2: Repair KAN-24 owned links**

In both moved Markdown files, replace global asset links with local links:

```markdown
![Module error translation architecture](assets/architecture.png)

Editable source: [architecture.mmd](assets/architecture.mmd)

![Login disclosure and audit flow](assets/login-flow.png)

Editable source: [login-flow.mmd](assets/login-flow.mmd)
```

Repair references between `design.md` and `implementation-plan.md`, and update portal/topic indexes.

- [ ] **Step 3: Verify cleanup and integrity**

```powershell
if (Test-Path docs/design) {
  $remainingDesign = Get-ChildItem docs/design -File -Recurse
  if ($remainingDesign) { throw "Unclassified design files: $($remainingDesign.FullName -join ', ')" }
}
if (Test-Path docs/assets) {
  $remainingAssets = Get-ChildItem docs/assets -File -Recurse
  if ($remainingAssets) { throw "Unclassified assets: $($remainingAssets.FullName -join ', ')" }
}
if (rg -n "docs/design/|docs/assets/KAN-24|\.\./assets/KAN-24" README.md docs) {
  throw 'stale flat documentation path'
}
.\mvnw.cmd -B -Dtest=DocumentationLinksTest test
git diff --summary
git diff --check
```

Expected: no unclassified file, no stale path, link test passes, and all six KAN-24 files appear as renames.

- [ ] **Step 4: Verify diagram files**

```powershell
$images = @(
  'docs/error-handling/work-items/KAN-24-module-migration/assets/architecture.png',
  'docs/error-handling/work-items/KAN-24-module-migration/assets/login-flow.png'
)
$images | ForEach-Object {
  if ((Get-Item -LiteralPath $_).Length -le 0) { throw "Empty diagram: $_" }
}
```

Open the moved `design.md` through GitHub's mobile viewport during PR review and confirm both PNGs render. Keep both `.mmd` sources downloadable.

- [ ] **Step 5: Commit KAN-24 documentation ownership**

```powershell
git add -A -- docs
git commit -m "docs: colocate KAN-24 diagrams and records (KAN-25)"
```

### Task 7: Final Audit and Pull-request Evidence

**Files:**

- Modify: `docs/architecture/work-items/KAN-25-documentation-information-architecture/implementation-plan.md` only to mark completed checkboxes and record exact evidence.

- [ ] **Step 1: Verify the final documentation inventory**

```powershell
rg --files docs | Sort-Object
git status --short
git diff origin/develop...HEAD --name-status
```

Expected: every document is under a defined subject, with no `docs/design` or global `docs/assets` file remaining.

- [ ] **Step 2: Run focused documentation tests**

```powershell
.\mvnw.cmd -B -Dtest=DocumentationLinkValidatorTest,DocumentationLinksTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run the complete unit suite**

```powershell
.\mvnw.cmd -B test
```

Expected: `BUILD SUCCESS` with zero failures and errors.

- [ ] **Step 4: Run the PostgreSQL integration suite**

```powershell
.\mvnw.cmd -B verify -Pintegration-tests
```

Expected: `BUILD SUCCESS` with zero failures and errors.

- [ ] **Step 5: Perform repository safety checks**

```powershell
git diff --check origin/develop...HEAD
git diff --name-only origin/develop...HEAD -- src/main src/main/resources pom.xml .github/workflows
```

Expected: diff check is clean. The protected-path command prints nothing except the intentional test-only files are outside these paths.

- [ ] **Step 6: Record evidence and commit the completed plan**

Update this plan with the exact test totals, commit SHAs, rename count, and GitHub mobile observation.

```powershell
git add -- docs/architecture/work-items/KAN-25-documentation-information-architecture/implementation-plan.md
git commit -m "docs: record documentation migration evidence (KAN-25)"
```

- [ ] **Step 7: Push one review branch and open one pull request**

```powershell
git push -u origin docs/KAN-25-information-architecture
$body = "## Summary`n- organize documentation by engineering subject`n- separate stable references from Jira work-item history`n- colocate diagrams with their owning work item`n- add repository-local documentation link validation`n`n## Verification`n- focused documentation tests passed`n- complete unit suite passed`n- PostgreSQL integration suite passed`n- KAN-24 diagrams render on GitHub mobile`n`n## Risk and rollback`nDocumentation-only path migration plus test-only validation. Roll back by reverting this PR."
gh pr create --base develop --head docs/KAN-25-information-architecture --title "KAN-25: Establish scalable documentation information architecture" --body $body
```

Do not merge until the user reviews and approves the pull request.
