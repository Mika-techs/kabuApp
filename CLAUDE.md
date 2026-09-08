# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app (Java, no Kotlin — `android.builtInKotlin=false`) showing the timetable and exam plan for BSZ Wiesau students, backed by the `digikabu.de` API. Package `org.kabuapp.kabuapp`, single Gradle module `:app`. UI is XML layouts + ViewBinding, no Compose, no Fragments (Activities only).

## Build & Run

```bash
./gradlew assembleDebug            # build APK
./gradlew installDebug             # build + install on connected device/emulator
./gradlew test                     # JVM unit tests (52 of them)
./gradlew :app:lint                # Android lint — currently clean of errors
./gradlew assembleRelease          # minified release build
./gradlew clean
```

`app/src/test` holds JVM unit tests (JUnit 4 + Robolectric). There is no `androidTest` source set: Room DAO tests run in the JVM via Robolectric and `Room.inMemoryDatabaseBuilder`, so everything is reachable with `./gradlew test` and no emulator. Do not add instrumented tests without a reason that Robolectric cannot cover.

### Prerequisites

- JDK 17 (source/target compatibility 17), compileSdk/targetSdk 36, minSdk 28.
- `local.properties` must contain `sdk.dir`.
- **[MetisJson](https://github.com/Random-user420/MetisJson) must be installed in the local Maven repo** — the `io.lilithtechs:metis-json:1.0-SNAPSHOT` dependency resolves via `mavenLocal()`. A build failing on `metis` means it is missing locally, not a version-catalog problem.
- Dependency versions live only in `gradle/libs.versions.toml`; never hardcode versions in `app/build.gradle`.
- `minSdk` is 28, so APIs above it are unavailable without desugaring. `Stream#toList()` in particular needs API 34 — use `collect(Collectors.toList())`. Lint catches this; run it.

### Code style (enforced by review, not by Gradle)

`app/checkstyle.xml` is the source of truth and is run via the Android Studio Checkstyle plugin — there is no Gradle checkstyle task. Key rules the existing code follows:

- Allman braces: `LeftCurly` on a new line, `RightCurly` alone.
- Max line length 160, no tabs, braces required on every `if`/loop.
- `DeclarationOrder`, `UnusedImports`, `AvoidStarImport` (except `java.io`, `java.net`, `java.util`), `OneStatementPerLine`.

## Architecture

### Package layout: package-by-feature

```
core/data/     Room database, converters, AppContainer, ActiveUserStore, credential crypto
core/net/      ApiClient, ApiException, auth interceptor and authenticator
core/ui/       base Activity, ViewModelFactory, theme and notice helpers
core/util/     DateTimeUtils
domain/        LessonPeriods, RefreshState, DbType
feature/{schedule,exam,auth,settings,notification}
```

A feature owns its entity, DAO, API endpoint, DTO, mapper, repository, ViewModel and UI in one directory. `core/data` depends on the feature packages because Room requires a single `@Database` referencing every entity and DAO — that inverted arrow is deliberate and unavoidable.

### Wiring: AppContainer

`AppContainer` (in `core/data`) constructs and owns the object graph: the `AppDatabase`, the OkHttp clients, the API classes, the repositories, the controllers, the two executors. `KabuApp` only builds the container and schedules startup work; it holds no dependencies of its own and exposes no setters.

Activities extend `org.kabuapp.kabuapp.core.ui.Activity`, which applies EdgeToEdge + system-bar insets, provides `barButtonRefListener()` for the nav bar, and exposes `getContainer()`. ViewModels are obtained through `new ViewModelProvider(this, new ViewModelFactory(getContainer()))`. **Anything new that needs a dependency is wired in `AppContainer` and, if it is a ViewModel, added to `ViewModelFactory`.**

### Data flow: Room is the single source of truth

```
Feature API (ScheduleApi / ExamApi / AuthApi)   extends core/net ApiClient
        ↓  *Response DTOs
Mapper  (pure: DTOs → entities, incl. merging)
        ↓
Room    (entities are records, natural primary keys)
        ↓  LiveData
ViewModel  (transforms rows into *Row display models)
        ↓  observe()
Activity
```

There is **no in-memory mirror of the database**. Room emits on every write, so nothing has to notify the UI by hand — there is no polling loop, no `WeakReference<Activity>`, no untyped callback interface. If you find yourself wanting to tell the UI that data changed, write to Room instead.

- **Entities are records** (`Lesson`, `Exam`, `User`, `Lifetime`). Room 2.8.4 supports this. Where a mapper needs a modified copy, the record provides one (`Lesson.withEnd`, `Exam.extendedByADay`).
- **DTOs must stay mutable Lombok classes** (`LessonResponse`, `ExamResponse`, `AuthRequest`): MetisJson instantiates them through a no-arg constructor and setters. They also carry `@Keep`, because it deserialises by reflecting over field names and R8 would otherwise rename them — silently producing empty data.
- **Display models are records** in a sealed interface (`ScheduleRow`, `ExamRow`), built by pure `*RowFactory` classes. Anything that is a view concern — the "now" divider, splitting a block at the long break — is a row type or a transform, never a sentinel value in the data.

### Database

`AppDatabase.getDatabase()` is a double-checked singleton, DB name `kabuApp-db`, **version 5 with `fallbackToDestructiveMigration(true)` and no migration chain.** Everything except `users` is a cache that re-fetches from digikabu.de, so a schema change costs a re-login rather than real data. Bumping the version is therefore cheap — but it *does* log every user out, so do it deliberately. Schemas are exported to `app/schemas/`.

Keys are natural, not surrogate: `Lesson` is `(userId, date, begin, group)` and `Exam` is `(userId, date, info)`. That makes `OnConflictStrategy.REPLACE` a real upsert, which is why refreshes use the `@Transaction replaceForUser` DAO methods instead of a separate delete. Both tables are indexed on `(userId, date)` and carry `ON DELETE CASCADE` to `users`, so removing an account cleans up its rows without a fan-out across controllers.

### Multi-user

Every row of user data carries a `UUID userId`. The **active account lives in SharedPreferences** (`ActiveUserStore`), exposed as `LiveData<UUID>`; ViewModels `switchMap` off it, so switching account re-points every query with no Activity recreation. `SessionController` still orchestrates load/switch/remove. Adding a new per-user data type means: a `userId` column with the FK cascade, a DAO exposing `LiveData`, a repository, and a `switchMap` in the ViewModel.

Credentials are encrypted with an AES/GCM key in the Android Keystore (`CredentialCipher` + `CredentialKeys`); `User.password` and `User.token` are BLOBs. Backup is off (`allowBackup="false"` plus `data_extraction_rules.xml`, since the former is ignored from Android 12).

### Network

OkHttp. `ApiClient` owns the base URL, the timeouts and the status-code → `ApiException.Kind` mapping. **Endpoints know nothing about authentication**: `AuthInterceptor` attaches the bearer token and `TokenAuthenticator` re-authenticates once on a 401 and replays the request. Two clients exist because the authenticate call itself must not be intercepted, which also breaks the construction cycle.

Errors are typed: endpoints throw `ApiException` rather than returning `null`, so "offline" is distinguishable from "nothing scheduled". Repositories catch it and post `RefreshState` (`IDLE`/`LOADING`/`ERROR(kind)`), which drives both the `SwipeRefreshLayout` spinner and the Snackbar. Never go back to returning `null` from an endpoint.

### Caching via Lifetime + Duration

Refreshes are gated by TTL. `LifetimeController` records the last update per `DbType` in memory and in the `lifetimes` key-value table; callers pass acceptable staleness as a `Duration`:

- `ScheduleActivity.onCreate` → `Duration.ofHours(2)`; exams `Duration.ofHours(1)`
- pull-to-refresh → `Duration.ofSeconds(1)` (effectively "force")
- `ExamActivity` → `Duration.ofMinutes(5)`

Because the kind is a column rather than a column per kind, adding a `DbType` needs no schema change.

### Threading

No coroutines or RxJava. Two executors, with different guarantees:

- **`dbExecutor`** — single-threaded, so database write ordering is an invariant. All fire-and-forget DB work goes here.
- **`ioExecutor`** — bounded pool of two, for network work where ordering does not matter.

Reads need no threading code at all: `LiveData` from a DAO is already correctly threaded by Room. `StrictMode` is enabled in debug builds and detects main-thread network and disk access — do not weaken it. Anything blocking called from a click listener must be dispatched to an executor; `AuthController.login` blocks by design and is only called from `AuthViewModel` on `ioExecutor`.

### UI construction

Lists are `RecyclerView` + `ListAdapter` with `DiffUtil` (`ScheduleAdapter`, `ExamAdapter`, `DateAdapter`). Theme colors are read through `ThemeColorResolver.resolveColorAttribute` so Material dynamic color is respected; do not hardcode colors in adapters.

`LoginActivity` is the launcher activity; `ScheduleActivity.onStart` bounces to it when `AuthController.isInitialized()` is false. Every inter-screen bounce calls `finish()` so the back stack cannot return to a sessionless screen.

Strings are localized: `values/strings.xml` (default), `values-de/`, `values-en/`. Add user-facing text to all three.

### Conventions worth keeping

- Never call `LocalDate.now()`/`LocalTime.now()`/`LocalDateTime.now()` directly — go through `DateTimeUtils`. Tests depend on this.
- Period-to-wall-clock mapping belongs in `domain/LessonPeriods`, never in a view class.
- Logging is `java.util.logging.Logger.getLogger("<tag>")`, not `android.util.Log`.
- ISO date formatting applies **only to the exam screen** — intentional, since ISO dates do not fit the schedule's date strip. Do not "fix" this.
- August is skipped when fetching exams (`SUMMER_HOLIDAY_MONTH`): summer holidays, the API never returns exams for it.
- Consecutive identical periods are merged into one block in `ScheduleMapper`; multi-day exams are merged into a duration in `ExamMapper`. Both merges are pure and unit-tested — change them there, with a test.
