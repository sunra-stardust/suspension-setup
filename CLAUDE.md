# Fahrwerk — agent instructions

Android app (Kotlin, Jetpack Compose) for ambitious mountain bikers: understand, set and save
suspension settings. Manufacturer recommendations per model year are preselected; the rider's
own changes are shown as deviations. Everything is stored on the device. Owner speaks German —
answer in German. Vision, phases and decisions: [`docs/ROADMAP.md`](docs/ROADMAP.md).

## A push is a release

Every push to `main` or `claude/*` runs `.github/workflows/ship.yml`: tests → signed APK →
emulator test (upgrade from the last release + fresh install) → GitHub Release → the owner's
phone offers the update. `claude/*` branches are fast-forwarded onto `main` and deleted.
So:

- `./gradlew verify` must be green **before** you push (unit + Robolectric UI tests, both flavors, debug builds).
- Cloud sessions push their `claude/<topic>` branch (the only branch they may push); locally, push `main` or a `claude/` branch.
- After pushing, check the run (`gh run list --workflow ship.yml --limit 3`) and report the outcome with the version number.
- Commit subjects become the release notes shown in the app: write them as short German change notes ("Dämpfer: HSC-Regler ergänzt").
- Pipeline details, failed runs, rollback: skill **`ship`**.
- Exception: a `claude/*` push whose head commit contains `[catalog-review]` does not ship — the weekly catalog research routine (skill **`catalog-research`**) proposes data as a PR against `main`; the owner's merge ships it.

## Rules

1. **Manufacturer data** (pressures, clicks, charts, adjusters, model years) only from verified primary sources — skill **`catalog-data`**. A wrong number is worse than a missing one.
2. **UI text** lives in `res/values/strings.xml` (English, default) and `res/values-de/strings.xml` (German) — every string in both; damping controls use rotation vocabulary, never +/− — skill **`ui-text`**.
3. **Stored data is rollback-safe:** only add keys/fields; never rename or delete stored keys without a migration and a test (an older release must still read what a newer one wrote). `garage.json` changes go through `GarageDoc` (edits the JSON in place, so unknown fields survive); keep the legacy mirror working (`LegacyStorageTest`, `GarageRepositoryTest`).
4. **Logic change ⇒ unit test. New screen or flow ⇒ extend `AppSmokeTest`.** Reproduce a bug as a failing test first.
5. **Two flavors:** `github` (sideload, self-updater, INTERNET + REQUEST_INSTALL_PACKAGES) and `play` (no self-update code or permissions — Google Play policy). Flavor-specific code goes in `src/github` / `src/play` behind `update/Distribution`.
6. No new permissions, network use, analytics or accounts unless the owner asks.
7. Never force-push `main`, delete tags/releases, or touch the signing secrets.

## Commands

| | |
|---|---|
| Verify (Linux/cloud) | `./gradlew verify` |
| Verify (Windows) | `.\scripts\verify.ps1` (sets Android Studio's JBR 21; the Bash tool is broken on this machine — use PowerShell) |
| Single test class | `./gradlew :app:testGithubDebugUnitTest --tests '*ScenarioDataTest'` |
| Ship status | `gh run list --workflow ship.yml --limit 3` |
| Roll back | `gh workflow run rollback.yml -f version=<x.y.z>` (details: skill `ship`) |

Cloud sessions install the Android SDK via the SessionStart hook (`scripts/cloud-setup.sh`); it needs `dl.google.com` in the environment's allowed domains. Without it, rely on CI.

## Code map (`app/src/main/java/dev/suspension/app/`)

- `data/` — catalog (`ComponentCatalog`, data in `src/main/resources/catalog/catalog.json`), bike profile, scenario rows and defaults, temperature model, storage (`GarageRepository` → `files/garage.json`; `LegacyStorage`/`LegacyStores` import from and mirror to the pre-phase-3 DataStores).
- `ui/` — Compose screens (Setup with Vorlage tabs, Diagnose, Basics, component pickers, `BikesOverlay`), dialogs in `components/Dialogs.kt`, `UpdateUi` (banner, app card, safe mode), `components/`, `theme/`.
- `update/` — OTA logic shared by both flavors (`Release`, `UpdateViewModel`); `src/github/…/update/GitHubUpdater` does the network + install.
- `safety/CrashGuard` — crash-loop detection → safe-mode screen with update check.
- Tests: `app/src/test/…` (JUnit 5 + Robolectric/JUnit 4 via the vintage engine). Device test: `scripts/device-smoke.sh`.
