# TV Launcher (Android TV / Google TV)

Voll funktionsfähiger Home-Launcher für Android TV, Google TV, Fire TV und Tablets.

## Funktionen

- Registriert sich als HOME-App, auswählbar als Standard-Launcher
- "Als Standard-Launcher festlegen" (Rollen-Dialog ab Android 10, sonst Home-Einstellungen)
- App-Grid mit D-Pad-Fokus, Fokus-Animation, TV-Banner (Leanback) oder Icon
- Favoriten-Reihe (lang drücken: zu Favoriten hinzufügen/entfernen)
- Kontextmenü per langem Druck auf OK: Öffnen, Favorit, App-Info, Ausblenden, Deinstallieren
- Ausgeblendete Apps verwalten, Spaltenanzahl (4–8), 6 Hintergrundverläufe
- Uhr und Datum, Direktzugriff auf WLAN und Systemeinstellungen
- Aktualisiert sich automatisch bei Installation, Update und Deinstallation von Apps
- Menü-Taste der Fernbedienung öffnet die Launcher-Einstellungen
- Zurück-Taste beendet den Launcher nicht

## APK bauen

### Variante A: GitHub Actions (kein Android Studio nötig)

1. Projekt in ein GitHub-Repo pushen (Branch `main`).
2. Tab *Actions*, Workflow *Build Release APK* läuft automatisch.
3. Fertige `TVLauncher-release.apk` unter *Artifacts* herunterladen.
4. Für Updates mit gleicher Signatur eigene Secrets setzen:
   `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
   Ohne Secrets erzeugt der Workflow pro Lauf einen neuen Key.
   Ein Tag `v1.0.0` hängt die APK zusätzlich an ein GitHub-Release.

### Variante B: Lokal

Voraussetzung: JDK 17 und Android SDK (Android Studio bringt beides mit).

```
./gradlew assembleRelease
```

Ergebnis: `app/build/outputs/apk/release/app-release.apk`

Eigener Key (optional):

```
keytool -genkeypair -v -keystore release.jks -alias mykey -keyalg RSA -keysize 2048 -validity 10000
KEYSTORE_PATH=release.jks KEYSTORE_PASSWORD=... KEY_ALIAS=mykey KEY_PASSWORD=... ./gradlew assembleRelease
```

Ohne diese Variablen wird die Release-APK mit dem Debug-Key signiert (installierbar).

## Installation auf dem TV

```
adb connect <TV-IP>:5555
adb install -r TVLauncher-release.apk
adb shell am start -n com.btx.tvlauncher/.MainActivity
```

Danach in der App unter *Einstellungen* auf *Als Standard-Launcher festlegen* gehen.

Hinweis: Manche Google-TV-Geräte lassen keinen anderen Home-Launcher zu.
Dann hilft per ADB (Gerät mit Entwickleroptionen):

```
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
```

Rückgängig: `adb shell pm enable com.google.android.apps.tv.launcherx`
(Paketname je nach Gerät, z. B. `com.google.android.tvlauncher` bei Android TV.)
