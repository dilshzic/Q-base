# Fix Contact and Chat Syncing Issues

The app currently faces several issues with contact and chat syncing:
1.  **Missing Public Keys and Contact Names**: Users cannot start chats or see contact names because public keys and display names are often not fetched correctly from Appwrite.
2.  **Chat Sending Failures**: Messages fail to send and remain in the queue because of missing encryption keys or empty participant lists.
3.  **Inconsistent Syncing**: Real-time updates and manual syncs often fail to receive or process data correctly due to inconsistent channel patterns and potential JSON corruption.

## Proposed Changes

### Appwrite Backend Component

#### [AppwriteDatabaseImpl.kt](file:///home/dilshan/AndroidStudioProjects/Qbase/core-auth/src/main/java/com/algorithmx/q_base/data/backend/AppwriteDatabaseImpl.kt)

- **Fix Permissions**: Change `Permission.write` to `Permission.create` to ensure new documents can be created. Appwrite 1.4+ uses `create` and `write` (alias), but using explicit `create`, `update`, `delete` is more robust across SDK versions.
- **Robust Field Mapping**: Ensure `mapRow` handles potential nulls and different JSON structures gracefully.

```diff
-                io.appwrite.Permission.write(io.appwrite.Role.user(documentId)),
+                io.appwrite.Permission.create(io.appwrite.Role.user(documentId)),
```

#### [AppwriteModule.kt](file:///home/dilshan/AndroidStudioProjects/Qbase/app/src/main/java/com/algorithmx/q_base/core/backend/di/AppwriteModule.kt)

- **Fix JSON Shielding**: Disable or refine the logic that renames `data` to `payloadData`. This logic likely corrupts Appwrite service responses (like `TablesDB`) where `data` is a reserved field for document content.

```diff
-                                    if (obj.has("data") && obj.get("data") is String) {
-                                        val dataVal = obj.getString("data")
-                                        obj.remove("data")
-                                        obj.put("payloadData", dataVal)
-                                        modified = true
-                                    }
```

---

### Sync Orchestration Component

#### [ChatManagerRepository.kt](file:///home/dilshan/AndroidStudioProjects/Qbase/app/src/main/java/com/algorithmx/q_base/sync/orchestration/ChatManagerRepository.kt)

- **Fix Participant Parsing**: Handle cases where `participantIds` might be returned as a CSV string instead of a `List<String>` from the remote database.
- **Ensure Profiles are Synced**: Ensure that when a chat is synced, participant profiles (including public keys) are also synced and cached locally.

```diff
-                        val participantsList = doc["participantIds"] as? List<String> ?: emptyList()
+                        val participantsList = when (val p = doc["participantIds"]) {
+                            is List<*> -> p.filterIsInstance<String>()
+                            is String -> p.split(",").filter { it.isNotBlank() }
+                            else -> emptyList()
+                        }
```

#### [MessageSyncIncomingExtensions.kt](file:///home/dilshan/AndroidStudioProjects/Qbase/app/src/main/java/com/algorithmx/q_base/sync/orchestration/MessageSyncIncomingExtensions.kt)

- **Align Real-time Channel**: Update the real-time subscription channel to match the pattern used in `AppwriteDatabaseImpl` if using `TablesDB`.
- **Improve Key Alignment**: Ensure public keys are aligned correctly on the remote server for the current user.

---

### Profile Component

#### [ProfileRepository.kt](file:///home/dilshan/AndroidStudioProjects/Qbase/core-auth/src/main/java/com/algorithmx/q_base/data/auth/ProfileRepository.kt)

- **Robust Profile Mapping**: Handle potential nulls or different field names for `displayName` and `publicKey`.

## Verification Plan

### Manual Verification
1.  **Profile Sync**:
    - Sign in with a new account.
    - Verify that a `users` document is created in Appwrite with a valid `publicKey`.
2.  **Contact Search**:
    - Search for a contact by friend code.
    - Verify that the contact name and public key are correctly fetched and cached in the local Room database.
3.  **Chat Sending**:
    - Start a chat with a contact.
    - Send a message.
    - Verify that the message is sent successfully and not stuck in the queue.
4.  **Real-time Updates**:
    - Use two devices/accounts.
    - Send a message from one.
    - Verify the other receives it in real-time.
