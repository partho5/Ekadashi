# Vaishnava Vrata — Project Modules & Progress Tracker

> **Instruction for AI Agent:**
> When asked to *"read project_modules.md and build next part"*:
> 1. Read this file to identify the first module marked as `[ ] In Progress` or `[ ] Pending`.
> 2. Read `PLAN.md` for full context and specifications if needed.
> 3. Refer to the UI layout guide reference image: [app_UI_plan.jpeg](file:///home/haku/projects/AndroidStudioProjects/Vaishnaba-Vrata/app_UI_plan.jpeg) for design aesthetics and layout implementation in Module 5.
> 4. Implement all code, resources, and tests required for that specific module.
> 5. Run appropriate build/test commands to verify the implementation.
> 6. Update this file (`project_modules.md`) by changing the module status to `[x] Completed`.
> 7. Provide a concise summary of what was completed and what to run next.

---

## UI Guide Reference
Visual layout reference for Home screen header, Vrata card, Bengali date/time formatting, and Parana time pill:
![App UI Guide](file:///home/haku/projects/AndroidStudioProjects/Vaishnaba-Vrata/app_UI_plan.jpeg)

---

## Progress Overview
- [x] **Module 1: Build Scaffolding, Assets & Base Resources**
- [x] **Module 2: Core Data Models, Parser, Localized Formatting & Unit Tests**
- [x] **Module 3: Configuration, Detection Heuristics & Network Repository**
- [x] **Module 4: Alarms, Background Refresh Job & Receiver System**
- [x] **Module 5: Single Framework Activity UI (Setup & Home Screens)**
- [x] **Module 6: Final Integration, Release Optimization & APK Verification**

---

## Detailed Module Breakdown

### Module 1: Build Scaffolding, Assets & Base Resources
- **Status:** `[x] Completed`
- **Description:** Set up project build configuration, data seed asset, string translations, color themes, and manifest.
- **Files to create/modify:**
  - `data/vratas.json` (Sample vrata data in IST)
  - `app/build.gradle.kts` (AGP 8.11.0, Kotlin 2.2.10, minSdk 26, targetSdk/compileSdk 36, R8 minification & size optimization settings, zero dependencies beyond stdlib, asset linking to `../data`)
  - `app/proguard-rules.pro`
  - `app/src/main/AndroidManifest.xml` (Permissions: `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `INTERNET`)
  - `app/src/main/res/values/strings.xml`, `values-bn/strings.xml`, `values-hi/strings.xml` (UI strings & `vrata_<type>` labels)
  - `app/src/main/res/values/colors.xml`, `values-night/colors.xml`, `themes.xml` (Warm saffron palette: `#E07A10`, dark/light themes)
  - `app/src/main/res/raw/keep.xml` (Prevent R8 shrinker from removing dynamic `vrata_*` string resources)
- **Verification:** Run `./gradlew assembleDebug` to verify build script & resource compilation without errors.

---

### Module 2: Core Data Models, Parser, Localized Formatting & Unit Tests
- **Status:** `[x] Completed`
- **Description:** Implement pure domain models, org.json parser, timezone conversions (IST to target timezone), localization helper, reminder calculations, and JVM unit tests.
- **Files to create/modify:**
  - `app/src/main/java/io/github/vaishnavavrata/Vrata.kt` (`Vrata`, `Parana` data classes, tolerant JSON parser, `startIn`/`endIn` IST conversion)
  - `app/src/main/java/io/github/vaishnavavrata/L10n.kt` (Locale context wrapper `attachBaseContext`, date/time formatters with localized digits, vrata dynamic string lookup)
  - `app/src/main/java/io/github/vaishnavavrata/Reminders.kt` (Pure function: `(vratas, zone, now) -> List<Reminder>` for 2-day 08:00, 1-day 20:00, and parana 20:00 within rolling 60 days)
  - `app/src/test/java/io/github/vaishnavavrata/ParserTest.kt` (Unit tests for JSON parsing: null parana, missing end time, invalid entries skipped)
  - `app/src/test/java/io/github/vaishnavavrata/RemindersTest.kt` (Unit tests for reminder calculation, timezone shifts, 60-day window limit)
- **Verification:** Run `./gradlew test` to ensure all unit tests pass.

---

### Module 3: Configuration, Detection Heuristics & Network Repository
- **Status:** `[x] Completed`
- **Description:** Create user configuration management with country/language auto-detection heuristics and data fetching/caching repository.
- **Files to create/modify:**
  - `app/src/main/java/io/github/vaishnavavrata/Config.kt` (`Country` enum, `SharedPreferences` reader/writer, country/language inference heuristics based on SIM, Locale, Timezone)
  - `app/src/main/java/io/github/vaishnavavrata/Repo.kt` (Data repository reading `filesDir/vratas.json` with fallback to `assets/vratas.json`, background update via `HttpURLConnection` with `If-None-Match` ETag header)
- **Verification:** Run `./gradlew test` or write unit test for Config inference logic.

---

### Module 4: Alarms, Background Refresh Job & Receiver System
- **Status:** `[x] Completed`
- **Description:** Build notification channels, AlarmManager scheduling, system broadcast receivers, and periodic 24-hour background JobScheduler sync.
- **Files to create/modify:**
  - `app/src/main/java/io/github/vaishnavavrata/Scheduler.kt` (`AlarmManager` inexact alarms registration `setAndAllowWhileIdle`, `JobScheduler` 24h background job setup)
  - `app/src/main/java/io/github/vaishnavavrata/Receivers.kt` (`AlarmReceiver` for posting notifications with localized texts, `SystemReceiver` for `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`)
  - `app/src/main/java/io/github/vaishnavavrata/RefreshJob.kt` (`JobService` for background network fetch and reminder rescheduling)
- **Verification:** Run `./gradlew assembleDebug`.

---

### Module 5: Single Framework Activity UI (Setup & Home Screens)
- **Status:** `[x] Completed`
- **Description:** Implement custom layout XMLs, vector drawables, and single framework activity managing Setup and Home views without external UI frameworks. Follow visual guide reference [app_UI_plan.jpeg](file:///home/haku/projects/AndroidStudioProjects/Vaishnaba-Vrata/app_UI_plan.jpeg) for the header style, Vrata card typography, Bengali numerals, and Parana time pill container.
- **Files to create/modify:**
  - `app/src/main/res/drawable/` (`card_bg.xml`, `card_selected.xml`, `pill_bg.xml`, `pill_selected.xml`, `btn_primary.xml`, `ic_gear.xml`)
  - `app/src/main/res/layout/` (`setup.xml`, `home.xml`, `item_vrata.xml`)
  - `app/src/main/java/io/github/vaishnavavrata/MainActivity.kt`
    - Setup screen: Country selectable cards, Language pills, live language switch preview, POST_NOTIFICATIONS permission prompt.
    - Home screen: Hero card for next vrata (with converted parana window following [app_UI_plan.jpeg](file:///home/haku/projects/AndroidStudioProjects/Vaishnaba-Vrata/app_UI_plan.jpeg)), ScrollView list for upcoming vratas, missing permission warning banner, gear icon for setup modification.
- **Verification:** Run `./gradlew assembleDebug`.

---

### Module 6: Final Integration, Release Optimization & APK Verification
- **Status:** `[x] Completed`
- **Description:** End-to-end integration, release build compilation, APK size verification (< 200 KB target), and sanity checks.
- **Tasks:**
  - Run full test suite: `./gradlew test`
  - Build release package: `./gradlew assembleRelease`
  - Inspect generated release APK size (verify < 200 KB) using build outputs or `apkanalyzer`
  - Final documentation update and summary
- **Verification:** Release APK built successfully under size threshold and all tests passing.
