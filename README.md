# Fahrwerk

Persönliche, offline Referenz-App für die Fahrwerkseinstellungen am Mountainbike (Level RR).
Einzelner Nutzer, ein Bike, keine Accounts, kein Netzwerk, kein Backend.

Native Android-App: Kotlin + Jetpack Compose (Material 3), `DataStore<Preferences>` für lokale
Persistenz. Web-Referenz: siehe Implementierungs-Spec (nicht Teil dieses Repos).

## Screens

- **Setup** — sieben Szenarien (Basis, Downhill, Bikepark, Tour, Uphill, Kalt, Warm), editierbare
  Stepper-Werte für Gabel, Dämpfer, Reifen, Rahmen.
- **Diagnose** — Symptom-Suche mit aufklappbaren Handlungsempfehlungen.
- **Basics** — statische Referenzkarten (Klicks zählen, Reihenfolge, Sag messen, Zielwerte,
  Temperatur, Serie ab Werk, Einstellbereiche) + Reset auf Startwerte.

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
