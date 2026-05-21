# Qbase End-to-End Encryption (E2EE) — Overview

This document describes how E2EE is implemented across Qbase, where the code lives, and the main message/file flows.

## Summary
- Qbase uses hybrid public-key + symmetric authenticated encryption (Google Tink).
- Long-term identity keys (ECIES P-256) are stored in the Android Keystore via Tink's AndroidKeysetManager; public keysets are published to Firestore.
- Each message/file uses a single ephemeral AES-GCM key (Tink AEAD). The session key is encrypted (wrapped) with each recipient's public key and uploaded alongside ciphertext.

## Key Components
- `CryptoManager` — central Tink wrapper: key generation, hybrid encrypt/decrypt, AEAD session keys, file encryption.
  - File: [core-chat/src/main/java/com/algorithmx/q_base/data/util/CryptoManager.kt](core-chat/src/main/java/com/algorithmx/q_base/data/util/CryptoManager.kt)
- `SyncRepository` — orchestrates sending/receiving messages, wrapping session keys, uploading encrypted files, ephemeral deletion logic.
  - File: [app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt](app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt)
- `MockDownloader` / `MockExporter` — encrypt/decrypt ZIP collection payloads with `CryptoManager`.
  - File: [app/src/main/java/com/algorithmx/q_base/data/util/MockDownloader.kt](app/src/main/java/com/algorithmx/q_base/data/util/MockDownloader.kt)
- `ProfileRepository` — generates/publishes user public key during signup/profile sync.
  - File: [core-auth/src/main/java/com/algorithmx/q_base/data/auth/ProfileRepository.kt](core-auth/src/main/java/com/algorithmx/q_base/data/auth/ProfileRepository.kt)
- `ChatViewModel` — handles import/share UX for file transfers and passes symmetric keys through secure message payloads.
  - File: [app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt](app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt)
- `DataClearingRepository` — clears local keys on data wipe.
  - File: [app/src/main/java/com/algorithmx/q_base/data/core/DataClearingRepository.kt](app/src/main/java/com/algorithmx/q_base/data/core/DataClearingRepository.kt)

## Key Generation & Storage
- On first run / signup the app creates a Tink hybrid keyset (ECIES P-256) using `AndroidKeysetManager`.
- Private keyset is stored encrypted and backed by a hardware master key URI (`android-keystore://...`).
- Public keyset is exported (CleartextKeysetHandle → Binary → Base64) and saved to the user's Firestore `users/{userId}` document.
- Method: `CryptoManager.initializeAndGetPublicKey()`.

## Message Send Flow (P2P and Group)
1. Sender calls `SyncRepository.sendMessage()`.
2. Create a single ephemeral AEAD key: `CryptoManager.encryptWithSessionKey()` → returns `(ciphertext, sessionKeyHandle)` where `sessionKeyHandle` is a serialized KeysetHandle (Base64).
3. Session key bytes are decoded and for each recipient the session key is wrapped using the recipient's public key: `CryptoManager.encryptSessionKey(sessionKeyBytes, recipientPublicKeyBase64)`.
4. Firestore message document contains:
   - `ciphertextPayload` (Base64 AEAD ciphertext)
   - `wrappedKeys` map (recipientId -> wrappedSessionKey Base64)
   - `payload` set to a placeholder like `ENCRYPTED_MESSAGE`
   - `keyFingerprint` (sender fingerprint)
5. For groups the same session key is wrapped individually for every participant (client-side fan-out).

## Message Receive Flow
1. Listener (`SyncRepository.observeAndSyncMessages()`) receives a new message document.
2. If `wrappedKeys` and `ciphertextPayload` are present, the client looks up its own wrapped key entry.
3. Client calls `CryptoManager.decryptSessionKey(wrappedKey)` to unwrap session key using local private KeysetHandle.
4. If unwrap succeeds, client decodes the session KeysetHandle and calls `CryptoManager.decryptWithSessionKey(ciphertextPayload, sessionKeyHandle)` to obtain plaintext.
5. `decryptionStatus` flags in `MessageEntity` are set to: `SUCCESS`, `DECRYPTION_ERROR`, `FAILED`, or `NOT_ENCRYPTED`. UI surfaces `DECRYPTION_ERROR` warnings.

## File Sharing Flow (ZIP collections)
- Exported ZIP is encrypted locally with a fresh AEAD Keyset via `CryptoManager.encryptFileContent()` which returns `(encryptedBytes, keyBase64)`.
- Encrypted ZIP is uploaded to Appwrite via `SyncRepository.uploadQuestionBankZip()` and the returned `downloadUrl` and `symmetricKey` are embedded into either a P2P ephemeral message or group shared metadata.
- Recipient downloads bytes, passes them to `MockDownloader.downloadAndImportMock(url, symmetricKey)` which calls `CryptoManager.decryptFileContent()` to recover the ZIP bytes.

## Group Chats, Ephemeral Deletion, and Delivery
- For 1-on-1 chats ephemeral messages may be deleted from Firestore once delivered to the recipient.
- For groups the app updates `deliveredTo` arrays and deletes the message only when all participants have delivered it.
- Group membership changes: messages encrypted before a new member joined will not include their wrapped key (they cannot decrypt historical messages).

## Multi-Device & Key Recovery
- Currently keys are device-local (Keystore-backed). Multi-device sync is a known challenge.
- The e2ee_plan.md notes options: secure export/backup using passphrase-protected key backup, or linked-device forwarding. These are not implemented by default — consider adding a secure recovery path.

## Error Handling & Observability
- Decryption failures are recorded via `decryptionStatus` on `MessageEntity` and surfaced in UI (`MessageBubble` shows `DECRYPTION_ERROR`).
- Recent code changes added explicit logging in `CryptoManager` for decryption failures. Check device logs for `CryptoManager` tags.
- Common failure causes:
  - Missing wrapped key for this device (user joined later / wrong participant list).
  - Private keyset not initialized or cleared (e.g., after account clear or re-install).
  - Corrupted stored keyset or mismatched master key.

## Implementation Notes & Code Locations
- Hybrid key creation & public key export: `CryptoManager.initializeAndGetPublicKey()` — [core-chat/src/main/java/com/algorithmx/q_base/data/util/CryptoManager.kt](core-chat/src/main/java/com/algorithmx/q_base/data/util/CryptoManager.kt)
- Message send/receive orchestration: `SyncRepository.sendMessage()` and `observeAndSyncMessages()` — [app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt](app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt)
- File encryption upload/download: `uploadQuestionBankZip()` and `MockDownloader.downloadAndImportMock()` — [app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt](app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt) and [app/src/main/java/com/algorithmx/q_base/data/util/MockDownloader.kt](app/src/main/java/com/algorithmx/q_base/data/util/MockDownloader.kt)
- Public key publishing during signup/profile sync: [core-auth/src/main/java/com/algorithmx/q_base/data/auth/ProfileRepository.kt](core-auth/src/main/java/com/algorithmx/q_base/data/auth/ProfileRepository.kt)
- UI handling for file import and message formatting: [app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt](app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt)

## Security Considerations & Recommendations
- Protect key backups: implement an opt-in encrypted key backup using a user passphrase and server-stored encrypted blobs only accessible with the passphrase.
- Consider adopting Signal/Double Ratchet or Sender Keys for improved forward secrecy in large groups.
- Limit plaintext in remote logs and notifications (current design uses placeholders for notifications).
- Audit Firestore rules to prevent unauthorized writes to `wrappedKeys`/`messages` collections.

## Next Steps (Suggested)
- Reproduce the reported critical error by collecting a device log trace showing `CryptoManager` tag errors.
- Implement a small health-check that validates the local private keyset exists at app start and surfaces a clear remediation (re-generate keys + republish public key or instruct user to restore backup).
- Add unit/integration tests for `CryptoManager` key wrap/unwrap and AEAD file encryption.

---

For questions or to expand this into a developer onboarding doc (diagrams, sequence flows, and tests), tell me which areas to expand.
