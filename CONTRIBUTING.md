# Contributing

## Branch lifecycle

Optrabidz uses two permanent branches:

- `main` contains verified milestone releases and is the default branch.
- `develop` integrates reviewed work for the next milestone.

Create each change from the latest `develop` branch and keep it focused on one
technical outcome. Use a short-lived branch with a concise, meaningful
description:

| Change type | Pattern | Example |
| --- | --- | --- |
| Product work | `feature/<description>` | `feature/api-error-contract` |
| Defect fix | `bugfix/<description>` | `bugfix/invalid-status-handling` |
| Test-only change | `test/<description>` | `test/payment-timeout` |
| Documentation | `docs/<description>` | `docs/security-guide` |
| Maintenance | `chore/<description>` | `chore/ci-runtime` |
| Release preparation | `release/<description>` | `release/notification-foundation` |

The normal delivery path is:

1. Create a short-lived branch from the latest `develop`.
2. Commit only the approved technical scope.
3. Open a pull request into `develop` and let the required checks finish.
4. Address review findings and merge only after the change is accepted.
5. Delete the merged head branch. GitHub performs this automatically.

When a milestone is complete, open a separate release pull request from
`develop` into `main`. Verify the complete release range and required checks
before merging it. Record release and rollback checkpoints with immutable Git
tags instead of retaining completed feature or release branches.

Never include `main`, `develop`, an active branch, or an unmerged branch in
routine cleanup. Deleting a merged head branch does not remove its commits,
pull-request discussion or review history.
