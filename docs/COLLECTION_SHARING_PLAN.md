# Qbase Collection Sharing & E2EE Group Library Plan

This document outlines the architecture, cryptographic designs, database schemas, and implementation models required to establish a secure, receipt-based End-to-End Encrypted (E2EE) persistent sharing system in Qbase group libraries.

---

## 1. Architectural Foundations of Collection Sharing

Qbase divides material sharing into two distinct pipelines depending on the chat topology:

- **Direct P2P Chat Sharing**: Ephemeral message-driven sharing. The ZIP file is encrypted and shared directly via message-level E2EE using Google Tink's ECIES.
- **Group Persistent Library Sharing**: A persistent drive ("Shared Library") where users can access shared collections long after they were initially posted, secured with dynamic group key wrapping.

---

## 2. Dynamic Group Key Wrapping Architecture

To enforce group privacy without exposing plain keys to Appwrite database servers:

1. **Symmetric Encryption**: When Alice shares a collection, she encrypts the ZIP payload with a secure random AES-256 key.
2. **Asymmetric Wrapping**: Alice fetches the active E2EE public keys of all current group participants. She encrypts the symmetric key for each member using their public key.
3. **JSON Key Map**: She serializes these wrapped keys into a single map stored inside the `shared_collections` document:
   `{"userId1": "wrappedKey1Base64", "userId2": "wrappedKey2Base64"}`
4. **Decryption at Receiver**: When Bob views the group library, his device parses the map, extracts `wrappedKeys[bobId]`, decrypts it locally using his private Tink keyset, and instantly unlocks the collection card.

---

## 3. Advanced Ephemeral Cleanup & Zero-Retention Storage

To minimize cloud storage footprints and enforce ultimate privacy, we implement a receipt-based cleanup protocol:

- **Dynamic Receipt Map**: The `shared_collections` record includes a JSON array of `pendingDownloads` listing all current group participant IDs.
- **Auto-Cleanup**: As each client downloads and imports the collection successfully, they send a receipt mutation that removes their ID from the `pendingDownloads` list.
- **Storage Erasure**: Once `pendingDownloads` becomes empty, the last downloading client immediately triggers a deletion request to Appwrite Storage, removing the encrypted ZIP file permanently from the cloud bucket.
- **On-Demand Resend Protocol (Self-Healing)**: If a new user joins, or an existing member suffers data loss (device reset / key recovery), they will query `shared_collections` but the cloud ZIP will already be garbage collected. The UI displays a **"Request Re-Upload / Resend"** option to notify group administrators to re-key and re-share.

---

## 4. Threshold-Based Micro-Update Diff Synchronization

To optimize bandwidth and prevent full ZIP rebuilds when simple modifications occur (e.g., adding an answer choice or editing a question), Qbase implements a **Delta-Sync Update Protocol**:

- **Diff Message Type (`COLLECTION_MICRO_UPDATE`)**: Simple edits are captured locally as a structural JSON delta (diff) object. These small diffs are encrypted and broadcasted to the group as a special chat message payload.
- **Cumulative Bandwidth Thresholding**: If cumulative edits are below a defined threshold (e.g., `< 10` edits), they are transmitted as cheap micro-updates. If edits cross the threshold, the client triggers a full baseline rebuild, zipping and uploading a new master file.

---

## 5. Local Database Version Log Ledger

To prevent sync race conditions, out-of-order execution, or duplicate applications of delta-sync edits across devices, Qbase implements a strict **Version Ledger State Machine**:

- **Sequential Integrity**: A received micro-update is only applied to the study collection database if its `revisionId` is exactly `currentRevisionId + 1`.
- **Out-of-Order Catch-up**: If a recipient has been offline and receives a micro-update with a gap (e.g., local is `5`, received is `8`), the client holds the update in a buffer and requests the missing intermediate revision messages from the group chat message history.
- **Idempotence**: Revisions less than or equal to the current revision are silently ignored, preventing duplicate mutations.
