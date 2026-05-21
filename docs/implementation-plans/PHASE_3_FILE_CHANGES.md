# Phase 3 File Changes

This phase introduces a real connectivity signal and uses it to control sync orchestration, instead of inferring online/offline behavior from auth state alone.

## Scope

- Add a reusable connectivity monitor
- Feed connectivity into the app access state from Phase 1
- Start or pause sync work from an explicit online/offline signal

## Files To Add

### `app/src/main/java/com/algorithmx/q_base/util/NetworkMonitor.kt`
- Wrap `ConnectivityManager` callbacks in a small observable API
- Expose whether the device currently has usable network access

## Files To Edit

### `app/src/main/java/com/algorithmx/q_base/data/di/NetworkModule.kt`
- Provide the new `NetworkMonitor` through Hilt
- Keep existing Appwrite and OkHttp providers untouched

### `app/src/main/java/com/algorithmx/q_base/MainActivity.kt`
- Include connectivity when computing the shared app access state
- Gate sync startup so background sync only runs when both:
  - a user is available
  - network connectivity is available

### `core-auth/src/main/java/com/algorithmx/q_base/data/auth/AuthRepository.kt`
- Keep authentication state separate from connectivity state
- Avoid using auth-null states as a proxy for network loss

### `app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt`
- Accept the connectivity signal where needed for sync lifecycle coordination
- Preserve message encryption, wrapped-key processing, and ephemeral relay cleanup logic

### `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt`
- Reflect degraded network state in chat-related UI state where needed
- Keep chat content, selection mode, and deletion flows unchanged

## Explicit Non-Goals

- No server-side queueing
- No redesign of Appwrite schema
- No changes to E2EE payload format or receipt-based deletion semantics
