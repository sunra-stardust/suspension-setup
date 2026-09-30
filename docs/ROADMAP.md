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
| 2 | **Languages:** English default (`values/`), German (`values-de/`), in-app language choice, test: every key in both languages | ✅ shipped |
| 3 | **Data model:** catalog as data files (model year, source, retrieval date per value); bikes + Vorlagen (create/edit/copy); manufacturer recommendation preselected + deviation display; JSON storage (backup-ready, additive/rollback-safe) with migration of today's values. Shipped in four steps: 3.1 catalog as data file ✅ · 3.2 JSON storage + migration ✅ · 3.3 bikes + Vorlagen UI ✅ · 3.4 deviation display ✅ | ✅ shipped |
| 4 | **Research routine:** cloud routine keeps a register of all MTB bike and suspension makers and collects their product/model data into `research/` (placeholders for gaps), second independent verification pass, conflicts → `data-conflict` issue; everything reaches `main` only through the owner's review PR. Runbook: skill `catalog-research` ✅ · data format + `scripts/research.py` ✅ · environment `Suspension-Setup-Routine` (Full network) ✅ · routine `Katalog-Recherche` (1st of the month + manual) ✅ · PR tests (`pr-check.yml`) ✅ · initial build run ⏳ | ▶ in progress |
| 5 | **Pick your bike:** bike catalog in `catalog.json` (maker → model → year → trim → size); picking a bike selects its stock fork/shock and spring; parts we don't have become prefilled own parts marked "check"; bike not listed → own bike with maker search in the component pickers; nothing in the catalog → manual entry as today. Steps: 5.1 bike catalog data + garage support ✅ · 5.2 bike picker with maker filter ✅ · 5.3 maker search in fork/shock pickers ✅ · 5.4 "check these values" hints on prefilled parts | ▶ in progress |
| later | Backup/restore · AI assistant · Google Play release (store listing, privacy policy, Play App Signing, AAB of the `play` flavor) | |

## Decisions

- **One long-lived branch (`main`).** Cloud sessions can only push their own `claude/*` branch; `ship.yml` fast-forwards it onto main after all checks and deletes it.
- **Every green push is a release.** Version `versionBase.commitCount`; release notes from commit subjects.
- **OTA from public GitHub Releases** (`latest.json` + APK + SHA-256), `github` flavor only — Google Play forbids self-updating apps, so the `play` flavor has no updater code or permissions.
- **Rollback = republish old app code as a new version** (Android refuses downgrades). Requires stored data to change additively only.
- **Safe mode** after 2 crashes within 10 min: update check without the normal UI.
- **Tests gate every release:** unit + Robolectric UI tests (both flavors), emulator upgrade test from the previous release, fresh-install test.
- **Languages:** English default, German translation; device language unless the rider picks one on Basics (stored in SharedPreferences, read synchronously at activity start). Release notes stay German commit subjects.
- **Vorlagen start at the pure manufacturer value** (owner decision 2026-09-30). The owner's former built-in offsets (Bikepark +12 psi …) are migrated as the owner's own changes and then show up as deviations. Values without a manufacturer figure (shock clicks, tire pressure, sag per Vorlage) show their deviation from the Vorlage's starting value, labelled as such ("2 Klicks weiter zu als Startwert").
- **The rider's changes are stored relative to the starting value** ("+12 psi", "−2 clicks"; owner decision 2026-09-30): weight and temperature move the manufacturer value, the rider's deviation stays. Toggles store the chosen option.
- **Storage = one JSON file** (`files/garage.json`: rider conditions, Vorlagen, bikes, edits). Kept as JSON in memory so fields written by a newer release survive saves by an older one. The pre-phase-3 DataStores (`app_settings`, `scenario_values`) stay: the first bike is mirrored into them after every change (absolute values, every row) so a rolled-back release shows the latest values, and if a rolled-back release changed them (fingerprint mismatch) they are imported again on the next start.
- **Bikes added in the app use a generic frame profile** (no flip chip, dropper, spring-rate rule or factory spec — nothing we could verify for an unknown frame); they start on the current bike's fork, shock and installed spring. Copying a bike keeps its frame profile and all values. The old-storage mirror follows the migrated bike (or the first bike once that one is deleted).
- **Vorlagen are shared by all bikes**; each bike holds its own values per Vorlage. A new Vorlage starts as a copy of an existing one on every bike.
- **Catalog is a data file** (`app/src/main/resources/catalog/catalog.json`, Java resource so app and JVM tests read the same file); every value names a source document with model year and retrieval date.
- **Manufacturer data only from primary sources for the right model year**; conflicts go to the owner, never guessed.
- **Research routine proposes, the owner merges** (owner decision 2026-09-30): verified data arrives as a PR against `main`, not as a direct ship. Its commits carry `[catalog-review]`, which stops `ship.yml` from shipping a `claude/*` push; the merge onto `main` ships as usual.
- **Research routine scope: all MTB bike and suspension makers** (owner decision 2026-09-30): one initial build of the complete register with placeholders, then a monthly update of every maker; the owner can trigger a run by hand. Runs in its own cloud environment `Suspension-Setup-Routine` with Full network access (the default one blocks the manufacturer sites) — acceptable because nothing it writes reaches `main` without review, and web content is treated as data.
- **Bikes are picked as sold** (owner decision 2026-09-30): maker → model → model year → trim → size; the stock parts depend on trim and year, spring rates on size. A stock part missing from the component catalog becomes the rider's own part, prefilled with what the bike maker publishes and marked for checking — never guessed click ranges presented as data.
- **Research data lives in `research/`, not in the app** (format `research/FORMAT.md`, checked and summarised by `scripts/research.py`). Only fork/shock entries whose needed fields are all verified are promoted into `catalog.json`; bike models whose stock parts are verified are promoted into `catalog.json` → `bikes` (phase 5).
- **Second pass is blind**: a sub-agent gets component, model year and field definitions but not the first pass's values or URL, and finds the document itself. Any disagreement → `data-conflict` issue.
- **Charts only when the brackets match**: the app's charts are Fox's 13 rider-weight rows; tables with other brackets are not converted or interpolated — the chart stays empty with a note.
