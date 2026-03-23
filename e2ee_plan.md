# End-to-End Encryption (E2EE) Implementation Plan

Integrating E2EE into Qbase ensures that neither the application host, Firebase, nor Appwrite can read the contents of user chats, shared collections, or files. 

## 1. Cryptographic Architecture
We will use an authenticated encryption protocol, specifically AES-GCM for message/file payload encryption, combined with Elliptic Curve Diffie-Hellman (ECDH) via X25519 for secure key exchange between users.

### Dependencies
Android provides `Tink`, Google's cryptography library, which is highly recommended for securely managing keys and performing AES-GCM and ECDH in a way that avoids common footguns.
*   `com.google.crypto.tink:tink-android:1.8.0`

## 2. Key Management & Distribution

### Identity Keys (Long-term)
Every user needs a long-term Key Pair (Public/Private).
*   **Private Key:** Stored locally in the Android `KeyStore`. It NEVER leaves the user's device.
*   **Public Key:** Published to a public `Users/{userId}` document in Firestore.

### Session Keys (Ephemeral)
When exchanging messages, a shared secret is derived.
1.  **Key Agreement (ECDH):** User A uses their Private Key and User B's Public Key to compute a shared secret.
2.  **Key Derivation Function (KDF):** The shared secret is passed through HKDF to generate a symmetic AES-256 key.

## 3. Encrypting Firestore Chats

*   **Current State:** `MessageEntity` stores plaintext `payload`.
*   **E2EE State:**
    *   Sender encrypts the `payload` using the derived AES-256 session key.
    *   The ciphertext is uploaded to Firestore along with an initialization vector (IV) and an authentication tag (inherent to AES-GCM).
    *   Receiver fetches the ciphertext, computes the identical session key (using their Private Key + Sender's Public Key), and decrypts the payload locally before saving to Room.
*   **Group Chats:** Requires "Sender Keys" (Signal Protocol) or sharing a symmetric group key wrapped individually for each group member's public key. For Qbase, wrapping a symmetric group key is simpler to implement over Firestore.

## 4. Encrypting Collections & Storage (Appwrite)

When exporting and sharing a `.zip` collection via `MockExporter`:
1.  Locally generate a random symmetric AES-256 file key.
2.  Encrypt the `.zip` file using this key.
3.  Upload the *encrypted* `.zip` to Appwrite.
4.   The `FILE_TRANSFER` message sent over the chat contains the URL *and* the symmetric file key. 
5.  Since the chat itself is E2EE, the symmetric file key is securely transported to the recipient, who uses it to decrypt the downloaded ZIP.

## 5. Security Considerations and Challenges

*   **Multi-Device Sync:** If a user logs into a new device, they don't have their Private Key (it's in the old device's KeyStore). To fix this, you need a mechanism to sync the private key securely (e.g., generating a backup passphrase/QR code, or forwarding keys from the old device to the new device). Wait, Signal handles this via Linked Devices.
*   **Firebase Functions / Qbase AI bot:** The AI bot lives on the server or acts directly. If chats are E2EE, the AI bot *must* have its own Public/Private key pair, or it can only run locally on the device (as it currently does). Since `GeminiBrainImpl` runs locally on the Android device, it can read the decrypted local Room database perfectly! Sending E2EE messages to the AI bot simply means the AI operates on the decrypted local text.

## 6. Draft Implementation Steps
1.  **Add Tink dependency** and initialize it in `QbaseApplication`.
2.  **Generate User Key Pairs** on signup/login. Store private in `EncryptedSharedPreferences` or KeyStore. Publish public key to Firestore.
3.  **Update `SyncRepository`**:
    *   Intercept outbound messages, fetch receiver's public key, derive secret, encrypt `payload`.
    *   Intercept inbound messages, derive secret, decrypt `payload` before inserting into Room.
4.  **Update `MockExporter`/`MockDownloader`**:
    *   Wrap input/output streams with Tink's Streaming AEAD to encrypt/decrypt large ZIP files on the fly.
