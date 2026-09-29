---
name: ui-text
description: Rules for any user-visible text — where strings live, languages, rotation vocabulary on damping controls, Diagnose entries, phone-first wording. Use when adding or changing UI text, hints, Diagnose entries or translations.
---

# UI text

## Where

- All user-visible text in string resources — nothing hardcoded in Kotlin (accessibility labels included). Model names (Fox 38, DHX2 …) are proper nouns and stay as data.
- **English is the default** (`app/src/main/res/values/strings.xml`), **German the translation** (`values-de/strings.xml`). Every new string goes into both — `TranslationCompletenessTest` fails otherwise (missing keys, extra keys, differing placeholders).
- Text that is identical in every language (units like psi/mm, LSC/HSC, model names, "Fahrwerk") is `translatable="false"` and lives only in `values/`.
- Document placeholders in a comment above the English string (`%1$s = version name`). Literal `%` in formatted strings is `%%`.
- Decimal numbers follow the UI language (German `1,75`, English `1.75`): format with `formatStepValue(…, currentLocale())`; from data code pass `TextSpec.Decimal` as a format argument, never a pre-formatted string.
- Stored values never depend on the language. If a stored value is shown as text (e.g. climb switch "Offen"/"Firm"), map it to a string resource for display (`RowSpec.Toggle.optionLabels`) and keep the stored value unchanged.
- Language choice: device language by default; Basics → Language overrides it (`LanguageStore`, applied in `MainActivity.attachBaseContext`).

## Rotation vocabulary (safety — "Change 01")

A click counter counts **from closed**. On the dial, clockwise (zudrehen / close, ↻) means *more* damping and a *lower* number. Mixing this up with +/− once made riders turn the wrong way.

- Damping rows (LSC, HSC, LSR, HSR, single rebound) use ↺/↻ buttons with the captions *weicher / fester* — *softer / firmer* (compression) and *schneller / langsamer* — *faster / slower* (rebound); units *zu / offen* — *closed / open*.
- Quantities (pressure, spacers, sag, preload, spring rate, tire pressure) use `+` / `−`.
- Banned in damping-row strings and the whole Diagnose list, in every language: "plus", "minus", "erhöhen", "verringern", "increase", "decrease" — `DampingStringLintTest` enforces it.
- A Diagnose action saying "zudrehen" / "Close" must have `RotationDirection.CLOCKWISE`, "aufdrehen" / "Open" `COUNTER_CLOCKWISE` — `DiagnoseDirectionConsistencyTest` enforces it per language. A new language needs its verbs added there.
- Showing a deviation from the manufacturer on a damping row: say "2 Klicks weiter zu als Fox-Empfehlung" / "2 clicks more closed than Fox recommends", never "−2".

## Style

- Read on a phone, often outdoors with gloves: short, concrete, no jargon without explanation.
- German addresses the rider as "du"; English as "you".
- English: American spelling (tire, behavior).
- Hints say *when* to change something ("Wippen, Einsacken unter Motorlast" / "Bobbing, sagging under motor load"), not textbook definitions.
