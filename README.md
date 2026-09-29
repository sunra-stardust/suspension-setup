# Fahrwerk

Offline-Referenz-App für die Fahrwerkseinstellungen am Mountainbike. Aktuell konfiguriert für
ein Mondraker **Level RR** (Fox 38 GRIP X2 · Fox DHX2 · OnOff Pija), aufgebaut so, dass weitere
Bikes und Komponenten reine Datenpflege sind.

Native Android-App: Kotlin + Jetpack Compose (Material 3), `DataStore<Preferences>` für lokale
Persistenz. Keine Accounts, kein Netzwerk, keine Berechtigungen.

## Screens

- **Setup** — fünf Gelände-Szenarien (Basis, Downhill, Bikepark, Tour, Uphill). Karte
  „Bedingungen": Fahrergewicht (mit Ausrüstung) und Fahrtemperatur (Basis 20 °C) gelten für alle
  Szenarien; der Header zeigt sie als Erinnerung. Gabel- und Dämpfer-Modell über die
  Gruppenüberschrift wählbar. Dämpfungskreise (LSC/HSC/LSR/HSR bzw. einzelne Zugstufe) nutzen
  ↺/↻-Drehrichtungs-Buttons, alle Mengenwerte `+`/`−` (siehe „Drehrichtung").
- **Diagnose** — Symptom-Suche mit Handlungsempfehlung und Drehrichtungs-Chip.
- **Basics** — Referenzkarten (Drehrichtung, Reihenfolge, Sag messen, Zielwerte, Temperatur,
  Druckstufe, Serie ab Werk, Einstellbereiche) + Reset auf Startwerte. Zielwerte und
  Einstellbereiche folgen dem gewählten Gabel-/Dämpfermodell.

## Woher die Zahlen kommen

| Wert | Quelle |
|---|---|
| Gabel-Luftdruck (Basis) | Fox-Drucktabelle (FLOAT-Spalte) für die Gewichtszeile des Fahrers, genau wie auf dem Casting-Aufkleber |
| Gabel-Zugstufe LSR/HSR (Basis) | Fox-Zugstufentabelle 36/38 GRIP X2 nach Fahrergewicht |
| Gabel LSC 10 / HSC 5 (Basis) | Fox-Startempfehlung „Klicks ab zu" |
| Spacer, Maximaldruck | Fox-Handbuch (38/180 mm: 1 ab Werk, max. 4, max. 140 psi) |
| Szenario-Abweichungen (Bikepark +12 psi …) | Eigene Abstimmung des Besitzers (Spec §6), als Offsets auf den Herstellerwert |
| Federrate | Verbaute Feder (ab Werk 500 lbs, Gr. L/XL). Empfehlung im Hinweis per Faustregel, kalibriert auf diesen Rahmen (97,5 kg → 550 lbs, ±5 lbs/kg) — für Federraten gibt es keine Herstellertabelle, sie hängt vom Übersetzungsverhältnis des Rahmens ab |
| Dämpfer-Klickwerte, Sag-Ziele | Eigene Startwerte des Besitzers (Fox' DHX2-Tabellen hängen vom Rahmen ab) |
| Temperatur, Luft (Gabel, Reifen) | Gasgesetz auf den **absoluten** Druck bei konstantem Volumen. Angezeigt wird der **Fülldruck bei 20 °C**, mit dem am Rad bei Fahrtemperatur der Zielwert (Fox-Tabelle / deine Reifenwerte) erreicht wird — z. B. Gabel 110 psi Ziel bei 0 °C → 119 psi bei 20 °C füllen. Wer draußen pumpt, nimmt den Zielwert (steht im Hinweis). Fox' Tabellen gelten für 21–24 °C |
| Temperatur, Öl (alle Klickkreise) | Kaltes Öl dämpft stärker → Klicks weiter auf, warm → weiter zu. Kalibriert an den früheren eigenen Kalt-/Warm-Setups (≈ −15 K: LSC +2, übrige +1; +10 K: LSC −1), linear, max. ±3 Klicks, skaliert auf kurze Dials (RockShox: 5 Klicks). Deckt sich mit veröffentlichten Empfehlungen: unter ca. 5 °C Druckstufe 1–3 Klicks öffnen, Zugstufe 1–2 schneller |

Alles sind Startwerte. Werksempfehlung der eigenen Einheit: 4-stellige ID auf ridefox.com.

**Quellen (Stand 2026):**
- [Fox 36/38 Handbuch 2025](https://tech.ridefox.com/bike/owners-manuals/2979/fork--2025-36mm-or-38mm) — Drucktabellen, Zugstufentabelle, Spacer, Maximaldruck, Klick-Anzahlen, Sag 15–20 %
- [Fox Coil-Dämpfer 2025 (DHX2)](https://tech.ridefox.com/bike/owners-manuals/2981/shock--2025-all-coil-shocks-(dhx2-and-dhx-models)) — Factory vs. Performance Elite, Vorspannung (8 Klicks ab spielfrei, max. 2 Umdrehungen), Sag ~30 %
- [Fox DHX2 2026](https://tech.ridefox.com/bike/owners-manuals/3090/shock--2026-dhx2)
- [Mondraker Level RR](https://mondraker.com/us/en/level-rr1750250055) — DHX2 Performance Elite 205×65, Federraten je Größe, OnOff Pija
- [Blister: Mondraker Level](https://blisterreview.com/gear-reviews/mondraker-level) — Flip-Chip: −5 mm Tretlager, −0,35°
- [OnOff Pija Anleitung](https://www.onoffcomponents.com/uploads/maintances/pija-seatpost-20211130093117-es.pdf) — 280–300 psi, nie über 300, alle 10 h prüfen
- [SRAM Charger 3 Setup](https://www.sram.com/en/rockshox/learn/charger-3-setup), [Super Deluxe Coil Ultimate](https://www.sram.com/en/rockshox/models/rs-sdlc-ult-b1), [Vivid Coil Ultimate](https://www.sram.com/en/rockshox/models/rs-vivc-ult-c1) — RockShox-Klicks und Hebel
- [Pinkbike: Temperatur und Fahrwerk](https://www.pinkbike.com/news/nerding-out-how-temperature-affects-your-suspension.html), [NSMB: Cold Weather Suspension](https://nsmb.com/articles/cold-weather-mountain-bike-suspension/), [Singletracks: Cold-Weather Tuning](https://www.singletracks.com/mtb-gear/tuning-your-mountain-bike-fork-and-shock-for-cold-weather-riding/) — Öl-Viskosität, Klick-Richtung, Druck bei Fahrtemperatur prüfen

RockShox-**Gabeln** sind bewusst nicht im Katalog: RockShox veröffentlicht keine allgemeine
Drucktabelle, und die Werte auf dem Casting unterscheiden sich je Modelljahr/Federweg. Solche
Gabeln laufen über „Eigenes Modell" mit dem Startdruck vom Aufkleber.

## Drehrichtung (Change 01 — safety fix)

Der Klick-Zähler zählt vom geschlossenen Anschlag aus offen. `+` auf dem Bildschirm hieß früher
„mehr Klicks" (weniger Dämpfung), am Gabel-Dial bedeutet `+` aber mehr Dämpfung. Deshalb gilt eine
strikte Trennung: **Rotation** (↺/↻) nur für Dämpfungskreise, **Quantity** (`+`/`−`) für alles
andere. `↻` (zudrehen) senkt die Zahl, `↺` (aufdrehen) erhöht sie.

## Erweitern

- **Neues Gabel-/Dämpfermodell:** ein Eintrag in `data/ComponentCatalog.kt`. Nur verifizierte
  Herstellerdaten eintragen; was fehlt, bleibt `null` — die App zeigt dann neutrale Hinweise statt
  erfundener Werte. `ScenarioDataTest` sichert die Werte ab.
- **Neues Bike:** ein `BikeProfile` in `data/BikeProfile.kt` (Serienteile, Feder, Faustregel,
  Flip-Chip, Sattelstütze, „Serie ab Werk"-Texte) und `BikeProfiles.current` darauf zeigen lassen.
  Die übrige App kennt kein Bike beim Namen.
- **Texte:** alles in `res/values/strings.xml`. Eine englische Übersetzung wäre ein
  `values-en/strings.xml` — nötig, bevor die App öffentlich wird.

## Toolchain

| | Version |
|---|---|
| Android SDK | API **36** |
| AGP | **9.2.0** (built-in Kotlin, KGP **2.2.10**) |
| Gradle | **9.4.1** (Wrapper eingecheckt) |
| Compose | BOM 2026.06.00 |

## Bauen & Testen

```powershell
.\scripts\verify.ps1   # Unit-Tests + Debug-APK (nutzt Android Studios JBR)
```

## Release & Installation (Sideload)

```
git tag v0.5.0 && git push origin v0.5.0
```

`.github/workflows/release.yml` baut eine signierte APK und hängt sie an ein GitHub-Release.
Alle Versionen sind mit demselben Schlüssel signiert, Updates installieren sich also über die
bestehende App. Kein In-App-Updater (keine `INTERNET`-Permission).

## Persistenz

Jeder editierte Wert wird unter `"<paramId>:<szenarioIndex>"` gespeichert. Nicht editierte Werte
folgen den Standardwerten — ändert sich die Herstellertabelle, das Gewicht, die Temperatur oder
das Modell, ziehen unveränderte Werte automatisch nach; **selbst verstellte Werte bleiben fest**.
Ein Modellwechsel löscht die Overrides dieser Komponente.

Migration: Die früheren Szenarien „Kalt"/„Warm" (Index 5/6) gibt es nicht mehr — Temperatur ist
eine globale Einstellung. Ihre gespeicherten Overrides werden beim Start entfernt
(`ValueRepository.dropScenarioIndicesFrom`); die Indizes 0–4 bleiben unverändert.

## Lizenzen

Barlow und Barlow Condensed (SIL Open Font License), siehe `licenses/`.
