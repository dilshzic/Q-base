# Phase 1 File Changes

This phase introduces a single app-level readiness model so the UI can distinguish between session restore, authenticated use, and guest/offline states without changing message relay or E2EE behavior.

## Scope

- Separate **bootstrap state** from **authenticated state**
- Surface a **shared readiness indicator** in the UI
- Keep `SyncRepository` message send/receive logic unchanged

## Files To Add

### `app/src/main/java/com/algorithmx/q_base/ui/state/AppAccessState.kt`
- Define the shared app access/readiness states used by the UI
- Recommended states:
  - `RestoringSession`
  - `OnlineReady`
  - `SignedInOffline`
  - `OfflineGuest`

### `app/src/main/java/com/algorithmx/q_base/ui/state/LocalAppAccessState.kt`
- Expose the shared access state through a `CompositionLocal`
- Avoid threading the same state through every screen signature

## Files To Edit

### `core-auth/src/main/java/com/algorithmx/q_base/data/auth/AuthRepository.kt`
- Expose bootstrap/session-restore completion more clearly than the current `currentUser == null` check alone
- Keep `currentUser` intact for existing feature flows

### `app/src/main/java/com/algorithmx/q_base/MainActivity.kt`
- Build the app-level access state from:
  - session bootstrap completion
  - active user presence
  - persisted login hint
  - connectivity signal if introduced in this phase
- Provide the shared state above `MainScreen`

### `app/src/main/java/com/algorithmx/q_base/ui/components/reusable/CommonComponents.kt`
- Update `UnifiedTopAppBar` to render a compact readiness/status indicator
- Keep the change presentational only in this phase

### `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatDetailScreen.kt`
- Mirror the same readiness indicator on the custom `TopAppBar`
- Keep chat interaction logic unchanged in this phase

## Explicit Non-Goals

- No changes to `app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt`
- No new pending-send queue
- No server-side message retention changes
- No E2EE key wrapping or relay cleanup changes
