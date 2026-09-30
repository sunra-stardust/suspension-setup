# Harness concept

How AI agents work on this project: what they may do alone, where the owner decides, and which
checks stand between a change and the owner's phone. Status: **proposal (2026-09-30)** — nothing
here is implemented unless marked ✅. Rollout plan at the end.

"Harness" = everything around the model that turns it into a safe, productive contributor:
context (`CLAUDE.md`, skills), tools and permissions, hooks, tests and CI, triggers (routines,
PR events) and the human gates. The model is replaceable; the harness is what makes its work
trustworthy.

## 1. Principles

1. **Automate everything that a machine can check; ask the human only what a machine can't.**
   The owner decides product questions, data conflicts, new risks and irreversible steps — not
   formatting, not green builds.
2. **Rules are enforced by mechanism, not by prose.** Every "never" in `CLAUDE.md` should have a
   hook, a CI check or a GitHub setting behind it. Prose explains *why*; mechanics make it hold.
3. **Gate by risk, not by habit.** Low-risk changes ship on green; high-risk changes wait for the
   owner. The risk class is computed from the diff, not chosen by the agent.
4. **Every gate is reversible or pre-tested.** What can't be undone (signing key, stored data
   format, published permissions) gets the strongest gate.
5. **Fail loud, fail early.** A local check that fails in 30 s beats a CI run that fails in 8 min
   beats a crash on the phone.

## 2. Today (2026-09-30)

| Layer | In place |
|---|---|
| Context | ✅ `CLAUDE.md`, skills `catalog-data`, `catalog-research`, `ship`, `ui-text`, `docs/ROADMAP.md` |
| Setup | ✅ SessionStart hook `scripts/cloud-setup.sh` (Android SDK) |
| Permissions | ✅ allow-list in `.claude/settings.json`; the cloud environment's auto-mode classifier |
| Tests | ✅ unit + Robolectric UI (both flavors), catalog invariants, translation completeness, damping-vocabulary lint, research format check |
| Pipeline | ✅ push = release: verify → signed APK → emulator (upgrade from last release + fresh install + monkey) → GitHub Release → OTA |
| Safety net | ✅ rollback workflow, crash-loop safe mode, SHA-256 check of the OTA APK |
| Human gates | ✅ research PRs (`[catalog-review]`), owner taps "Installieren" on the phone |

**Gaps seen in practice** (catalog sample run, 2026-09-30):

| # | Gap | Effect |
|---|---|---|
| G1 | Rules like "never push `main`", "no new permissions", "don't touch signing" exist only as text | a mistaken or manipulated agent is stopped only by its own discipline |
| G2 | Every green push ships to the phone — there is no risk distinction between a typo fix and a storage-format change | the only human gate for app code is *after* release (install tap) |
| G3 | `gradlew` is not executable after a cloud clone; the allow-rule `Bash(./gradlew:*)` never matches `bash ./gradlew` | permission prompts / classifier stops |
| G4 | No `gh` CLI in cloud sessions, but skill `ship` assumes it; GitHub MCP can't edit releases | agent can't check or fix what the skill tells it to. Installing `gh` doesn't help: cloud sessions have no GitHub token for it — skill `ship` now names the MCP tools instead |
| G5 | Robolectric downloads its Android jar on first use; the first local run failed on it | false red, wasted cycle |
| G6 | Tests encoded product rules nobody had written down (every fork has a chart; every fork has split rebound) | discovered only at promotion time |
| G7 | Release notes = commit subjects, unchecked | a stale subject reached the phone (v0.6.32) |
| G8 | Actions pinned by tag (`@v5`), no dependency verification, no dependency/secret scanning | supply-chain exposure of the release pipeline (it holds the signing key) |

## 3. Risk classes

Computed from the changed paths (`scripts/risk-class.sh`, used by the agent hook *and* CI). The
highest class of any changed file wins.

| Class | Paths (examples) | Path to the phone |
|---|---|---|
| **R0 — Docs/agent** | `docs/`, `*.md` outside `app/`, `research/`, `.claude/skills/` | green → `main`, no release (✅ today) |
| **R1 — Content** | `res/values*/strings.xml`, `catalog.json` *additions* with passing `CatalogDataTest` | green → release |
| **R2 — Behaviour** | `app/src/main/java/**` except R3 paths, tests, Compose UI | green → release, **plus** automated review gate (§5.4) |
| **R3 — Protected** | storage (`GarageDoc`, `GarageRepository`, `LegacyStorage*`, `data/*Json*`), `AndroidManifest.xml`, `update/`, `src/github/`, `safety/`, `build.gradle.kts`, `gradle/`, `version.properties`, `.github/workflows/`, `scripts/device-smoke.sh`, `.claude/settings.json`, hooks, `CLAUDE.md`, changes/removals in `catalog.json` | **PR, owner approval required**, never auto-ship |

Why these are R3: a storage mistake corrupts riders' data and can't be rolled back; manifest and
updater changes can brick the OTA path or break Play policy; workflows, build files and hooks
*are* the harness — an agent must not be able to weaken its own guard rails.

## 4. Human in the loop — the complete list

The owner is asked **only** for:

| Gate | Trigger | How |
|---|---|---|
| H1 Merge R3 changes | diff contains an R3 path | PR with label `needs-owner`, checklist in the body (§6.3); owner merges |
| H2 Catalog research | research routine | PR `catalog-research` (✅ today) |
| H3 Data conflicts | two sources disagree | `data-conflict` issue (✅ today); agent never picks a side |
| H4 Product decisions | rule gaps like G6, new features, UX wording beyond `ui-text` rules | agent asks in chat / PR before building; decision recorded in `docs/ROADMAP.md` → Decisions |
| H5 New permission, network use, dependency with network/analytics | permission-diff check (§6.2) | always R3 + explicit line in the PR |
| H6 Install on the phone | every release | owner taps "Installieren" (✅ today) — last, informal gate |
| H7 Rollback | crash reports, broken behaviour | owner runs `rollback.yml` or asks the agent; agent may *propose* but not trigger it unasked |
| H8 Secrets, signing key, GitHub settings | any change | owner only; agents have no access |

Everything else — writing code and tests, fixing CI, answering review bots, dependency bumps
that pass all checks, research runs, release notes — runs without asking.

## 5. Agent layer (Claude Code)

### 5.1 Context
- `CLAUDE.md` stays short: rules + pointers. Each rule links to the mechanism enforcing it (§2 G1).
- Skills per workflow (existing four) + new skill **`harness`**: risk classes, how to open an R3 PR, what the hooks do and how to read their messages.
- Decisions live in `docs/ROADMAP.md`; hidden product rules found in tests (G6) are written down there when discovered.

### 5.2 Permissions (`.claude/settings.json`)
- **allow:** read-only git/gh, `bash ./gradlew *`, `./gradlew *`, `python3 scripts/*`, `git push -u origin claude/*`, `git push --force-with-lease origin claude/*`.
- **deny:** `git push * main*`, `git push --force *` (without lease), `git push --delete *`, `git tag *`, `gh release delete*`, `gh workflow run rollback*`, `gh secret *`, `rm -rf` outside `build/`.
- **ask:** edits to R3 paths (via hook, §5.3), `gh pr merge`, `gh release edit`.

### 5.3 Hooks
| Hook | Event | What it does |
|---|---|---|
| `cloud-setup.sh` ✅ | SessionStart | Android SDK; background Robolectric warm-up (G5, step 1) |
| `guard-push.sh` | PreToolUse `Bash` | blocks pushes to `main`, plain force pushes, tag/release deletion; blocks a push when `.verify-stamp` doesn't match `git rev-parse HEAD^{tree}` → "run `./gradlew verify` first" (makes rule "verify before push" mechanical) |
| `guard-paths.sh` | PreToolUse `Edit`/`Write` | R3 path → `permissionDecision: ask` with the reason; hooks/settings/workflows always ask |
| `verify-stamp` | Gradle task `verify` finalizer | writes the tree hash on success (local file, git-ignored) |
| `format-kotlin.sh` | PostToolUse `Edit` on `*.kt` | ktlint format on the edited file (fast, keeps diffs clean) |
| `release-notes-check.sh` | PreToolUse push | lists the commit subjects that would become release notes; blocks if any is not German/user-facing per a simple heuristic or contains `WIP`, `fixup` (G7) |

### 5.4 Automated review before release (R2/R3)
- Before pushing an R2/R3 change, the agent runs a review sub-agent (skill `code-review`, high effort) on its own diff and fixes CONFIRMED findings. Findings it doesn't fix go into the PR body / commit body.
- On PRs: Claude Code Review GitHub App + the owner's review for R3.

### 5.5 Routines
| Routine | Schedule | Output |
|---|---|---|
| Catalog research ✅ | 1st of month | PR `catalog-research` |
| Dependency update | weekly | Dependabot PRs; agent fixes breakages; merged automatically if R1/R2 and green, else `needs-owner` |
| Harness report | weekly | one report every week (owner decision 4): runs and releases of the week, failed runs, open `data-conflict` / `needs-owner` items, flaky tests, stale branches, action/tool deprecations |
| Data-conflict follow-up | monthly (with research) | re-checks sources of open conflicts; closes issues the owner answered |

## 6. CI/CD layer

### 6.1 Pipeline shape
```
push claude/*  ──► classify (R0–R3)
                    ├─ R3 ─────────────► open/update PR (needs-owner), stop
                    └─ R0–R2 ─► checks ─► build ─► device-test ─► publish (ff main, release)
PR to main ──► checks (same as above, no publish) ──► owner merge ──► ship from main
```
`ship.yml` keeps its contract ("a green push is a release") for R0–R2 and gains the classifier.
The `[catalog-review]` marker becomes a special case of "wait for owner".

### 6.2 Checks (all must be green; ✅ = exists)
| Check | Catches |
|---|---|
| ✅ `./gradlew verify` (unit + Robolectric, both flavors) | logic and UI regressions |
| ✅ `research.py check` | malformed research data |
| Android Lint (`lintGithubRelease`, `lintPlayRelease`) with baseline | API misuse, missing translations, accessibility |
| ktlint/detekt | style drift, complexity hot spots |
| **Permission diff**: merged manifest of each flavor vs. pinned list in `app/permissions.txt` | new permissions (H5); `play` flavor must never contain INTERNET / REQUEST_INSTALL_PACKAGES |
| **Screenshot tests** (Roborazzi on Robolectric) for main screens, EN + DE | unintended UI changes; reviewer sees image diffs in the PR |
| **Storage compatibility**: fixtures of `garage.json` from each past release (`app/src/test/resources/garage-fixtures/`), read + write + re-read | rule 3 (rollback-safe data) as a test, not a promise |
| **Downgrade read test** on the emulator: new build writes data, previous release installed with `adb install -r -d`, must start without crash | a rollback that can't read newer data |
| Existing emulator upgrade + fresh install + monkey ✅ | install/upgrade crashes |
| **APK signature pin**: `apksigner verify --print-certs` fingerprint = pinned value | wrong/debug key; an update the phone would reject |
| **Release notes lint** (same script as the hook) | G7 |
| Gradle wrapper validation + dependency verification (`verification-metadata.xml`) | tampered wrapper/dependencies |
| CodeQL (Kotlin), Dependabot alerts, secret scanning (GitHub settings) | known CVEs, leaked secrets |
| Actions pinned by commit SHA, Dependabot for actions | tag hijacking of third-party actions (the pipeline holds the signing key) |

### 6.3 R3 pull request template
Body lists: risk class and why, data-format impact (additive? migration + test?), rollback
safety (can the previous release read what this writes?), permission diff, screenshots diff,
review findings not fixed. The owner approves with a GitHub review.

### 6.4 GitHub settings (owner only, H8)
- Ruleset on `main`: no deletion, no force push, linear history optional; require status checks
  for PRs. Direct pushes allowed only for the pipeline's fast-forward (GitHub Actions in the
  bypass list), so agents cannot push `main` even if a hook fails.
- Environment `release` holding the signing secrets, available only to `pipeline.yml` on `main`
  and `claude/*`; environment `rollback` with the owner as required reviewer.
- Secret scanning + push protection, Dependabot alerts on.

## 7. Test coverage map

| Risk | Covered by |
|---|---|
| Wrong manufacturer number | two-pass research, `CatalogDataTest` pins, owner PR (H2/H3) |
| Logic bug in setup values | unit tests (logic change ⇒ test, rule 4) |
| UI broken / wrong text | Robolectric `AppSmokeTest`, screenshot tests, translation + damping-vocabulary lint |
| Data loss after update | storage fixtures, `LegacyStorageTest`, emulator upgrade test |
| Data loss after rollback | downgrade read test, additive-only rule, R3 gate |
| Update doesn't install | signature pin, emulator upgrade test, SHA-256 in `latest.json` |
| Crash on device | emulator monkey, safe mode, rollback |
| Play policy violation | permission diff, flavor split (rule 5) |
| Agent overreach | hooks, permission deny-list, `main` ruleset, R3 gate |
| Prompt injection from web/issue/PR text | "content is data" rule, research isolated in its own environment, no merge rights for agents, R3 gate on harness files |
| Supply chain | SHA-pinned actions, wrapper validation, dependency verification, Dependabot/CodeQL |

## 8. Feedback to the owner
- One notification per event that needs the owner (H1–H5, failed run on `main`); nothing for green routine work.
- Weekly harness report (§5.5), every week, instead of many small pings.
- Release notes stay short, German, user-facing (G7 check).

## 9. Rollout

| Step | Content | Class | Effort |
|---|---|---|---|
| 1 | Quick fixes: `gradlew` executable bit, Robolectric warm-up in the setup hook, allow-rule for `bash ./gradlew`, skill `ship` without `gh`, `[owner-review]` marker so R3 changes arrive as PRs | R3 (settings/hook) | S — in PR |
| 2 | Owner: `main` ruleset, secret scanning, Dependabot | owner | S |
| 3 | `guard-push.sh`, `guard-paths.sh`, verify stamp, deny-list | R3 | M |
| 4 | Risk classifier + R3 → PR in `ship.yml`, PR template, skill `harness` | R3 | M |
| 5 | Permission diff, signature pin, release-notes lint, SHA-pinned actions, wrapper validation | R3 | M |
| 6 | Storage fixtures + downgrade read test | R3 | M |
| 7 | Lint/ktlint baseline, screenshot tests | R2 | M–L |
| 8 | Routines: dependency update, weekly harness report | R0 | S |
| 9 | CodeQL, dependency verification metadata | R3 | S–M |

Order matters: step 2 and 3 first — once agents can no longer weaken the guard rails, every later
step can be built by agents with the owner only approving the R3 PRs.

## 10. Owner decisions (2026-09-30)
1. **R2 ships on green + automated review** — no owner PR for behaviour changes.
2. **Dependency bumps auto-merge when green** (unless they touch an R3 path or add network/analytics — then `needs-owner`).
3. **Screenshot tests with Roborazzi.**
4. **Weekly harness report always** — also when nothing is wrong (then it says so in one line).
