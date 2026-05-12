# End-to-End Encryption (E2EE) Implementation Plan

Integrating E2EE into Qbase ensures that neither the application host, Firebase, nor Appwrite can read the contents of user chats, shared collections, or files. 

## 1. Cryptographic Architecture
We will use an authenticated encryption protocol (AES-GCM) for message/file payload encryption.

For key exchange we will use **public‑key hybrid encryption** (ECIES P‑256 via Google Tink) to wrap (encrypt) the symmetric AES key for each recipient.

**Implementation note (matches current code):** Qbase generates a random ephemeral AES‑GCM key per message/file, encrypts the payload once, then encrypts (“wraps”) that AES key separately for each participant using their published public key.

### Dependencies
Android provides `Tink`, Google's cryptography library, which is highly recommended for securely managing keys and performing AES-GCM and ECDH in a way that avoids common footguns.
*   `com.google.crypto.tink:tink-android:1.8.0`

## 2. Key Management & Distribution

### Identity Keys (Long-term)
Every user needs a long-term Key Pair (Public/Private).
*   **Private Key:** Stored locally in the Android `KeyStore`. It NEVER leaves the user's device.
*   **Public Key:** Published to a public `Users/{userId}` document in Firestore.

**Implementation detail:** Keys are managed by Tink `AndroidKeysetManager` using a Keystore-backed master key URI (e.g. `android-keystore://...`). The public keyset is serialized (Base64) and stored in Firestore.

### Session Keys (Ephemeral)
When exchanging messages/files, we generate an ephemeral symmetric key.
1.  **Generate session key:** Create a new random AES‑256‑GCM key (Tink AEAD key).
2.  **Wrap for recipients:** Encrypt the session key separately for each recipient using Tink Hybrid (ECIES P‑256) and the recipient’s published public key.
3.  **Unwrap on device:** The recipient uses their private key to decrypt (“unwrap”) the session key locally, then decrypts the payload with AES‑GCM.

## 3. Encrypting Firestore Chats

*   **Current State:** `MessageEntity` stores plaintext `payload`.
*   **E2EE State:**
    *   Sender generates a random AES‑256‑GCM session key and encrypts the `payload`.
    *   Sender uploads the ciphertext to Firestore plus a `wrappedKeys` map (`userId -> wrappedSessionKey`) so every participant can unwrap the same session key.
    *   Receiver fetches the ciphertext + their wrapped key, unwraps the session key using their private key, decrypts locally, then saves plaintext to Room.
*   **Group Chats:** Requires "Sender Keys" (Signal Protocol) or sharing a symmetric group key wrapped individually for each group member's public key. For Qbase, wrapping a symmetric group key is simpler to implement over Firestore.

## 4. Encrypting Collections & Storage (Appwrite)

When exporting and sharing a `.zip` collection via `MockExporter`:
1.  Locally generate a random symmetric AES-256 file key.
2.  Encrypt the `.zip` file using this key.
3.  Upload the *encrypted* `.zip` to Appwrite.
4.   The `FILE_TRANSFER` message sent over the chat contains the URL *and* the symmetric file key (or a serialized key handle).
5.  Since the chat itself is E2EE, the symmetric file key is securely transported to the recipient, who uses it to decrypt the downloaded ZIP.

## 5. Security Considerations and Challenges

*   **Multi-Device Sync:** If a user logs into a new device, they don't have their Private Key (it's in the old device's KeyStore). To fix this, you need a mechanism to sync the private key securely (e.g., generating a backup passphrase/QR code, or forwarding keys from the old device to the new device). Wait, Signal handles this via Linked Devices.
*   **Firebase Functions / Qbase AI bot:** The AI bot lives on the server or acts directly. If chats are E2EE, the AI bot *must* have its own Public/Private key pair, or it can only run locally on the device (as it currently does). Since `GeminiBrainImpl` runs locally on the Android device, it can read the decrypted local Room database perfectly! Sending E2EE messages to the AI bot simply means the AI operates on the decrypted local text.

## 6. Draft Implementation Steps
1.  **Add Tink dependency** and initialize it in `QbaseApplication`.
2.  **Generate User Key Pairs** on signup/login. Store private in `EncryptedSharedPreferences` or KeyStore. Publish public key to Firestore.
3.  **Update `SyncRepository`**:
    *   Intercept outbound messages, fetch every participant's public key, generate random AES session key, encrypt `payload`, wrap session key for each participant.
    *   Intercept inbound messages, unwrap session key for the current user, decrypt `payload` before inserting into Room.
4.  **Update `MockExporter`/`MockDownloader`**:
    *   Wrap input/output streams with Tink's Streaming AEAD to encrypt/decrypt large ZIP files on the fly.
