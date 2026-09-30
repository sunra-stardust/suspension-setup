---
name: ship
description: How a change reaches the owner's phone (push → tests → signed APK → emulator test → GitHub Release → in-app update), how to check a run, fix a failed ship, and roll back a broken release. Use whenever you push, release, or the owner reports a broken update.
---

# Ship, check, roll back

## The flow

```
push main / claude/<topic>
  └─ ship.yml → pipeline.yml
       verify       ./gradlew verify (unit + Robolectric UI tests, both flavors)
       build        signed githubRelease APK, version, release notes, latest.json (+ SHA-256)
       device-test  emulator: previous release → monkey → install new over it → start + 500 random taps;
                    then fresh install → start + 500 random taps. Any crash = red.
       publish      claude/*: fast-forward main to the commit, delete the branch
                    tag v<version>, GitHub Release with APK + latest.json (marked latest)
```

- **Version** = `versionBase` from `version.properties` + `.` + commit count on main (e.g. `0.6.27`). The count only grows, so every release can update the previous one. Bump `versionBase` only for a milestone.
- **Pull requests against `main`** run `pr-check.yml` (`research.py check` + `./gradlew verify`); it publishes nothing.
- **Catalog research PRs** (skill `catalog-research`): commits whose message contains `[catalog-review]` are not shipped from a `claude/*` branch; they ship when the owner merges the PR into `main`.
- **Docs/agent-only changes** (nothing under `app/`, `gradle/`, build files, `version.properties`) are tested and moved to main, but produce no release.
- **Release notes** = commit subjects since the previous tag. They appear in the app's update banner — keep them short, German, user-facing.
- The phone checks `releases/latest/download/latest.json` at most every 12 h (and on "Nach Updates suchen" in Basics → App). The APK is downloaded, checked against the SHA-256 and handed to Android's installer (the owner taps "Installieren").

## Check a run

```bash
gh run list --workflow ship.yml --limit 3
gh run view <run-id> --log-failed        # only the failing step's log
gh release view --json tagName,assets    # what the phone will see
```

Report to the owner: shipped version, or which job failed and why.

## When it fails

| Job | Typical cause | Fix |
|---|---|---|
| Tests → "not based on the current main" | main moved since you branched | `git fetch origin main && git rebase origin/main && ./gradlew verify && git push --force-with-lease` |
| Tests | a test is red | download the `test-reports` artifact or run `./gradlew verify` locally; fix, push again |
| Signed APK | `RELEASE_KEYSTORE_BASE64 secret missing` | owner must restore the repo secrets — never publish a debug-signed APK |
| Emulator | crash after upgrade or under monkey | the job log ends with the crash stack; reproduce as a test (often: stored data from the old version not readable) |
| Publish → push to main rejected | another change reached main first | rebase as above and push again |

Nothing reaches the phone unless every job is green; a red run needs no cleanup.

## Roll back a broken release

Android never installs an older version over a newer one. A rollback therefore republishes the **app code** of an earlier release as a **new** version; the phone offers it like any update.

```bash
gh workflow run rollback.yml                     # back to the release before the current one
gh workflow run rollback.yml -f version=0.6.21   # back to a specific release
```

The owner can do the same from the GitHub app: *Actions → Rollback → Run workflow*.

The workflow commits the old `app/`, `gradle/` and build files onto main (pipeline, `.claude/`, `version.properties` stay current) and runs the normal pipeline, including the emulator upgrade test from the broken release. Afterwards: find the bug, write a failing test, fix it on top of main — the rollback commit is part of history, never force-push it away.

Limits: releases before the in-app updater (≤ v0.5.2) can't be targets. A rollback only works if the older code can read data written by the newer one — hence rule 3 in `CLAUDE.md` (additive data changes only).

If the app crashes twice within 10 minutes, it opens in **safe mode** (update check + "Trotzdem starten"), so a fixed or rolled-back release can always be installed from inside the app.
