# Fahrwerk

Persönliche, offline Referenz-App für die Fahrwerkseinstellungen am Mountainbike (Level RR).
Einzelner Nutzer, ein Bike, keine Accounts, kein Netzwerk, kein Backend.

Native Android-App: Kotlin + Jetpack Compose (Material 3), `DataStore<Preferences>` für lokale
Persistenz. Web-Referenz: siehe Implementierungs-Spec (nicht Teil dieses Repos).

## Screens

- **Setup** — sieben Szenarien (Basis, Downhill, Bikepark, Tour, Uphill, Kalt, Warm), editierbare
  Stepper-Werte für Gabel, Dämpfer, Reifen, Rahmen. Fahrergewicht im Header editierbar; Gabel- und
  Dämpfer-Modell über die Gruppenüberschrift wählbar (siehe „Fahrergewicht & Komponenten" unten).
- **Diagnose** — Symptom-Suche mit aufklappbaren Handlungsempfehlungen.
- **Basics** — statische Referenzkarten (Klicks zählen, Reihenfolge, Sag messen, Zielwerte,
  Temperatur, Serie ab Werk) + dynamische Einstellbereiche-Karte (folgt der Gabel-/Dämpferwahl) +
  Reset auf Startwerte.

## Fahrergewicht & Komponenten

Gabel- und Dämpfer-Katalog mit vier bzw. drei recherchierten Modellen; „Eigenes Modell" erlaubt
freie Eingabe (Name, Federweg/Hub, Klick-Bereiche, Rebound-Modus). Auswahl + Fahrergewicht sind
globale Einstellungen (nicht pro Szenario), persistiert in einem eigenen `DataStore`.

**Wie sich Werte berechnen:**
- **Luftdruck (Gabel):** lineare Interpolation über die recherchierte Gewicht→Psi-Tabelle des
  gewählten Modells, plus dieselben Szenario-Offsets wie im Spec (§6), z. B. Bikepark = Basis +12.
  Eigene Modelle haben keine Tabelle → fester Basiswert, manuell nachjustieren.
- **Federrate (Dämpfer):** kein Hersteller veröffentlicht eine Gewicht→Federrate-Tabelle ohne
  Rahmen-Kennlinie (Übersetzungsverhältnis) — RockShox nennt das explizit als Grund. Daher eine
  Faustregel, kalibriert auf den bekannten Punkt aus Spec §6/§10 (97,5 kg → 550 lbs): ±5 lbs pro kg,
  gerundet auf 25-lbs-Schritte.
- **Klick-Kreise (LSC/HSC/LSR/HSR):** ändern sich NICHT mit dem Gewicht (Dämpfungscharakteristik,
  keine Federungs-Kompensation). Beim Modellwechsel werden die Szenario-Klicks proportional auf
  den neuen Klick-Bereich umgerechnet (z. B. 10 von 18 → 8 von 15).
- **Sag (mm):** bleibt der gemessene Wert aus dem Spec — wird nicht automatisch neu berechnet, das
  misst man selbst nach (siehe Basics → „Sag messen").

Modellwechsel löscht die gespeicherten Szenario-Overrides der betroffenen Komponente (die alten
Klicks passen sonst nicht zum neuen Bereich); das Fahrergewicht ändert dagegen nur die
*Defaults* — eigene Anpassungen bleiben erhalten (Persistenz-Prinzip aus Spec §8).

**Komponenten-Recherche (Quellen, Stand 2026):**
- Fox 38 / Fox 36 GRIP X2 Klick-Bereiche: [enduro-mtb.com Fox 38 Test](https://enduro-mtb.com/en/fox-38-factory-grip-x2-2027-test/), [Fox 36 Freehub-Review](https://freehub.com/reviews/fox-36-factory-grip-x2)
- RockShox ZEB/Lyrik/Pike Charger-3(.1) Klicks: [SRAM Charger 3 Setup](https://www.sram.com/en/rockshox/rockshox-technology/charger-3-setup), [RockShox FAQ](https://support.rockshox.com/hc/en-us/articles/5910471591579-What-is-the-range-of-adjustability-on-Charger-3)
- Fox DHX2 / Float X2 Klicks: [Fox Bike Tech Help Center](https://tech.ridefox.com/bike/owners-manuals/2928/shock--2024-all-coil-shocks-(dhx2-and-dhx-models))
- RockShox Super Deluxe / Vivid Coil Klicks: [BikeRadar Super Deluxe Review](https://www.bikeradar.com/reviews/components/rear-shocks/2023-rockshox-super-deluxe-coil-ultimate-review), [Blister Vivid Coil Review](https://blisterreview.com/gear-reviews/rockshox-vivid-coil)
- Gewicht→Psi-Tabellen (Fox 38/36, RockShox ZEB/Lyrik): aggregiert über [suspendmtb.com](https://suspendmtb.com/suspension-calculator)
- Dass es **keine** universelle Gewicht→Federrate-Tabelle gibt: explizit bestätigt von [roadmancycling.com](https://roadmancycling.com/tools/shock-pressure) unter Berufung auf RockShox

Startwerte, keine Herstellerangaben — wie der Rest der App (Spec §13).

## Toolchain

| | Version |
|---|---|
| Android SDK | API **36**, build-tools 36/37 |
| AGP | **9.2.0** (built-in Kotlin, KGP **2.2.10**) |
| Gradle | **9.4.1** (Wrapper eingecheckt) |
| Kotlin | **2.2.10** |
| Compose | BOM 2026.06.00 |

## Bauen

```powershell
.\gradlew.bat verify   # Unit-Tests + Debug-APK
.\gradlew.bat :app:assembleDebug
```

Debug-APK: `app/build/outputs/apk/debug/`.

## Release & Installation (Sideload, ohne Play Store)

```
git tag v0.1.0 && git push origin v0.1.0
```

`.github/workflows/release.yml` baut eine **signierte** Release-APK und hängt sie an ein
GitHub-Release. Auf dem Handy: Release-Seite öffnen, APK herunterladen, „Aus dieser Quelle
installieren" einmalig erlauben, installieren.

Kein In-App-Updater (App ist vollständig offline, keine `INTERNET`-Permission — Updates sind ein
manueller Re-Download. Damit ein Update über die bestehende Installation drüberinstalliert (statt
Deinstallieren + Neuinstallieren zu verlangen), muss jede Version mit demselben Schlüssel signiert
sein.

**Einmalig einzurichten:**
1. Release-Keystore erzeugen (**dauerhaft aufbewahren**):
   `keytool -genkeypair -v -keystore release.jks -alias suspensionsetup -keyalg RSA -keysize 2048 -validity 10000`
2. Repo-Secrets (Settings → Secrets and variables → Actions): `RELEASE_KEYSTORE_BASE64` (=
   `base64 release.jks`), `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

## Persistenz

Jeder editierte Wert wird sofort unter dem Schlüssel `"<paramId>:<szenarioIndex>"` in
`DataStore<Preferences>` gespeichert. Unveränderte Werte fallen auf die Standardwerte im Code
zurück — ein künftiges Ändern der Defaults wirkt sich also nur auf nicht editierte Werte aus.

## Lizenzen

Barlow und Barlow Condensed (SIL Open Font License) sind unter `licenses/` dokumentiert und als
Font-Dateien unter `app/src/main/res/font/` gebündelt.
