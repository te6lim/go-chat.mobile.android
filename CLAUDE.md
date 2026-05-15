# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew build                         # Full build (all modules)
./gradlew :app:build                    # Android app only
./gradlew :app:installDebug             # Install debug APK on connected device/emulator
./gradlew :shared:build                 # KMP shared library
./gradlew :cli:run                      # Run the CLI interactively
./gradlew clean                         # Remove all build artifacts
./gradlew lint                          # Android lint
./gradlew test                          # Unit tests
./gradlew :app:connectedAndroidTest     # Instrumented tests (requires device/emulator)
```

## Module Structure

Three Gradle modules defined in `settings.gradle.kts`:

- **`:app`** — Android application. Compose UI, ViewModels, Room database, Android-specific implementations.
- **`:shared`** — Kotlin Multiplatform library (`androidTarget` + `jvm`). All business logic, repositories, HTTP/WebSocket handling, and data models. Consumed by both `:app` and `:cli`.
- **`:cli`** — JVM command-line app for testing GoChat without an Android emulator. Depends on `:shared` (jvm target).

## Architecture

The project follows Clean Architecture with MVVM and the Repository pattern.

```
:app (Compose UI + ViewModels)
  └── :shared (Repository + Remote layers)
        └── chat-library-2.0.jar  (ChatEngine — WebSocket management)
```

### Shared Module (`shared/src/commonMain/`)

The core of the app. Organized as:

- `repository/` — `ChatRepository`, `ConversationsRepository`, `LoginRepository`, `SignupRepository`. These implement `ChatEngineEventListener<Message>` to react to WebSocket events.
- `remote/` — Ktor HTTP client with auto token refresh (`HttpHandler.kt`), API services (`api_services/`), and use-cases (`api_usecases/`).
- `model/` — Shared data classes: `Message`, `ChatInfo`, `UIMessage`, etc. Enums: `MessageStatus`, `PresenceStatus`.
- `listener/` — `ChatEventListener` and `ConversationEventListener` interfaces (repositories call these to notify ViewModels).
- `storage/` — `IChatStorage` and `IConversationsStore` interfaces for persistence abstraction (Room on Android, in-memory on JVM).
- `session/` — `ISession` interface.
- `config/` — `IConfig` interface for URL configuration.
- `util/` — `ChatUtils` (ChatEngine factory), `UserPresenceHelper`, `DateUtils`.

Platform-specific implementations live in `androidMain/` (AndroidConfig, AndroidSession, Room) and `jvmMain/` (JvmConfig, JvmSession, InMemoryChatStorage, InMemoryConversationStore).

### App Module (`app/src/main/java/com/simulatedtez/gochat/`)

- `view_model/` — `LoginViewModel`, `SignupViewModel`, `ChatViewModel`, `ConversationsViewModel`, `AppViewModel`. All hold `LiveData<T>` observed by Compose.
- `view/` — Compose screens (original design).
- `view/redesign/` — New design screens and components (`chatitems/` for message bubbles, `modals/` for sheets).
- `database/` — Room: `AppDatabase`, `DBMessage`, `DBConversation`, DAOs, migrations (current version: 3).
- `remote/` — `AndroidHttpClient.kt`.
- `util/` — `AndroidConfig`, `AndroidDateUtils`, `NetworkMonitor`, `CleanupManager`.
- `listener/` — Android-side event listener implementations.
- `Session.kt` — Global session singleton coordinating navigation and chat state.
- `UserPreference.kt` — EncryptedSharedPreferences wrapper for tokens and user settings.
- `GoChatApplication.kt` — Initializes `UserPreference`, `NetworkMonitor`, `Session`.
- `MainActivity.kt` — Navigation host with Jetpack Navigation Compose.

### ChatEngine (bundled JAR)

`chat-library-2.0.jar` handles WebSocket lifecycle. Repositories implement `ChatEngineEventListener<Message>` with callbacks: `onReceive()`, `onConnect()`, `onDisconnect()`, `onError()`, `onClose()`. The factory is `ChatUtils` in shared.

## Key Patterns

- **Feature flag `USE_NEW_UI`** in `MainActivity.kt` — toggles between the old (`view/`) and new (`view/redesign/`) UI. Currently `true`.
- **Auto token refresh** — `HttpHandler.kt` checks expiry before requests and retries on 401 with a refreshed token.
- **Event listener chain** — WebSocket event → Repository (`ChatEngineEventListener`) → ViewModel listener (`ChatEventListener`/`ConversationEventListener`) → LiveData → Compose UI.
- **KMP abstraction** — Business logic is platform-agnostic. Only persistence and HTTP-client bootstrapping differ per platform.

## API Endpoints (BuildConfig)

- **Debug:** Auth `192.168.0.3:50051`, Chat `192.168.0.3:50053`
- **Release:** Both on `gochat.com`

## Key Dependencies

| Library | Version | Purpose |
|---|---|---|
| Ktor | 3.2.0 | HTTP client (CIO engine) |
| Jetpack Compose BOM | 2025.06.01 | UI framework |
| Room | 2.7.2 | Android local database |
| Kotlinx Serialization | 1.9.0 | JSON |
| Napier | 2.7.1 | Multiplatform logging |
| OkHttp | 4.12.0 | Required by ChatEngine |
| Kotlinx Coroutines | 1.10.2 | Async |

All versions are managed centrally in `gradle/libs.versions.toml`.
