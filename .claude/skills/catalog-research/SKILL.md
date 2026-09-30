---
name: catalog-research
description: Runbook for the weekly catalog research routine (roadmap phase 4) — find new fork/shock models and model years, read the manufacturer documents, verify every value in a second blind pass, then open a review PR against main or a data-conflict issue. Use when the routine fires, when running it by hand ("Katalog-Recherche starten"), or when changing how it works.
---

# Catalog research routine

Runs every Friday morning as a cloud routine in its own environment (the default environment's
network policy blocks the manufacturer sites). Goal: the catalog stays current **without a single
unverified number reaching a rider**. All data rules come from skill **`catalog-data`** — read it
first; this skill only adds the procedure.

Outcome of a run, one of:

| Found | Result |
|---|---|
| Nothing new, nothing changed | No PR, no issue. Final message: "Keine Änderungen" + what was checked. |
| Values that passed both passes | **One PR against `main`** (never a direct ship — owner decision 2026-09-30) |
| Values where the passes disagree, or documents contradict each other | One `data-conflict` issue per component; the value stays out of the PR |
| Document unreachable / unreadable | Mentioned in the final message; nothing guessed |

## 0. Setup

1. Make sure `sunra-stardust/suspension-setup` is checked out (fresh routine sessions may start empty: `add_repo` with push access, clone, work on the session's designated branch).
2. Check reachability of the source hosts in [`watchlist.md`](watchlist.md) (`curl -sI`). If the manufacturer hosts are blocked, stop and report that — do not fall back to retailer pages or search snippets.
3. Look for open work from earlier runs: open PRs whose body contains `catalog-research`, open issues labelled `data-conflict`. Don't duplicate them; if a previous PR is still open, add new findings to a new PR only if they don't touch the same entries.

## 1. Scope for this run

- **Tier A (every run):** every family that is already in `catalog.json` — look for a new model year, a new revision of the manual (changed charts, clicks, adjusters) and fill `legacy` / `owner` values if a manufacturer document now covers them.
- **Tier B (rotating):** one group from [`watchlist.md`](watchlist.md), chosen by `ISO week number mod 4` (`date +%V`). The full list is covered every four weeks. Adding the whole list each week would cost four times the tokens and find the same nothing — model years change once a year.

Per family: find the manufacturer's document hub (owner's manual, tech center, setup guide), list the model years it covers, compare with the catalog.

## 2. First pass — extract

For each candidate (new model year, new entry, or changed value):

1. Download the primary document itself (PDF/HTML) and search its text. Search summaries only point you to documents; they are never evidence.
2. Extract into a working file (scratchpad, not the repo) per field: value, unit, page/table/row, **verbatim quote**.
3. Fields and units are the catalog's (see `catalog-data`, `CatalogJson.kt`): clicks counted **from closed**, psi, lbs, mm; `null` where the adjuster does not exist.
4. **Weight charts:** the app's charts have exactly 13 rows = Fox's rider-weight brackets (120–130 lb … 240–250 lb, `WeightBrackets.kt`). Enter a chart only if the manufacturer's table maps 1:1 onto these brackets. Other brackets (kg steps, 15-lb steps, "rider weight ± sag") → chart `null` and a `note` saying what the document offers. **Never interpolate or convert a chart.**

## 3. Second pass — blind verification

Spawn a sub-agent (Agent tool, `general-purpose`) **without the first pass's values**. Give it only:

- component name, maker, model year
- the list of fields with unit definitions from step 2.3/2.4
- the instruction to find the manufacturer's document itself and to answer with value + quote + URL per field, or "not in document"

Do not pass the first pass's URL or numbers — the point is that it can go wrong in a different way.

Then compare field by field:

| Result | Action |
|---|---|
| Same value, level-1/2 source on both sides | Accepted |
| Different value or different adjuster set | Re-read both quoted passages. Still different → `data-conflict` issue, value not entered |
| One side "not in document" | Not entered; mention in the final message |
| Both found, but in different documents that disagree | `data-conflict` issue (the documents contradict each other; the owner decides) |

## 4. Write the change

Only accepted values, following `catalog-data`:

- New model year = new entry or explicit year range; never overwrite the old year's values.
- New source in `sources` with title, URL, `modelYears`, `retrieved` = today.
- `note` = table/row/footnote of the quote.
- Pin every new/changed value in `CatalogDataTest` (the fork/shock list assertions compare the whole catalog — new entries must be added there).
- README → "Woher die Zahlen kommen": add the source.
- New UI text (e.g. a new preload hint) → skill `ui-text`, both languages.
- `./gradlew verify` must be green.

**Commit:** German subject as a rider-facing change note ("RockShox Lyrik 2027: Drucktabelle ergänzt"). The commit body **must end with the line `[catalog-review]`** — on a `claude/*` branch, `ship.yml` skips such pushes, so nothing ships before the owner merged the PR. Put the marker on every commit of the run.

## 5. Report

- **PR against `main`** from the session's branch. Title: `Katalog-Recherche KW<week>: <summary>`. Body per entry: field · value · quote · URL · page/row, plus "Pass 2: same value from <URL>". Mention `catalog-research` in the body (used by step 0.3). No auto-merge. The owner merges → push to `main` → normal ship.
- **`data-conflict` issue** per conflicting component: both quotes with links, what the catalog currently says, what the owner could check on the bike. Label `data-conflict`.
- **Final message** (becomes the routine's notification): what was checked (tier A + tier B group), PR/issue links, unreachable documents. German, short.

## Guardrails

- A missing value is better than a guessed one. When in doubt, leave it out and report it.
- Never touch `data/BikeProfile.kt` values or `owner` values without a document that covers exactly that part — the owner's bike was checked by hand.
- No new permissions, network code or app features — this routine changes data, tests and docs only.
- Never push `main`, never merge your own PR.
