# Phase 4 File Changes

This phase hardens the final user-facing chat behavior for degraded/offline conditions after the shared access state, routing, and connectivity infrastructure are in place.

## Scope

- Make the composer and chat actions respect the phased access/connectivity model
- Prevent misleading “ready” interactions when the app is offline or still restoring
- Keep the Appwrite relay + E2EE architecture intact

## Files To Edit

### `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatDetailScreen.kt`
- Disable or clearly gate send actions when the app is not ready to send
- Surface the degraded state in the composer/header instead of failing silently

### `app/src/main/java/com/algorithmx/q_base/ui/chat/components/ChatDetailComponents.kt`
- Apply the same readiness rules to reusable chat input or attachment UI
- Keep existing message rendering behavior unchanged

### `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt`
- Centralize “can send” checks so UI gating is not duplicated across screens
- Keep message encryption and actual send transport delegated to existing repositories

### `app/src/main/java/com/algorithmx/q_base/ui/chat/NewChatScreen.kt`
- Prevent new-chat actions that depend on network/cloud state when the app is offline
- Keep local-only browsing behavior intact if still supported

### `app/src/main/java/com/algorithmx/q_base/ui/chat/NewGroupScreen.kt`
- Apply the same offline/degraded restrictions to group creation flows
- Keep existing group form behavior intact when online

## Optional Validation Targets

If the above changes expose duplicated assumptions elsewhere, review:

- `app/src/main/java/com/algorithmx/q_base/ui/chat/ContactSelector.kt`
- `app/src/main/java/com/algorithmx/q_base/ui/chat/ContactOverviewScreen.kt`
- `app/src/main/java/com/algorithmx/q_base/ui/chat/GroupOverviewScreen.kt`

## Explicit Non-Goals

- No new cloud persistence for pending messages
- No replacement of the current receipt-based cleanup flow
- No changes to public-key alignment or wrapped-key generation
