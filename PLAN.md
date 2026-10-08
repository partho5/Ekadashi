# Vaishnava Vrata — Complete Plan & Context

> Status: **Plan only, not implemented.** Only the empty Gradle scaffolding (see §12) exists in the repo.
> Last updated: 2026-10-06

---

## 1. Intent

A **very small, open-source Android app** that tells Vaishnava devotees about upcoming vratas (Ekadashi, Janmashtami, Shiva Ratri, …) and reminds them about the vratas and their **parana** (fast-breaking) windows.

- **Data:** one public JSON file hosted on GitHub. It is the only source. All times in it are **IST (Asia/Kolkata)**.
- **No** backend, server, database, login or account.
- **Setup:** the user picks a country and a language on a single custom-designed setup screen.
- **Display:** dates and times are shown in the user's timezone, and notifications fire in it too.
- **UX goal:** as little friction as possible. **Build goal:** the smallest possible release APK.

---

## 2. Original specification (from the user)

### 2.1 Data source
- A public GitHub JSON URL is the only primary data source. The JSON lives in this repo (`data/vratas.json`). The user will supply real data later.
- Source data uses **Indian Standard Time (Asia/Kolkata)**.

```json
{
  "vratas": [
    {
      "type": "ekadashi",
      "date": "2026-10-13",
      "parana": { "date": "2026-10-14", "start": "06:12", "end": "09:24" }
    },
    { "type": "janmashtami", "date": "2026-08-28", "parana": null }
  ]
}
```

`parana` is either `null` or an object `{date, start, end}`. `end` may be `null`.

### 2.2 Configuration
```json
{ "language": "en", "timezone": "Asia/Dhaka" }
```
Supported languages: `en`, `bn`, `hi`. The language is a plain string value, as is usual in industry.

### 2.3 Timezone
- Source data is always read as Asia/Kolkata.
- The device timezone is detected, and the user confirms the result on first launch. *(The original wording was a timezone list. It was refined to a country pick, see §3.)*
- The confirmed timezone is stored in config. Everything displayed, and every notification, uses the configured timezone.

### 2.4 Vrata names
The JSON holds only a stable `type` id (`ekadashi`, `janmashtami`, `shiva_ratri`). The UI keeps translated labels, for example `en: Ekadashi`, `bn: একাদশী`, `hi: एकादशी`.

### 2.5 Notifications
- For every vrata: one reminder 2 days before and one reminder 1 day before.
- Parana: one reminder on the vrata day at 8 PM, but only if the parana has a date and time.

### 2.6 Architecture
```
GitHub JSON → Data Parser → Vrata Model → Timezone Conversion → Localized UI → Notification Scheduler
```

---

## 3. Decisions confirmed during clarification

| # | Topic | Decision |
|---|---|---|
| D1 | Tech stack | **Kotlin, framework APIs only.** No Compose, AppCompat, Material, RecyclerView, WorkManager, OkHttp, kotlinx-serialization or coroutines. Smallest possible production APK. |
| D2 | Reminder times | 2 days before → **08:00**. 1 day before → **20:00**. Parana → **20:00 on the vrata day**. All in the configured timezone. |
| D3 | Vrata `date` handling | **Kept exactly as published.** A date with no time is not a moment in time, so it is never converted. Only parana `date + start/end` are converted from IST to the configured timezone, and the parana date may shift. |
| D4 | Setup UX | Instead of a technical timezone list, the user picks a **Country** and a **Language** on **one custom-designed screen** (not a stock dialog). All inferred values are **preselected**. Full names are shown, never codes. |
| D5 | Country list | **বাংলাদেশ (Bangladesh)**, **भारत (India)**, **Other**. |
| D6 | Country → timezone | BD → `Asia/Dhaka`. IN → `Asia/Kolkata`. Other → the device's timezone (shown read-only as "auto"). |
| D7 | Language | Detected from the device and preselected. Shown as **English / বাংলা / हिन्दी**, never `en/bn/hi`. Chosen on the same screen as the country. |
| D8 | Changing later | A **⚙ icon** on the home screen reopens the same setup screen with the current values. There is no separate Settings screen. |

---

## 4. Gaps found and how they are resolved

| Gap | Resolution |
|---|---|
| When is a parana "with date time"? | Notify only if `parana != null && parana.start != null`. If `end == null`, the text reads "after 06:42". |
| What counts as "upcoming"? | `vrata.date >= today` in the configured timezone. |
| Offline / first launch with no network | A last-good copy of the JSON is saved as a file in app storage. `data/vratas.json` is also **bundled in the APK** as a seed, so the app always opens with data. (This is a cached file, not a database.) |
| Reboot wipes alarms | Reschedule on `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`. |
| Device timezone changes (travel) | If country = Other, update the stored timezone to the new device timezone and reschedule. BD and IN keep their fixed timezone. |
| Malformed JSON pushed to GitHub | The parser skips bad entries instead of crashing. A fetch that yields zero valid entries never overwrites the cache. |
| `type` the UI doesn't know yet | Fall back to a title-cased id (`shiva_ratri` → "Shiva Ratri"), so a new type needs no app update. |
| Android alarm limit (~500 per app) | Schedule a rolling **60-day** window and refresh it daily. |
| Notification permission (Android 13+) | Ask once, 3 seconds after the home screen first appears. If it is not granted, the home screen shows a banner and the settings screen shows a row; tapping either asks again, or opens the system notification settings once Android stops showing the dialog. |
| GitHub repo URL / package name | **Placeholders**: `OWNER/REPO` and `com.jovoc.ekadashi`. They live in one constant each and will be replaced when the user provides them. |

---

## 5. Architecture

```
            ┌──────────────── GitHub raw: data/vratas.json (IST) ────────────────┐
            │                     (ETag / If-None-Match)                          │
            ▼                                                                     │
  Repo ── cache file (filesDir) ── fallback: bundled asset (same data/vratas.json)│
            │                                                                     │
            ▼                                                                     │
  Parser (org.json, tolerant) → List<Vrata>                                       │
            │                                                                     │
            ▼                                                                     │
  TZ conversion (java.time: IST → configured zone, parana only)                   │
            │                                                                     │
     ┌──────┴────────────┐                                                        │
     ▼                   ▼                                                        │
  Localized UI      Reminders (pure fn) → Scheduler (AlarmManager, inexact)       │
  (MainActivity)          │                                                       │
                          ▼                                                       │
                AlarmReceiver → Notification                                      │
                                                                                  │
  RefreshJob (JobScheduler, 24h, network) ────────────────────────────────────────┘
  SystemReceiver (BOOT / PACKAGE_REPLACED / TIMEZONE_CHANGED) → reschedule
```

### 5.1 Files (planned)
```
data/vratas.json                 public data file (sample now → real data later); bundled as asset
settings.gradle.kts, build.gradle.kts, gradle.properties, gradlew, gradle/wrapper (Gradle 8.14.3)
app/build.gradle.kts
app/proguard-rules.pro
app/src/main/AndroidManifest.xml
app/src/main/java/com/jovoc/ekadashi/
  Vrata.kt        model + parser + IST→zone conversion
  Config.kt       Country enum, SharedPreferences config, device inference
  Repo.kt         cache / asset / GitHub fetch with ETag
  Reminders.kt    pure: (vratas, zone, now) → List<Reminder>     ← unit tested
  Scheduler.kt    AlarmManager schedule/cancel, channel, JobScheduler registration
  Receivers.kt    AlarmReceiver, SystemReceiver
  RefreshJob.kt   JobService: fetch + reschedule
  MainActivity.kt single framework Activity: SetupView ↔ HomeView
  L10n.kt         locale-wrapped Context, formatters, vrata label lookup
app/src/main/res/
  layout/         setup.xml, home.xml, item_vrata.xml
  drawable/       card_bg, card_selected, pill_bg, pill_selected, btn_primary, ic_gear (vector)
  values{,-bn,-hi}/strings.xml   UI strings + vrata_<type> labels
  values{,-night}/colors.xml, themes.xml
  raw/keep.xml    keeps vrata_* strings (looked up by name) from resource shrinking
app/src/test/.../ParserTest.kt, RemindersTest.kt
```

---

## 6. Component details

### 6.1 Data contract (`data/vratas.json`)
- Root object with a `vratas` array. Unknown fields are ignored, so future additions are safe.
- `type`: required, lowercase snake_case id.
- `date`: required, `YYYY-MM-DD` (IST calendar date, shown unchanged).
- `parana`: `null` or `{ "date": "YYYY-MM-DD", "start": "HH:mm" | null, "end": "HH:mm" | null }`, all in IST.
- An entry with an invalid `type` or `date` is skipped. An invalid parana object is treated as `null`.

### 6.2 Parser / model (`Vrata.kt`)
```kotlin
data class Parana(val date: LocalDate, val start: LocalTime?, val end: LocalTime?)
data class Vrata(val type: String, val date: LocalDate, val parana: Parana?)
val IST: ZoneId = ZoneId.of("Asia/Kolkata")
fun Parana.startIn(z: ZoneId) = start?.let { ZonedDateTime.of(date, it, IST).withZoneSameInstant(z) }
fun Parana.endIn(z: ZoneId)   = end?.let   { ZonedDateTime.of(date, it, IST).withZoneSameInstant(z) }
```
The parser uses `org.json`, which ships in the Android framework, so it costs 0 KB.

### 6.3 Config (`Config.kt`)
- SharedPreferences keys: `country`, `language`, `timezone`, `setupDone`, `etag`, `scheduledIds`.
- Country is inferred, first match wins:
  1. `TelephonyManager.networkCountryIso`
  2. `simCountryIso`
  3. `Locale.getDefault().country`
  4. device timezone (`Asia/Dhaka` → BD; `Asia/Kolkata`/`Asia/Calcutta` → IN)
  5. otherwise Other
- Language is inferred: the device language if it is en, bn or hi; otherwise the country default (BD → bn, IN → hi, Other → en).
- Timezone comes from the country (D6) and is **stored**, as the spec requires.

### 6.4 Repo (`Repo.kt`)
- `load()`: `filesDir/vratas.json`, falling back to `assets/vratas.json`.
- `refresh()`: runs on a background thread with `HttpURLConnection`, a 10 s timeout and `If-None-Match: <etag>`.
  - 304 → nothing to do.
  - 200 → parse. If there are 1 or more valid entries, write a temp file, rename it into place and store the new ETag.
  - Any error → keep the existing data and fail silently.
- URL: `https://raw.githubusercontent.com/OWNER/REPO/main/data/vratas.json` (placeholder).

### 6.5 Reminders (`Reminders.kt`, pure and testable)
For each vrata with `date >= today(zone)` inside the next 60 days:

| Kind | When (configured zone) | Condition |
|---|---|---|
| `TWO_DAY` | `date − 2 days` at **08:00** | always |
| `ONE_DAY` | `date − 1 day` at **20:00** | always |
| `PARANA`  | `date` at **20:00** | `parana?.start != null` |

Times already in the past are dropped. The stable id is `hash("$type|$date|$kind")`.

### 6.6 Scheduler (`Scheduler.kt`)
- Cancels the stored ids, then calls `AlarmManager.setAndAllowWhileIdle(RTC_WAKEUP, millis, pi)` for each reminder, then stores the new ids.
- **Inexact alarms**, so no `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` permission and no trip to system settings.
- Runs on:
  - app open
  - setup save
  - each refresh
  - boot
  - package replaced
  - device timezone change
- Daily refresh: a periodic `JobScheduler` job (24 h, `NETWORK_TYPE_ANY`, persisted).

### 6.7 Notifications (`Receivers.kt`)
- Channel: "Vrata reminders" (default importance).
- Text is built with a locale-wrapped context. Examples:
  - TWO_DAY: "Ekadashi in 2 days — Tue, 13 Oct" / "একাদশী ২ দিন পর — মঙ্গল, ১৩ অক্টো"
  - ONE_DAY: "Ekadashi tomorrow — Tue, 13 Oct"
  - PARANA: "Parana tomorrow 06:42–09:54" or "Parana tomorrow after 06:42". The date is shown when it isn't the next day.
- Tapping a notification opens MainActivity.

### 6.8 UI (`MainActivity.kt`, framework widgets with custom drawables)
**Visual language & layout guide:**
- Visual design reference: [app_UI_plan.jpeg](file:///home/haku/projects/AndroidStudioProjects/Vaishnaba-Vrata/app_UI_plan.jpeg)
- warm saffron accent `#E07A10`
- cream background (light) or deep brown-black (dark), following the system theme
- 16 dp rounded cards, generous spacing
- edge-to-edge with manual inset handling (targetSdk 36)

![App UI Guide](file:///home/haku/projects/AndroidStudioProjects/Vaishnaba-Vrata/app_UI_plan.jpeg)

**SetupView** (first launch and ⚙):
```
  ┌─────────────────────────────────┐
  │  বৈষ্ণব ব্রত / Vaishnava Vrata   │
  │  Never miss an Ekadashi         │
  │                                 │
  │  Country                        │
  │  ┌───────────┐┌──────────┐┌────┐│
  │  │ বাংলাদেশ ✓ ││  भारत     ││Other││  ← large selectable cards
  │  └───────────┘└──────────┘└────┘│
  │  (Other → "Timezone: Europe/London · UTC+1 · auto")
  │                                 │
  │  Language                       │
  │  ( English ) ( বাংলা ✓ ) ( हिन्दी ) │  ← pills; switching re-renders live
  │                                 │
  │  [          Continue          ] │
  └─────────────────────────────────┘
```
On Continue:
1. Save the config.
2. Request `POST_NOTIFICATIONS` (API 33+).
3. Start the refresh, schedule the reminders, register the daily job.
4. Show Home.

**HomeView:**
- Header: app name and a ⚙ icon.
- Banner when notifications are off; tapping it opens notification settings.
- **Hero card** for the next vrata: label, localized date, "today / tomorrow / in N days", parana window (converted).
- A list of the remaining upcoming vratas (ScrollView + LinearLayout; the data is small).
- Footer note: "Dates follow the Indian (IST) calendar; parana times are converted to your timezone."
- On resume: render from cache right away, refresh in the background, re-render if the data changed.

### 6.9 Localization (`L10n.kt`)
- `attachBaseContext` wraps the configured locale (works on all API levels; receivers use the same helper).
- Strings live in `values/`, `values-bn/`, `values-hi/`.
- Dates use `DateTimeFormatter` with the locale and `DecimalStyle.of(locale)`, so Bengali digits appear in bn.
- Vrata label: `getIdentifier("vrata_$type", "string")`, falling back to a title-cased id. `raw/keep.xml` stops R8 resource shrinking from removing these strings.

---

## 7. Build configuration (size first)
- Versions: AGP **8.11.0**, Kotlin **2.2.10**, Gradle **8.14.3**, JDK 17 (all present locally).
- SDKs: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` (native `java.time` and notification channels, no desugaring).
- Release build:
  - `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt`
  - turn off `buildConfig`, `aidl`, `renderscript`, `resValues`
  - `dependenciesInfo { includeInApk = false; includeInBundle = false }`
  - exclude `kotlin/**`, `META-INF/*.kotlin_module`, `META-INF/*.version`, `DebugProbesKt.bin` from packaging
  - `androidResources.localeFilters += listOf("en", "bn", "hi")`
- Dependencies: only the Kotlin stdlib (implicit). Tests use `junit:4.13.2` and `org.json:json` (the framework `org.json` is stubbed in JVM tests). Neither ships in the APK.
- `sourceSets["main"].assets.srcDir("../data")` makes one source for the published and the bundled JSON.
- Release signing uses the debug key for local testing. The production keystore is supplied by the maintainer.
- **Target release APK: < 200 KB.**

---

## 8. Alternatives considered

| Area | Alternatives | Chosen | Why |
|---|---|---|---|
| Stack | Jetpack Compose; Flutter; Kotlin Multiplatform; PWA | **Kotlin + framework views** | Compose adds ~1–2 MB, Flutter ~5 MB+, and PWA notifications are unreliable. Native views are the smallest and the most reliable for alarms. |
| Data hosting | Bundle in APK only; jsDelivr CDN; GitHub Pages; own API | **GitHub raw + cache + bundled seed** | APK-only data goes stale until an app update. jsDelivr caches for up to 7 days. An own API breaks the no-backend rule. Raw updates within ~5 minutes, and the cache plus seed means the app always opens. |
| Scheduling | Exact alarms; WorkManager; schedule everything at once | **Inexact AlarmManager, 60-day rolling window + daily JobScheduler** | Exact alarms need a permission that Android 14 denies by default (a settings detour). WorkManager adds size and can be delayed by hours. Scheduling everything at once hits the 500-alarm limit. |
| Timezone UX | Full timezone list (spec v1); silent auto-detect | **Country + language pick, preselected** | Timezones are jargon. A country is something people know. Silent detection gives no chance to correct mistakes. |
| Parana accuracy | Compute parana per location from sunrise | **Convert the published IST times** | Per-location computation is heavy and overrides the publisher's authority. Out of scope for a minimal app. |
| Vrata date | Shift the date via IST midnight | **Keep as published** | Shifting moves dates a day early west of India, which is usually wrong. |
| Settings | Separate Settings screen | **Reuse the setup screen via ⚙** | Less code, smaller APK, and one consistent place. |
| Storage | Room/SQLite; DataStore | **SharedPreferences + one JSON file** | The dataset is tiny, and these add no dependencies. |

**Most frictionless combination (chosen):** a one-screen setup with everything preselected (often a single tap on Continue), no special permissions beyond notifications, data available offline from the first launch, and silent background updates.

---

## 9. Side effects & limitations

| # | Side effect | Conflicts with goal? | Mitigation |
|---|---|---|---|
| S1 | **Converted IST parana is not the true local parana.** Parana depends on local sunrise. For Dhaka, 06:12–09:24 IST shows as 06:42–09:54, but the real Dhaka window differs. Far from India, the Ekadashi **date itself** can differ from the local panchang. | Limits **accuracy** for users outside India, not UX. | A visible footer note. Long term: per-region data files (e.g. `vratas.bd.json`) in the same schema. |
| S2 | Inexact alarms can fire a few minutes late (Doze). | No; fine for reminders. | — |
| S3 | If notification permission is denied, reminders stop silently. | Yes, if it went unnoticed. | Persistent in-app banner with a deep link to settings. |
| S4 | OEM battery savers (Xiaomi, Oppo, Vivo…) may kill alarms or jobs. Common in BD/IN. | Partly. | Daily refresh plus reschedule on every app open. Optional future "battery optimization" hint. |
| S5 | A bad JSON edit reaches every user within minutes. | Possibly. | Tolerant parser, never cache an empty result, keep last good copy. Future: a CI JSON-schema check on the data repo. |
| S6 | Network or region blocks on `raw.githubusercontent.com`. | Minor. | Cache plus bundled seed. Optional mirror URL later. |
| S7 | Country-derived timezone: BD/IN users abroad still see BD/IN times. | Intended. | They can pick "Other" to follow the device timezone. |
| S8 | Language picked on setup overrides the system language for the app. | No. | Changeable via ⚙. |
| S9 | Bundled seed goes stale in old APKs. | No; the network refresh overrides it. | — |
| S10 | Only next 60 days are scheduled. | No. | Daily job and app opens roll the window forward. |

---

## 10. Verification plan
1. **Unit tests** (`./gradlew test`):
   - Parser: null parana, null end, malformed entry skipped, unknown type kept, unknown fields ignored.
   - Reminders, zone Asia/Dhaka: TWO_DAY at 08:00, ONE_DAY at 20:00, PARANA only when `start != null`, past times dropped, 60-day window respected.
   - Conversion: IST 06:12 → Dhaka 06:42. IST times map to the earlier date in America/New_York.
2. **Build:** `./gradlew assembleRelease`, then check the APK size (< 200 KB) and inspect it with `apkanalyzer`.
3. **Device or emulator:**
   - Fresh install: check preselection, the live language switch, Continue, the permission prompt and the Home list.
   - `adb shell dumpsys alarm | grep vaishnava` shows the scheduled alarms.
   - A sample vrata 2 days ahead → the notification arrives (advance the clock if needed).
   - Reboot → the alarms are re-registered.
   - Airplane mode on a fresh install → the bundled data shows.

---

## 11. Open items (need input from the maintainer)
- [ ] GitHub `OWNER/REPO` for the raw data URL.
- [ ] Final application id / package name (placeholder `com.jovoc.ekadashi`).
- [ ] Real `data/vratas.json` data.
- [ ] Release keystore.
- [ ] License for the open-source repo (e.g. MIT / Apache-2.0 / GPL-3.0).
- [ ] App icon artwork (a simple vector placeholder is planned).

---

## 12. Current repository state
Created before the "plan only" instruction, and kept as requested:
- `settings.gradle.kts`, `build.gradle.kts` (AGP 8.11.0, Kotlin 2.2.10 plugins), `gradle.properties`, `.gitignore`, `local.properties`
- the Gradle 8.14.3 wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`)
- empty directories: `app/src/main/{java/com/jovoc/ekadashi,res/*}`, `app/src/test/...`, `data/`, `docs/`

There is no app module build file, no source code and no data file yet. Implementation starts only after explicit approval.
