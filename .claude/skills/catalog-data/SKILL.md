---
name: catalog-data
description: Rules for researching and entering manufacturer suspension data — forks, shocks, model years, pressure/rebound charts, adjuster click ranges, bike OEM specs. Use when adding or updating a component, checking a value, answering "is this number right?", or running the product-research routine.
---

# Manufacturer data

The app preselects the manufacturer's recommendation and shows the rider how far they deviate from it. A wrong number misleads every rider who trusts it — **a missing value is better than a guessed one.**

## Sources — in this order

1. **Manufacturer's own manual / tech center for that model year** (e.g. `tech.ridefox.com` owner's manual, SRAM/RockShox service & setup pages). Charts, click counts, max pressure, which adjusters exist.
2. **Bike maker's spec sheet or setup guide** for OEM parts and frame-specific values (e.g. Mondraker's per-model suspension setup PDF: installed shock variant, spring rates per size).
3. Never as evidence: retailer listings, forums, reviews, AI or search-engine summaries. Use them only as leads to find a level-1/2 source.

Read the primary document itself (download the PDF/HTML and search the text). Search summaries were wrong twice in this project's history.

## Record for every value

- Source URL + document title + **model year** the document covers.
- The exact quote or table row the value comes from, and the retrieval date.
- Today these go in a code comment next to the value (`data/ComponentCatalog.kt`, `data/BikeProfile.kt`) and in README → "Woher die Zahlen kommen". Phase 3 moves the catalog to data files with dedicated `source` fields (see `docs/ROADMAP.md`).

## Model years

Manufacturers change internals, click counts and charts between years. Every catalog entry names its model year(s). A new model year is a new entry (or an explicit year range), never a silent edit of the old one — riders with older parts keep correct data.

## Conflicts and uncertainty

Do not pick a side. Keep the current value, and open a GitHub issue labelled `data-conflict` with both quotes and links; tell the owner. Example (2026-09): Fox's 2025 DHX2 manual says Performance Elite has only LSC/LSR; the 2026 manual dropped that sentence but kept "Factory Series only" footnotes; the owner's actual shock has HSC + LSC + one rebound knob. Resolution came from the owner checking the bike — not from the documents.

## Checklist before pushing data changes

- [ ] Every new/changed number traced to a level-1/2 source for the right model year
- [ ] Adjusters present/absent verified (HSC? HSR? split rebound? lever?)
- [ ] Units right (psi vs bar, clicks counted **from closed**, lbs springs)
- [ ] `ScenarioDataTest` pins the new values (and still passes for existing ones)
- [ ] README source table updated
- [ ] Commit subject says what changed for riders ("Fox 38 2027: Drucktabelle ergänzt")
