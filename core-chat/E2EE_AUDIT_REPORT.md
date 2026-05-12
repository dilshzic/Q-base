# End-To-End Encryption (E2EE) Architectural Audit

Your implementation using Google Tink and Firestore is a strong baseline. However, building custom E2EE systems is notoriously complex. Based on your current codebase (`CryptoManager.kt`, `SyncRepository.kt`), here are the major architectural flaws, edge cases, and crashes that can arise, along with how to fix them.

## 1. Fatal Crash: Android Keystore Invalidation
**The Error**: In `CryptoManager.kt`, you use `AndroidKeysetManager` backed by `AndroidKeystoreKmsClient`. Android's hardware Keystore is extremely strict. If a user changes their device Lock Screen PIN/Password, or adds/removes a Biometric Fingerprint, the OS may permanently invalidate the Master Key.
**The Impact**: `keysetManager.build()` will throw a `KeyStoreException` because it can no longer decrypt the `SharedPreferences` file. The app will permanently crash on startup.
**The Fix**: 
Wrap your initialization in a `try-catch`. If it fails due to keystore invalidation, you must programmatically delete the `SharedPreferences` keyset file, clear the local database, and immediately prompt the user to use the "Secure Backup" UI you just built.

## 2. The Multi-Device Overwrite Glitch
**The Error**: In Firestore, public keys are stored under `/users/{uid}/publicKey`. 
**The Impact**: If a user logs into Q-base on their Phone, it uploads a public key. If they then log into their Tablet, the Tablet generates a *new* keypair and overwrites the Phone's public key in Firestore. 
Suddenly, friends will start encrypting messages using the Tablet's public key. The Phone will receive the messages, but will fail to decrypt them (showing the "Message Locked" error) because it doesn't possess the Tablet's private key.
**The Fix**: 
*   **Option A**: Force the user to restore their Secure Backup during the Login Flow on new devices. This ensures all devices share the exact same private key.
*   **Option B**: Store keys per-device (`/users/{uid}/devices/{deviceId}/publicKey`). Senders would then have to encrypt the session key multiple times (once for the phone, once for the tablet).

## 3. Lack of Forward Secrecy
**The Error**: You are using a static, long-term asymmetric keypair (`ECIES_P256`).
**The Impact**: If a malicious actor ever compromises your phone, roots it, or guesses your Backup Passphrase, they obtain your single private key. If they have been recording your network traffic or Firestore database over the last year, they can retroactively decrypt *every single message* you have ever sent or received.
**The Fix**: 
State-of-the-art apps (Signal, WhatsApp) use the **Double Ratchet Algorithm**. This constantly rotates the keys after every single message. If a key is compromised today, the attacker cannot read yesterday's messages.

## 4. Firestore 1MB Limit & Thread Blocking on Large Groups
**The Error**: In `SyncRepository.sendMessage`, you iterate through every participant and run `cryptoManager.encryptSessionKey` for each user.
**The Impact**: If you have a group chat with 1,000 members, sending a single message requires 1,000 heavy asymmetric encryptions. This will freeze the app's UI/IO thread. Furthermore, appending 1,000 base64-encoded wrapped keys to the `wrappedKeys` Map will likely exceed Firestore's strict 1 Megabyte document size limit, causing the send to fail entirely.
**The Fix**: 
For group chats, you must use a "Sender Key" protocol. A group symmetric key is generated once and securely distributed to all members. Future messages are just encrypted *once* symmetrically, drastically reducing size and computational load.

## 5. Man-In-The-Middle (MITM) via Firestore Admin
**The Error**: `SyncRepository` blindly trusts whatever public key it downloads from Firestore.
**The Impact**: If a malicious database administrator accesses your Firebase Console, they can replace User B's public key with their own. User A will unknowingly encrypt messages meant for User B using the Admin's key. The Admin intercepts it, reads it, re-encrypts it with User B's real key, and forwards it. Neither user knows they are being spied on.
**The Fix**: 
Implement an "Identity Verification" screen. Users must be able to meet in person and scan a QR code representing their Public Key Fingerprints to ensure the server hasn't tampered with them.
