# App-Wide Offline Queuing & Gating Analysis

Per your directive, I have conducted a full app-wide analysis of all network-dependent operations outside of the core Chat Messaging system (which we successfully queued in the previous phase). 

Our goal is to ensure that while background logins or session restorations are in progress, or during standard network loss, the user can still use the app fluidly (via queues) and is explicitly blocked from actions that structurally require a live connection (via gating).

---

## 1. Domain Analysis & Strategy

### A. Chat Administration (Group Management)
*   **Location:** `ChatRemoteRepository.kt` (e.g., `createChatOnRemote`, `addParticipantToRemote`, `promoteParticipantToAdminOnRemote`, `deleteChatOnRemote`).
*   **Current State:** Executed immediately via Appwrite `Databases`. If offline, they fail silently (or log an error) while the local database updates optimistically, causing a permanent desync.
*   **Recommendation (QUEUE):** These are lightweight mutations. We should queue them locally and flush them sequentially when online. 

### B. User Profile & Preferences
*   **Location:** `ProfileRepository.kt` (e.g., `updateProfile`, `createOrUpdateProfile`).
*   **Current State:** Updates the local `profileCache` but fails the remote Appwrite push if offline, with no mechanism to retry the upload later.
*   **Recommendation (QUEUE):** Profile updates should be queued to ensure your custom display name, bio, and friend code changes are eventually synced to the server.

### C. Moderation & Reporting
*   **Location:** `ChatRemoteRepository.kt` (`reportGroup`, `reportMessage`).
*   **Current State:** Fails silently when offline.
*   **Recommendation (QUEUE):** Safety and moderation reports must not be dropped. They should be queued.

### D. AI Processing & Generation
*   **Location:** `AiBrainManager.kt`, `GenerateQuizScreen`.
*   **Current State:** Generating a study session from AI or asking the AI bot requires live HTTP streaming and API orchestration. Fails ungracefully if offline.
*   **Recommendation (GATE):** AI actions require immediate bidirectional interaction. Queuing a quiz generation for later creates a confusing UX. We must *strictly gate* all AI UI entry points (e.g., disabling the "Generate Quiz" button and displaying an offline banner).

### E. File Transfers & Study Collection Sharing
*   **Location:** `SyncRepository.kt` (`uploadQuestionBankZip`).
*   **Current State:** Requires local ZIP creation, local Tink encryption, massive blob upload to Appwrite Storage, and generating a message payload.
*   **Recommendation (GATE):** Implementing robust background file upload managers (like Android `WorkManager`) for large files is complex and prone to edge-case failures on unreliable networks. For now, complex file transfers and Collection sharing should remain *strictly gated* (disabled offline).

---

## 2. Proposed Architecture: The Universal Action Queue

Rather than building a custom queue for every subsystem (like we did for Messages), we will implement a centralized **Universal Action Queue**.

### Step 1: Core Database Entity
```kotlin
@Entity(tableName = "Offline_Actions")
data class OfflineActionEntity(
    @PrimaryKey val actionId: String,
    val actionType: String, // e.g., "ADD_PARTICIPANT", "UPDATE_PROFILE", "REPORT_GROUP"
    val payloadJson: String, // Serialized parameters for the action
    val timestamp: Long,
    val retryCount: Int = 0
)
```

### Step 2: Centralized Queue Manager
A new `UniversalQueueManager` will listen to the same `NetworkMonitor` trigger we set up in `MainActivity`. When connectivity returns:
1. It fetches all `OfflineActionEntity` records ordered by timestamp.
2. A `when(actionType)` router delegates the payload to the appropriate repository (e.g., `ChatRemoteRepository`, `ProfileRepository`).
3. On success, the action is deleted from the queue. On persistent failure, it increments `retryCount`.

### Step 3: UI Gating Expansion
We will utilize the existing `LocalAppAccessState` to systematically disable:
1. AI Quiz Generation triggers.
2. Profile picture upload endpoints (since image blobs fall under file transfers).

---

## 3. Implementation Phasing

If you approve this analysis, we can implement it in the following safe phases:
*   **Phase 1:** Create `OfflineActionEntity`, `ActionDao`, and the `UniversalQueueManager` engine.
*   **Phase 2:** Reroute `ChatRemoteRepository` and `ProfileRepository` mutations through the Universal Queue when offline.
*   **Phase 3:** Apply UI gating to AI generation and profile image uploads.

Please review this app-wide analysis. Do not hesitate to request adjustments or scope changes. **I will not proceed with implementation until you explicitly approve.**
