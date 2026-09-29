---
name: ui-text
description: Rules for any user-visible text — where strings live, languages, rotation vocabulary on damping controls, Diagnose entries, phone-first wording. Use when adding or changing UI text, hints, Diagnose entries or translations.
---

# UI text

## Where

- All user-visible text in `app/src/main/res/values/strings.xml` — nothing hardcoded in Kotlin. Model names (Fox 38, DHX2 …) are proper nouns and stay as data.
- Document placeholders in a comment above the string (`%1$s = version name`). Literal `%` in formatted strings is `%%`.
- Today German is the only language. Phase 2 (see `docs/ROADMAP.md`) makes English the default (`values/`) and German a translation (`values-de/`); from then on every string must exist in both, enforced by a test.

## Rotation vocabulary (safety — "Change 01")

A click counter counts **from closed**. On the dial, clockwise (zudrehen, ↻) means *more* damping and a *lower* number. Mixing this up with +/− once made riders turn the wrong way.

- Damping rows (LSC, HSC, LSR, HSR, single rebound) use ↺/↻ buttons with the captions *weicher / fester* (compression) and *schneller / langsamer* (rebound); units *zu / offen*.
- Quantities (pressure, spacers, sag, preload, spring rate, tyre pressure) use `+` / `−`.
- Banned in damping-row strings and the whole Diagnose list: "plus", "minus", "erhöhen", "verringern" — `DampingStringLintTest` enforces it.
- A Diagnose action saying "zudrehen" must have `RotationDirection.CLOCKWISE`, "aufdrehen" `COUNTER_CLOCKWISE` — `DiagnoseDirectionConsistencyTest` enforces it.
- Showing a deviation from the manufacturer on a damping row: say "2 Klicks weiter zu als Fox-Empfehlung", never "−2".

## Style

- Read on a phone, often outdoors with gloves: short, concrete, no jargon without explanation.
- Address the rider as "du".
- Hints say *when* to change something ("Wippen, Einsacken unter Motorlast"), not textbook definitions.
