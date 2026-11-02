# NewMe

Private, offline Android habit tracking for separate installations. No accounts,
analytics, synchronization, automatic upload, or remote map service. Each phone
keeps its own records. The application ID remains `com.aistudio.newme.trkfit`.
Current source version: **2.0, version code 3, Room schema 7**.

## Training Arc

Today uses Ember, up to five due quests, real goal/XP progress and illustrated
quick logs. Routines have daily, selected-weekday, weekly and clamped monthly
recurrence; a new installation has no sample quests. Starter packs and meal
ideas are opt-in. Adding a routine pack enables its Routines module without
changing unrelated module choices. Routine/reminder form writes are atomic.
Settings opens from the profile control; enabled modules
determine which of the five tabs are visible.

Moments are optional illustrated morning/midday/evening questions, at most one
per eligible open. Later and Skip are always available; opening/skipping earns
no XP. Partial answers persist locally, and a completed check-in pays its
existing reward once. Night belongs to the preceding evening until 04:00.
The 24-badge collection keeps existing IDs/unlocks, supports three pins, and
uses local summaries rather than loading every historical day's records after
each write. Artwork and poster replacements are documented in [docs/ART.md](docs/ART.md).

## Setup And Builds

- Use JDK 21. Gradle/Android compilation supports JDK 17, but the API-36
  Robolectric tests require 21. The wrapper uses Gradle 9.3.1 with a pinned SHA-256.
- Install Android SDK platform 36.1, build tools 36.0.0, and platform-tools.
  Set `JAVA_HOME` and `ANDROID_HOME`, or set the SDK path through Android Studio.
- The Windows VS Code tasks use `%LOCALAPPDATA%/NewMeTools/jdk-21*` and
  `%LOCALAPPDATA%/Android/Sdk`. Adjust those tasks for other installations.
- This Windows environment uses OS certificate trust through
  `JAVA_TOOL_OPTIONS=-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE`.
  Do not disable TLS or checksum verification. Dependency downloads need a
  network connection initially; the installed app does not have Internet access.

From the repository root in PowerShell:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain --no-configuration-cache
```

VS Code task **NewMe: Verify Handoff** runs those clean validation gates.
Unit/Robolectric tests cover synthetic data only, including Android 24/25 and
34/36 behavior. Debug builds use normal debug signing, not the release key.
Test and lint reports are generated under `app/build/reports`.
If Windows cannot delete a locked lint-cache JAR during `clean`, run
`.\gradlew.bat --stop` and retry the clean command. Do not clear app data.

## Privacy And Backups

Room records, cycle history, and optional GPS points stay in app-private storage.
The live database is **not password-encrypted**; protect the phone with a screen
lock. Automatic cloud backup and device transfer are disabled in the manifest
and both backup rule files. This cannot remove copies previously created by
another version, a user, or a compromised device. Bundled illustrations and
local companion messages do not require a server. Voice uses available offline
TTS voices only. There is no working Health Connect or friend synchronization.

Use System settings to export an encrypted backup to device storage or Downloads.
Cloud document providers are rejected. Choose a password of at least 12
characters and keep it separately; a forgotten password cannot be recovered.
Archives use AES-256-GCM with authenticated headers, fresh salt/nonce, and
PBKDF2-HMAC-SHA256 (600,000 iterations). Maximum plaintext size is 64 MiB.

Restore authenticates the archive first, reconstructs a private temporary
database from a bundled schema, applies explicit Room migrations, and checks
constraints before offering replacement. Schema versions **4 through 7** are
supported. Unknown versions/hashes, damaged archives, wrong passwords, failed
writes, and cancellation must not replace live records. Confirmation replaces
all user tables atomically. Temporary files are removed after preparation and
interrupted staging is cleaned at the next launch. Active sessions restore
paused, and private cycle reminders require fresh opt-in after every restore.

Reset deletes all user tables and cancels reminders. Recalibration keeps earned
XP and historical records. Disabling cycle tracking hides history without
deleting it; re-enabling reveals it. Cycle dates and predictions are approximate,
not fertility, contraception, pregnancy, or medical guidance.

## Reminders And Permissions

Reminders are off by default and explicitly opt-in during setup or Settings.
Settings offers task notification controls,
a recurring daily check-in, quiet hours, and independently opt-in private cycle
reminders. Routine editors reuse the same scheduling/permission rules.
Checklist bell controls in Details configure one reminder for that saved task
and its recorded date. Completing or deleting the task suppresses its reminder.
Cycle notifications and their channel use generic private text, never period
dates or symptoms. Predictions without sufficient reliable history do not
produce a cycle reminder.

Approximate alarms are the default. A task reminder marked time-sensitive uses
exact timing only when Android grants exact-alarm access; otherwise the UI
reports approximate fallback. Notification permission is requested only after
enabling a reminder or starting tracking. Denial does not block ordinary logging.
Android notification/channel settings can independently block delivery.

Schedules reconcile after database edits, reboot, clock/time-zone changes, app
updates, reset, restore, and returning from Android settings. Delivered tokens
prevent repeat delivery of the same occurrence. Times follow the local zone.
Quiet hours defer delivery; a dated task is suppressed rather than moved into
another day. Doze, battery restrictions, and manufacturer policies can delay
delivery. Force-stop and exact-access revocation can stop alarms until NewMe is
opened again. These Android restrictions are not a guaranteed reminder service.

Activity recognition and precise location are requested only for user-started
tracking where relevant. GPS is optional for walking, running, and cycling.
There is no passive location tracking or background-location permission.

## Activity And Progress

Walking, running/jogging, cycling, dancing, and swimming are explicitly started,
paused, resumed, and stopped. Missing sensors or denied permissions leave the
timer/manual path available. Swimming is timer/manual use; do not put an
unsuitable phone in water. Existing gym and manual records remain supported.

Tracked history can correct distance on the **same finished session**. Daily
summaries keep their identities, time, steps, rewards, and routes. GPS proportions
are retained when available; otherwise distance is allocated by saved duration.
A new manual activity is a separate record, not a correction to a tracked one.
Route preview uses locally stored points, without map tiles. Deleting route
points does not delete the activity. Interruptions preserve confirmed time and
pause the session rather than inventing elapsed time or steps.

Daily goals are user-selected. Historical goal/target snapshots remain stable;
earned XP is awarded once and is not removed by undo or missed days. A missed
day can reset the current streak, not the accumulated XP.

## Migrations And Signing

Keep [schemas 4](app/schemas/com.example.data.local.NewMeDatabase/4.json),
[5](app/schemas/com.example.data.local.NewMeDatabase/5.json), and
[6](app/schemas/com.example.data.local.NewMeDatabase/6.json) immutable once
distributed. Every schema change needs a new version, an explicit migration in
[DatabaseMigrations](app/src/main/java/com/example/data/local/DatabaseMigrations.kt),
and an exported schema. Register the same migration list for live upgrades and
staged backup restore. Never introduce destructive fallback or drop user rows.

Use one durable release keystore **outside Git**, with a securely retained copy
and separately secured passwords. Do not generate a replacement for an installed
app unless the original certificate is deliberately being abandoned. Configure
`KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` securely in the local build
environment; `KEY_ALIAS` defaults to `newme`. Do not put passwords in commands,
repository files, chat, or logs. Release builds reject missing signing inputs or
a keystore inside the repository. Task **NewMe: Check Release Readiness** reports
only readiness flags and attached device status, not secret values.

When the existing release key is available:

```powershell
.\gradlew.bat :app:assembleRelease --console=plain --no-configuration-cache
& "$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat" verify --verbose --print-certs .\app\build\outputs\apk\release\app-release.apk
```

Record the APK version, SHA-256 file hash, and public certificate SHA-256
fingerprint. Increment `versionCode` for every distributed update. Compare the
certificate with the previously installed build, then use `adb install -r` for
the authorized test device. **Do not uninstall to bypass a signature mismatch.**
No release is published automatically.

## Manual Release Gates

- On Android 24/25 and a current physical phone, test a same-certificate in-place
  upgrade using synthetic profile, water, task, cycle, and route data.
- Export/restore locally; cancel each picker and confirmation, try wrong
  passwords/corruption, and verify records remain intact on every failure.
- Test notification/channel denial and revocation, exact-access fallback,
  quiet hours, reboot/time-zone changes, completed/deleted tasks, reset/restore,
  and discreet cycle notifications on a locked screen.
- Test sensor absence/baselines, GPS denial/loss, pause/resume, midnight, reboot,
  process interruption, and distance corrections with no duplicate activity.
  Verify an active session with the screen off on a suitable phone.
- Review light/dark/system appearance, reduced motion, TalkBack, largest system
  fonts, keyboard/dialog sizing, and the original bundled illustrations.

Robolectric and screenshots do not establish physical sensor, screen-off,
manufacturer scheduling, signed-upgrade, or real-device accessibility behavior.
Those gates remain manual until checked on authorized hardware.

## Verified Phase 2 Build

- Clean unit/Robolectric test run: **72 passed**, zero failures/errors/skips
  (33 existing database/activity/reminder, 23 unit, 5 journey database,
  5 journey workflow, and 6 component/theme screenshot tests).
- `lintDebug` passed with warnings and no errors; `assembleDebug` passed.
- Light/dark screenshots were recorded with enlarged text; reduced-motion,
  module navigation, due filtering, saved state and seven-step setup checks passed.
- Schema 6-to-7 migration preserved synthetic rows in every released table;
  older encrypted backup restoration and the 2,000-day query-count fixture passed.
- Merged manifest retains the application ID, disables automatic backup,
  and contains no Internet or background-location permission.
- The version-2.0/code-3 debug APK passed `apksigner verify` with APK Signature Scheme
  v2 and the standard **Android Debug** certificate. Local artifact:
  [app-debug.apk](app/build/outputs/apk/debug/app-debug.apk).
- APK size: **23,081,013 bytes**, within the Phase 2 growth budget.
- APK SHA-256: `D2C76502B26188DD692E4BEBBDA773E8EF2E2EA4ED050710CBD7FD01D02EC328`.
- Public debug certificate SHA-256:
  `60ee3aa6667bd7bf6e808c5275201f79482bb5eeea06e72acc38c776320df819`.
- **Not release-verified:** release signing inputs/keystore were unavailable,
  and `adb devices` reported no attached device. No signed release, public
  release-certificate fingerprint, physical-phone run, or in-place signed
  upgrade was produced or claimed. Nothing was published.

## Lint Triage

Remaining warnings are intentionally recorded, not suppressed wholesale:

| Category | Count | Disposition |
| --- | ---: | --- |
| GradleDependency / NewerVersionAvailable / AGP version | 37 | Retain the tested toolchain; dependency upgrades need their own compatibility run. |
| UnusedResources | 9 | Retain bundled font licence, original legacy artwork and legacy resources; no personal data or remote assets. |
| UseKtx | 7 | Existing API-style suggestions, not correctness failures; defer unrelated mechanical changes. |
| IconDipSize | 2 | Existing launcher sizing; review on the manual device matrix. |
| RedundantLabel | 1 | Existing explicit activity label; harmless and consistent with application branding. |
| DiscouragedApi | 1 | Intentional cached local poster-slot lookup for optional user-supplied raster replacements. |

New context-retention and plural warnings were fixed. Original sensor, screen-off,
TalkBack/largest-font device checks and same-key signed upgrades remain manual.