# Qbase Shared Collections Logic & Security Audit

This report provides a detailed system-wide audit of the **Shared Collection Logic** in Qbase. It traces how study collections are packaged, encrypted, distributed, updated, and garbage-collected under Qbase's Zero-Retention and End-to-End Encryption (E2EE) guarantees.

---

## 1. Executive Summary

Qbase implements a hybrid E2EE design for sharing bulk data (study collections consisting of questions, options, and answers). The system is built on two primary layers:
1.  **Symmetric Envelope Encryption**: Bulk ZIP packets containing SQLite database exports are encrypted client-side using a transient symmetric key via AES-GCM (Google Tink).
2.  **Asymmetric Key Wrapping**: The symmetric key is encrypted (wrapped) individually for each group participant using their long-term ECIES P-256 public key and uploaded to Appwrite as part of the shared collection metadata.

Additionally, Qbase enforces a **Zero-Retention Storage Policy** where encrypted ZIP archives are permanently deleted from Appwrite Storage as soon as all intended participants have downloaded and imported the collection. Subsequent modifications are synchronized using lightweight, real-time encrypted JSON patches (`COLLECTION_MICRO_UPDATE` / `COLLECTION_PATCH`) transmitted over E2EE chat channels.

---

## 2. Architecture & Components

The shared collection subsystem spans several distinct modules and layers:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          UI & VIEWMODEL LAYER                          │
│     ChatDetailScreen  ◄───►  ChatViewModelSharingExtensions            │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                        DATA EXPORT / IMPORT LAYER                      │
│     MockExporter (ZIP packaging)  ◄───►  MockDownloader (Import/Unzip)  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                           SECURITY & CRYPTO                            │
│                  CryptoManager (Tink Hybrid & AEAD)                    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                           SYNC REPOSITORY                              │
│                      CollectionSyncRepository                          │
│     ┌─────────────────────────────┼──────────────────────────────┐     │
│     ▼                             ▼                              ▼     │
│  Files (ZIP Upload/Down)     Access Delegation             Patches/Deltas  │
│  (CollectionSyncFileExt)    (CollectionSyncReqExt)       (CollectionSyncPatchExt)
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                           CLOUD PROVIDER                               │
│                   Appwrite (Databases & Storage)                       │
└────────────────────────────────────────────────────────────────────────┘
```

### Core Classes & Files
*   **`CollectionSyncRepository.kt`**: Coordinates the dependencies and provides base settings (such as project/bucket IDs) for collection syncing.
*   **`CollectionSyncFileExtensions.kt`**: Handles the zipping, upload, download, and zero-retention deletion mechanisms.
*   **`CollectionSyncRequestExtensions.kt`**: Coordinates user-to-user and group-based access requests and administrative approvals.
*   **`CollectionSyncPatchExtensions.kt`**: Handles the delta update mechanisms for real-time question additions, edits, and deletions.
*   **`CryptoManager.kt`**: Performs local ECIES public key generation, E2EE key wrapping/unwrapping, and symmetric file encryption.
*   **`ChatViewModelSharingExtensions.kt`**: Exposes the UI-facing bindings for importing, sharing, resending, and joining shared collections/sessions.

---

## 3. End-to-End Workflow Analysis

### A. Sharing & Packaging Workflow (Upload)
When a user decides to share a study collection to a group:
1.  **Export to ZIP**: `MockExporter.exportCollection(collectionId)` queries the local Room database (`QuestionDao`, `CollectionDao`) and bundles the collection data into a temporary ZIP file.
2.  **Symmetric Envelope Encryption**: The file is passed to `CollectionSyncRepository.uploadQuestionBankZip()`. It reads the raw bytes, calls `CryptoManager.encryptFileContent(fileBytes)` to encrypt the payload via AES-GCM, and writes the ciphertext back.
3.  **Upload to Storage**: The encrypted ZIP is uploaded to Appwrite Storage under the configured bucket. Appwrite returns a `downloadUrl`.
4.  **Recipient Key Resolution**: In `shareCollectionToGroup()`, the app fetches the participant list of the chat and resolves their public keys:
    *   For the current user: Returns the locally generated public key.
    *   For other participants: Pulls the cached public key from `UserDao` or retrieves it from the remote Appwrite `users` collection.
5.  **Key Wrapping**: The symmetric key is encrypted using each participant's public key:
    $$\text{ciphertextKey} = \text{Encrypt}_{\text{Public\_Key}_{\text{recipient}}}(\text{Symmetric\_Key})$$
    A JSON map containing `{"userId1": "wrappedKey1", "userId2": "wrappedKey2"}` is constructed.
6.  **Metadata Publication**: A document is created in the `shared_collections` database collection with the download URL, the `wrappedKeys` JSON map, and the list of users in `pendingDownloads`. Crucially, the **public `symmetricKey` database column is left blank** to enforce E2EE.

---

### B. Library Sync, Retrieval & Decryption (Download)
When a participant accesses the group library:
1.  **Metadata Listening**: The app calls `observeGroupLibrary(chatId)`, subscribing to real-time events.
2.  **Key Unwrapping**: In `mapGroupLibrary()`, the client checks if their user ID exists in the `wrappedKeys` JSON map.
3.  **Local Decryption**: The client passes the wrapped key to `CryptoManager.decryptMessage(encKey)`. This uses the local Android Keystore-backed private key to decrypt the envelope and extract the plaintext symmetric key.
4.  **File Download & Decrypt**: The client downloads the encrypted ZIP file from `downloadUrl`, passes the bytes and the symmetric key to `MockDownloader.downloadAndImportMock()`, decrypts it, extracts it, and updates their local Room database.
5.  **Receipt Acknowledgment**: The client calls `acknowledgeCollectionDownload(collectionId)`.

---

### C. Zero-Retention Storage Policy
The zero-retention storage policy ensures the cloud does not retain user content:
1.  During `shareCollectionToGroup()`, `pendingDownloads` is populated with all group members (excluding the sender).
2.  Each time a recipient finishes downloading and importing the ZIP file, `acknowledgeCollectionDownload()` is triggered.
3.  The downloader removes their own user ID from the `pendingDownloads` list and updates the Appwrite document.
4.  When `pendingDownloads` becomes empty:
    *   The client deletes the encrypted ZIP from Appwrite Storage (`deleteQuestionBankZip(fileId)`).
    *   This leaves only metadata in the database; the file itself is gone, mitigating long-term storage leaks.

---

### D. Access Requests & Admin Delegation
If a new user joins a group after a collection has been shared, their public key is not in `wrappedKeys`, and the ZIP file may have already been garbage-collected. The user can request access:
1.  **Request**: The new user calls `requestCollectionAccess(chatId, collectionId)`, creating a document in `access_requests` collection with a `PENDING` status.
2.  **Observation**: Group admins listen to pending requests via `observeAccessRequests(chatId)`.
3.  **Approval**: The admin calls `grantCollectionAccess(chatId, collectionId, requesterId)`:
    *   Retrieves the requester's public key from the Appwrite `users` collection.
    *   Decrypts the collection's symmetric key using the admin's own private key (retrieved from the `wrappedKeys` field).
    *   Encrypts (wraps) the symmetric key using the requester's public key.
    *   Updates the `shared_collections` document's `wrappedKeys` JSON map to include the new user's wrapped key.
    *   Marks the access request as `APPROVED`.
4.  **Re-upload (If Purged)**: If the ZIP file has already been deleted from storage (due to Zero-Retention), the UI prompts the admin to "Resend/Re-upload" the collection to put a fresh encrypted copy back in storage so the new member can download it.

---

### E. Version Control & Delta Sync (Micro-Updates)
To synchronize updates to a collection without re-packaging and uploading the entire ZIP file:
*   **Broadcasting updates**: When a question is added/updated locally in a shared collection, `broadcastCollectionMicroUpdate()` is invoked:
    *   It retrieves the current revision from the local `CollectionVersionLedgerEntity`.
    *   Increments the `revisionId` (local revision + 1) and packages the change in a JSON diff packet.
    *   Encapsulates the payload into a standard E2EE chat message of type `COLLECTION_MICRO_UPDATE` and sends it via `MessageSyncRepository.sendMessage()`.
*   **Applying updates**: Other clients receive this message:
    *   If `revisionId <= localRevision`: The update is ignored (redundant).
    *   If `revisionId > localRevision + 1`: A sequence gap is detected (buffering / full sync recommended).
    *   Otherwise, the client parses the diff and inserts/updates the question, options, and answer records in the local SQLite database. It then updates `CollectionVersionLedgerEntity` to the new revision.

---

## 4. Security Assessment

| Security Aspect | Current Implementation | Risk Level | Remarks / Mitigation |
| :--- | :--- | :--- | :--- |
| **Data In-Transit** | Fully Encrypted (Symmetric AES-GCM for ZIPs; hybrid Tink E2EE for metadata & micro-update messages). | **Low** | Man-in-the-middle attacks on Appwrite connections yield only binary ciphertext. |
| **Data At-Rest (Cloud)** | Encrypted ZIPs in Storage; symmetric key columns are left blank in Appwrite databases. Keys exist only inside wrapped JSON blobs. | **Low** | Compromise of Appwrite backend does not expose collection content; keys can only be decrypted by private keys held locally on device hardware. |
| **Zero-Retention Leakage** | ZIPs are deleted when `pendingDownloads` reaches zero. | **Medium** | If a participant remains offline indefinitely, the ZIP remains in storage. **Mitigation**: Introduce a TTL (Time-To-Live) on Appwrite Storage objects (e.g., 7 days) to auto-delete. |
| **Key Wrapping Integrity** | ECIES P-256 keyset handles mapped client-side. | **Low** | Safe from identity spoofing assuming Appwrite public key registration is restricted. |

---

## 5. Architectural Recommendations

1.  **Time-To-Live (TTL) Policy**: Implement an automated backend function (or Appwrite schedule) to delete shared ZIP files older than 7 days, even if some participants have not acknowledged the download. This bounds the duration for which encrypted files remain on the cloud.
2.  **Sequence Gap Resolution**: If a client detects a sequence gap (`revisionId > localRevision + 1`) during micro-updates, they should trigger a full sync request to pull the latest ZIP package rather than simply buffering indefinitely.
3.  **Forward/Backward Secrecy**: Currently, adding a new member requires an admin to manually run `grantCollectionAccess` to re-encrypt the symmetric key. For large groups, consider adopting Sender Keys (similar to Signal protocol group messaging) to automate group key rotation.
