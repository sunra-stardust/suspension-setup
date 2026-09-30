# Research data format

`research/` is the catalog research routine's working data (skill `catalog-research`). The app
does **not** read it — nothing here ships. Verified entries that fit the app's catalog schema are
promoted into `app/src/main/resources/catalog/catalog.json` in the same review PR.

```
research/
  FORMAT.md            this file
  makers.json          register of all manufacturers (bike + suspension)
  suspension/<id>.json forks and shocks of one suspension maker
  bikes/<id>.json      bike models of one bike maker
  README.md            generated overview — never edit by hand (scripts/research.py overview)
  reviews/<date>.md    generated review of one run's changes (scripts/research.py review)
```

Check and regenerate: `python3 scripts/research.py check && python3 scripts/research.py overview`.

## `makers.json`

```json
{ "makers": [
  { "id": "fox", "name": "Fox", "kinds": ["suspension"], "website": "https://www.ridefox.com",
    "docHub": "https://tech.ridefox.com/bike/owners-manuals", "status": "researched",
    "lastChecked": "2026-10-02", "notes": "" } ] }
```

- `id`: lowercase, `a-z0-9-`; also the file name under `suspension/` / `bikes/`.
- `kinds`: `suspension` and/or `bike`.
- `status`: `seed` (only the name is known) · `listed` (website + document hub found, product list pending) · `researched` (product list done; values may still be missing) · `inactive` (brand gone or no longer makes MTB suspension/bikes — keep it, say why in `notes`).
- `website`, `docHub`: the maker's own domains only.

## Product / model files

```json
{ "maker": "fox",
  "products": [
    { "id": "fox-38-factory-grip-x2-2025", "name": "Fox 38 Factory GRIP X2", "type": "fork",
      "modelYears": [2025], "catalogId": "fox38_gripx2",
      "fields": { "travelMm": { …value… }, "pressureChart": { …value… } } } ] }
```

Bike files use `"models"` instead of `"products"`, without `type`/`catalogId`.
`catalogId` links a product to its `catalog.json` entry once promoted (else `null`).

## Value object — every field

| `status` | Meaning | Required |
|---|---|---|
| `missing` | Placeholder: not found yet | `value: null`; `note` may say where it was looked for |
| `single` | First pass found it; not independently confirmed | `value`, `url`, `retrieved`, `quote` |
| `verified` | Blind second pass found the same value | as `single` + `verifiedBy: { url, retrieved, quote }` |
| `conflict` | Passes or documents disagree | `candidates: [{ value, url, quote }, …]` (≥ 2), `value: null`, `issue` (URL of the `data-conflict` issue) |

```json
{ "value": 140, "status": "verified", "url": "https://tech.ridefox.com/…", "retrieved": "2026-10-02",
  "quote": "Maximum air pressure: 140 psi", "where": "p. 12, table 3",
  "verifiedBy": { "url": "https://tech.ridefox.com/…", "retrieved": "2026-10-02", "quote": "140 psi max" } }
```

Only `verified` values may be promoted into `catalog.json`. Units and conventions are the
catalog's (skill `catalog-data`): psi, mm, lbs, clicks counted from closed; `null` + `verified`
for an adjuster that provably does not exist.

## Fields

Suggested, not exhaustive — add a field when a document offers something useful, in camelCase
with the unit in the name.

**Fork:** `travelMm`, `wheelSize`, `stanchionMm`, `damper`, `springType` (air/coil), `lscMax`,
`hscMax`, `reboundMode` (single/split), `reboundMax`, `hsrMax`, `lscStart`, `hscStart`,
`pressureChart` (13 Fox brackets 120–250 lb only — see skill `catalog-research`),
`pressureTable` (the maker's own table as printed: `{ "unit": "psi", "rows": [{ "rider": "54–59 kg", "value": 72 }] }`),
`maxPressurePsi`, `spacersStock`, `spacersMax`, `sagPercent`, `manualUrl`.

**Shock:** `eyeToEyeMm`, `strokeMm`, `mount`, `springType`, `lscMax`, `hscMax`, `reboundMode`,
`reboundMax`, `hsrMax`, `hasClimbLever`, `preloadGuide`, `sagPercent`, `manualUrl`.

**Bike model:** `rearTravelMm`, `forkTravelMm`, `wheelSize`, `sizes`, `shockEyeToEyeMm`,
`shockStrokeMm`, `shockMount`, `stockForkBySpec` / `stockShockBySpec` (per build kit),
`springRateBySize` (coil), `sagPercent`, `setupGuideUrl`, `flipChip`.
