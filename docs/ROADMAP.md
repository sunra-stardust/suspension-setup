# Roadmap

## Vision

Android app for ambitious mountain bikers. It helps them understand suspension settings and keep
them. A rider has one or more **bikes**; each bike has components (fork, shock …) with an explicit
**model year**. For every riding situation (**Vorlage**: Basis, Downhill, Bikepark, Tour, Uphill,
plus the rider's own, e.g. "Nasse Wurzeln") the app holds a full set of values.

- The manufacturer's recommendation for the selected model and model year is preselected.
- When the rider changes a value, the app shows that — and by how much — it deviates from the manufacturer ("+6 psi", "2 Klicks weiter zu als Fox-Empfehlung").
- Built-in Vorlagen can be adapted; new Vorlagen and bikes can be created.
- Everything is stored on the device. Later: backup/restore.
- Later: an AI assistant that receives the current settings and advises when the rider wants to change how the bike rides.
- A scheduled cloud routine keeps the component catalog current (new products and model years, manufacturer defaults) — under the strict source rules in `.claude/skills/catalog-data/SKILL.md`.
- Later available on Google Play; English + German.

## Phases

| # | Content | Status |
|---|---|---|
| 1 | **Infrastructure:** agent setup (`CLAUDE.md`, skills), push = release pipeline, OTA updater, rollback workflow, crash-loop safe mode, Robolectric UI + emulator upgrade tests, `github`/`play` flavors | ✅ shipped |
| 2 | **Languages:** English default (`values/`), German (`values-de/`), in-app language choice, test: every key in both languages | ▶ next |
| 3 | **Data model:** catalog as data files (model year, source, retrieval date per value); bikes + Vorlagen (create/edit/copy); manufacturer recommendation preselected + deviation display; JSON storage (backup-ready, additive/rollback-safe) with migration of today's values | |
| 4 | **Research routine:** weekly cloud routine — finds new models/model years, reads manufacturer documents, second independent verification pass, conflicts → `data-conflict` issue instead of shipping | |
| later | Backup/restore · AI assistant · Google Play release (store listing, privacy policy, Play App Signing, AAB of the `play` flavor) | |

## Decisions

- **One long-lived branch (`main`).** Cloud sessions can only push their own `claude/*` branch; `ship.yml` fast-forwards it onto main after all checks and deletes it.
- **Every green push is a release.** Version `versionBase.commitCount`; release notes from commit subjects.
- **OTA from public GitHub Releases** (`latest.json` + APK + SHA-256), `github` flavor only — Google Play forbids self-updating apps, so the `play` flavor has no updater code or permissions.
- **Rollback = republish old app code as a new version** (Android refuses downgrades). Requires stored data to change additively only.
- **Safe mode** after 2 crashes within 10 min: update check without the normal UI.
- **Tests gate every release:** unit + Robolectric UI tests (both flavors), emulator upgrade test from the previous release, fresh-install test.
- **Manufacturer data only from primary sources for the right model year**; conflicts go to the owner, never guessed.
