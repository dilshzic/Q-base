# Chat Function Error Fixes

Date: 2026-05-20

This document records the chat-related fixes applied carefully, one by one.

## 1) Prevent duplicate/leaked chat observers

**Problem**
- Opening/switching chats repeatedly could leave old observers running.
- This could cause duplicate updates and unnecessary background work.

**Fix**
- Added tracked `Job` references in `ChatViewModel`.
- Cancel previous jobs before starting new observers in `setChatId`.
- Added early return when the selected chat ID is already active.

**Files**
- `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt`

---

## 2) Harden AI chat detection

**Problem**
- AI chat detection used substring matching on participant IDs.
- Substring matching can false-match unrelated IDs.

**Fix**
- Replaced substring checks with exact participant ID token checks using split + trim + equality.

**Files**
- `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt`
- `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModelMessageExtensions.kt`

---

## 3) Prevent duplicate participant addition

**Problem**
- Group participant add flow could append users that were already members.

**Fix**
- Added guard against blank user IDs.
- Normalized participant list (`split`, `trim`, non-blank filtering).
- Added duplicate-membership check before updating participants.

**Files**
- `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModelAdminExtensions.kt`

---

## 4) Prevent removing the last group admin

**Problem**
- Demote/remove flows could leave a group with zero admins.

**Fix**
- Added explicit checks to block:
  - removing a participant if they are the last remaining admin
  - demoting an admin when they are the last remaining admin

**Files**
- `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModelAdminExtensions.kt`

---

## 5) Remove unused/no-op shared collection logic

**Problem**
- Shared collection save flow created a random ID and called an unused repository fetch.
- This code had no functional effect and added confusion.

**Fix**
- Removed the no-op random ID + unused lookup call.
- Kept the intended `saveAsCollection` flow.

**Files**
- `app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModelSharingExtensions.kt`
