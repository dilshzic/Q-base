# Phase 2 File Changes

This phase applies the shared access/readiness model to navigation entry points and the Connect experience so guest, restoring, and signed-in states no longer collapse into the same UI path.

## Scope

- Apply the Phase 1 app access state to top-level routing decisions
- Refine the Connect screen behavior for guest vs restoring vs signed-in users
- Keep encrypted message transport and relay cleanup untouched

## Files To Edit

### `app/src/main/java/com/algorithmx/q_base/MainActivity.kt`
- Stop using a single persisted-login boolean as the only startup route signal
- Choose startup presentation based on the shared access state from Phase 1
- Keep the existing custom navigation model intact

### `app/src/main/java/com/algorithmx/q_base/ui/navigation/AppEntryProvider.kt`
- Keep route registration unchanged
- Update screen entry behavior where login redirects or guest fallbacks depend on the new access state

### `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatListScreen.kt`
- Distinguish:
  - session still restoring
  - guest/offline mode
  - signed-in ready state
- Avoid showing the guest CTA while the app is still restoring a valid session
- Keep selection mode and chat list rendering behavior intact

### `app/src/main/java/com/algorithmx/q_base/ui/auth/LoginScreen.kt`
- Prevent conflicting UI states when the app is already restoring or has already recovered a valid session
- Keep sign-in flows unchanged

### `app/src/main/java/com/algorithmx/q_base/ui/auth/SignupScreen.kt`
- Align sign-up entry behavior with the same access-state rules
- Keep account creation logic unchanged

## Optional Follow-Up Files

These should only change if the Connect/login flow still leaks old assumptions after the main edits:

- `app/src/main/java/com/algorithmx/q_base/ui/navigation/Screen.kt`
- `app/src/main/java/com/algorithmx/q_base/MainActivity.kt`

## Explicit Non-Goals

- No changes to `app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt`
- No changes to message encryption, wrapped-key handling, or relay deletion
- No offline send queue yet
