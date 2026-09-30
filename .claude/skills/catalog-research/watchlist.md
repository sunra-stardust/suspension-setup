# Research watchlist

Tier B of the catalog research routine (`SKILL.md`). One group per run, chosen by
`ISO week number mod 4`. Families already in `catalog.json` are tier A and checked every run.
Edit this list to change the scope — the routine reads it on every run.

Hosts are where the primary documents usually live; they must be allowed in the routine
environment's network settings. Retailers, forums and reviews are never sources (`catalog-data`).

| Group (week mod 4) | Maker | Families | Primary-source hosts |
|---|---|---|---|
| 0 | Fox | forks 34, 36, 38, 40 (GRIP, GRIP X, GRIP X2); shocks Float, Float X, Float X2, DHX, DHX2 | `tech.ridefox.com`, `www.ridefox.com` |
| 1 | RockShox | forks Pike, Lyrik, ZEB, Boxxer; shocks Super Deluxe (air/coil), Vivid (air/coil) | `www.sram.com`, `trailhead.sram.com`, `docs.sram.com` |
| 2 | Öhlins | forks RXF 36/38; shocks TTX2 Air, TTX22M | `www.ohlins.com` |
| 2 | Marzocchi | forks Z1, Bomber Z1 Coil, Bomber 58; shock Bomber CR | `www.marzocchi.com`, `tech.ridefox.com` |
| 3 | EXT | forks Era, Era V2; shocks Storia, Arma, Aria | `www.extremeshox.com` |
| 3 | Cane Creek | forks Helm; shocks Kitsuma, DB Coil | `www.canecreek.com` |
| 3 | DVO | forks Onyx, Diamond; shocks Topaz, Jade | `dvosuspension.com` |
| 3 | Formula | forks Selva, Belva | `www.rideformula.com` |
| 3 | Manitou | forks Mezzer, Mattoc, Dorado; shock Mara | `www.hayesbicycle.com`, `manitoumtb.com` |

Bike makers (OEM spec sheets, level 2) only for bikes in `data/BikeProfile.kt`:

| Maker | Documents | Hosts |
|---|---|---|
| Mondraker | per-model suspension setup guide (PDF) | `mondraker.com` |

Most non-Fox makers publish air-pressure charts in brackets that do not match the app's 13 Fox
rows, or only per-fork decals. Expect tier-B entries to land with adjuster data (clicks, which
knobs exist, travel, max pressure) and `null` charts — that is correct, not a failed run.
