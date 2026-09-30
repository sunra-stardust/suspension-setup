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

The weekly research routine (finding new models/model years, blind second pass, PR or `data-conflict` issue) is skill **`catalog-research`**.

## Record for every value

- The catalog is `app/src/main/resources/catalog/catalog.json`. Each document is one entry in `sources` (title, URL, **model years** it covers, retrieval date `YYYY-MM-DD`); each value is `{ "value": …, "source": "<id>", "note": "…" }`. Put the table row / column or footnote in `note`.
- `source: "owner"` = checked on the owner's bike, not a manufacturer document. `source: "legacy"` = entered before per-value sourcing and not yet traced — replace it with a real source when you verify the value, never add new `legacy` values.
- An entry's `modelYears` must be covered by the sources of its charts (`CatalogDataTest` enforces it).
- Bike-specific values still live in `data/BikeProfile.kt` with a code comment; README → "Woher die Zahlen kommen" stays the human-readable summary.

## Model years

Manufacturers change internals, click counts and charts between years. Every catalog entry names its model year(s). A new model year is a new entry (or an explicit year range), never a silent edit of the old one — riders with older parts keep correct data.

## Conflicts and uncertainty

Do not pick a side. Keep the current value, and open a GitHub issue labelled `data-conflict` with both quotes and links; tell the owner. Example (2026-09): Fox's 2025 DHX2 manual says Performance Elite has only LSC/LSR; the 2026 manual dropped that sentence but kept "Factory Series only" footnotes; the owner's actual shock has HSC + LSC + one rebound knob. Resolution came from the owner checking the bike — not from the documents.

## Checklist before pushing data changes

- [ ] Every new/changed number traced to a level-1/2 source for the right model year
- [ ] Adjusters present/absent verified (HSC? HSR? split rebound? lever?)
- [ ] Units right (psi vs bar, clicks counted **from closed**, lbs springs)
- [ ] `CatalogDataTest` pins the new values (and still passes for existing ones)
- [ ] README source table updated
- [ ] Commit subject says what changed for riders ("Fox 38 2027: Drucktabelle ergänzt")
