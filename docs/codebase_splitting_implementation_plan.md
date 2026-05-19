# Codebase Splitting & Modularization Plan (>500 Lines)

This document outlines the systematic strategy to break down the remaining 9 files containing more than 500 lines of code into smaller, single-responsibility files, placing them into dedicated subfolders (`components`, `state`, `helpers`) to improve maintainability and readability.

---

## Target Files & Proposed Splitting Strategy

### Phase 1: Chat UI & Navigation Components

#### 1. `ChatDetailScreen.kt` (727 lines)
*   **Target Subfolder**: `ui/chat/components/`
*   **Split Strategy**:
    *   Extract `MessageInputSection` (the composable containing the text box, attachment menus, and voice/AI indicators) to `MessageInputSection.kt`.
    *   Extract popup/sheet dialogues (participant configuration sheets, group invite sharing, blocking confirmation dialogs) to `ChatDetailDialogs.kt`.
    *   Extract custom action bars/headers (with custom scroll behaviors and presence status indicators) to `ChatDetailHeader.kt`.

#### 2. `AppEntryProvider.kt` (522 lines)
*   **Target Subfolder**: `ui/navigation/components/` and `ui/navigation/graphs/`
*   **Split Strategy**:
    *   Extract the bottom tab bar structure and tab icons to `AppBottomNavBar.kt`.
    *   Extract nested destination sub-graphs (e.g., `connectGraph`, `exploreGraph`, `sessionsGraph`) into separate extension files under `ui/navigation/graphs/`.

---

### Phase 2: Settings & Session Screens

#### 3. `SettingsScreen.kt` (516 lines)
*   **Target Subfolder**: `ui/settings/components/`
*   **Split Strategy**:
    *   Extract the database backup/restore & device syncing controls to `BackupSettingsView.kt`.
    *   Extract legal, about info, and version information layouts to `AboutSettingsView.kt`.

#### 4. `ActiveSessionScreen.kt` (516 lines)
*   **Target Subfolder**: `ui/sessions/components/`
*   **Split Strategy**:
    *   Extract the large `MasterNavigator` vertical grid view (lines 438-517) to `MasterNavigator.kt`.
    *   Extract any custom session alert dialogs (such as reporting dialogs) to `SessionDialogs.kt`.

#### 5. `ProfileComponents.kt` (642 lines)
*   **Target Subfolder**: `ui/settings/components/`
*   **Split Strategy**:
    *   Extract custom preference card items (theme switches, notification toggles) to `SettingsCards.kt`.
    *   Extract the profile editor (custom avatar picker, display name editor, bio input fields) to `ProfileEditorComponents.kt`.

---

### Phase 3: Sync Repositories

#### 6. `MessageSyncRepository.kt` (777 lines)
*   **Target Subfolder**: `data/sync/crypto/` and `data/sync/streaming/`
*   **Split Strategy**:
    *   Extract Tink cryptographic key operations (wrapping, unwrapping session keys, and profile key serializing) to `MessageCryptoSyncHandler.kt`.
    *   Extract Appwrite real-time messaging event handler loop (listening to incoming websocket frames and updating the database) to `MessageRealtimeStreamer.kt`.

#### 7. `CollectionSyncRepository.kt` (707 lines)
*   **Target Subfolder**: `data/sync/library/` and `data/sync/updates/`
*   **Split Strategy**:
    *   Extract study collection library remote fetch & observation logic to `CollectionLibrarySync.kt`.
    *   Extract collection delta updates, versioning logs, and patch applications to `CollectionMicroUpdater.kt`.

---

### Phase 4: Large ViewModels

#### 8. `ChatViewModel.kt` (968 lines)
*   **Target Subfolder**: `ui/chat/state/` and `ui/chat/helpers/`
*   **Split Strategy**:
    *   Move view state definitions (`ChatListState`, `ChatDetailState`, `ChatUiModel`) to `ui/chat/state/ChatUiState.kt`.
    *   Extract chat admin actions (muting, blocking, member role promotion/demotion) to a delegate helper `ChatAdminDelegate.kt`.
    *   Extract AI prompt generation and response streaming pipelines to `ChatAiDelegate.kt`.

#### 9. `ExploreViewModel.kt` (588 lines)
*   **Target Subfolder**: `ui/explore/state/` and `ui/explore/helpers/`
*   **Split Strategy**:
    *   Move search filter states and active filter logic to `ui/explore/state/ExploreState.kt`.
    *   Extract remote community search / metadata sync functions to a helper delegate `ExploreSyncDelegate.kt`.

---

## Safety Guidelines for Splitting
1. **Phased Commits**: Complete each phase, run `./gradlew compileDebugKotlin` to ensure compilation is fully intact, and verify logic.
2. **Hilt Dependencies**: Keep main ViewModel constructor injections simple; delegating behavior to helpers should be done by injecting singleton/activity-scoped managers, avoiding circular dependencies.
3. **No Package Violations**: Maintain original class names and imports to avoid breaking other files relying on them.
