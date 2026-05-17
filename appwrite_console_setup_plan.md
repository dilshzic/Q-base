# Appwrite Console Setup Plan (Qbase Sync Architecture)

This document provides the exact schema, indexes, and permission requirements for the Appwrite backend to support the `providers/appwrite` implementation.

## 1. Database Initialization
- **Database ID:** `qbase_db` (Matches `BuildConfig.APPWRITE_DATABASE_ID`)
- **Database Name:** Qbase Core

---

## 2. Collections & Attributes

### **users** (Public Profiles)
*Publicly searchable by Friend Code.*
- **Attributes:**
    - `userId`: String (36), Required
    - `displayName`: String (128), Required
    - `profilePictureUrl`: String (512), Nullable
    - `friendCode`: String (20), Required
    - `intro`: String (256), Nullable
    - `publicKey`: String (2048), Nullable
    - `isBanned`: Boolean, Default: `false`
    - `isPhotoVisible`: Boolean, Default: `true`
- **Indexes:**
    - `key_userId`: Unique, Attribute: `userId`
    - `key_friendCode`: Unique, Attribute: `friendCode`
- **Permissions:** 
    - `Any`: Read (to allow searching by friend code)
    - `Users`: Create
    - `Owner`: Update

### **user_private_settings** (Private Data & E2EE Backups)
*Restricted to the owner.*
- **Attributes:**
    - `userId`: String (36), Required
    - `email`: String (128), Required
    - `e2eeBackup`: String (16384), Nullable (Large string for encrypted keyset)
- **Indexes:**
    - `key_userId`: Unique, Attribute: `userId`
- **Permissions:** 
    - `Owner`: Read, Update, Delete

### **app_config** (Global Configuration & Encrypted Keys)
*Used to securely distribute dynamic server-side keys to authenticated clients.*
- **Attributes:**
    - `gemini_api_key`: String (4096), Nullable (AES-GCM encrypted global Gemini key)
    - `groq_api_key`: String (4096), Nullable (AES-GCM encrypted global Groq key)
    - `deepseek_api_key`: String (4096), Nullable (AES-GCM encrypted global DeepSeek key)
- **Indexes:** None
- **Permissions:**
    - `Any`: Read

### **chats** (Group/Private Chat Metadata)
- **Attributes:**
    - `chatId`: String (36), Required
    - `chatName`: String (128), Nullable
    - `isGroup`: Boolean, Default: `false`
    - `participantIds`: String Array (36), Required
    - `adminId`: String (36), Required
    - `createdAt`: Integer, Required
- **Indexes:**
    - `key_chatId`: Unique, Attribute: `chatId`
- **Permissions:** 
    - `Users`: Create
    - `Owner/Participants`: Read, Update (Requires Document Level Security logic or Function)

### **messages** (Chat Content & E2EE Metadata)
- **Attributes:**
    - `chatId`: String (36), Required
    - `senderId`: String (36), Required
    - `type`: String (32), Required (TEXT, IMAGE, COLLECTION_PATCH, etc.)
    - `payload`: String (16384), Required (Encrypted Ciphertext)
    - `wrappedKey`: String (16384), Nullable (JSON map of UIDs to wrapped session keys)
    - `keyFingerprint`: String (64), Nullable
    - `timestamp`: Integer, Required
- **Indexes:**
    - `key_chatId`: Plain, Attribute: `chatId`
- **Permissions:** 
    - `Users`: Create
    - `Participants`: Read (Requires Document Level Security)

### **notifications** (Real-time Event Triggers)
*Used for cross-user signaling (Realtime).*
- **Attributes:**
    - `targetUserId`: String (36), Required
    - `senderId`: String (36), Required
    - `title`: String (128), Required
    - `body`: String (256), Required
    - `data`: String (4096), Required (JSON string)
    - `timestamp`: Integer, Required
- **Indexes:**
    - `key_targetUserId`: Plain, Attribute: `targetUserId`
- **Permissions:** 
    - `Users`: Create
    - `Owner (targetUserId)`: Read, Delete

### **sync_requests** (Collection Sharing Requests)
- **Attributes:**
    - `senderId`: String (36), Required
    - `targetUserId`: String (36), Required
    - `targetCollectionId`: String (36), Required
    - `status`: String (20), Default: `PENDING` (PENDING, GRANTED, REJECTED)
- **Indexes:**
    - `key_targetUser_status`: Plain, Attributes: `targetUserId`, `status`
- **Permissions:** 
    - `Users`: Create
    - `TargetUser`: Read, Update

### **shared_collections** (Group Library)
- **Attributes:**
    - `collectionId`: String (36), Required
    - `chatId`: String (36), Required
    - `sharedBy`: String (36), Required
    - `adminIds`: String Array (36), Required
    - `updatedAt`: Integer, Required
    - *Note: Add other metadata fields like 'name', 'category' as needed by the UI.*
- **Indexes:**
    - `key_chatId`: Plain, Attribute: `chatId`
- **Permissions:** 
    - `Users`: Create
    - `Participants`: Read

### **access_requests** (Shared Library Permissions)
- **Attributes:**
    - `chatId`: String (36), Required
    - `collectionId`: String (36), Required
    - `requesterId`: String (36), Required
    - `status`: String (20), Default: `PENDING`
    - `timestamp`: Integer, Required
- **Indexes:**
    - `key_chatId_status`: Plain, Attributes: `chatId`, `status`

### **shared_sessions** (Active Group Study Sessions)
- **Attributes:**
    - `sessionId`: String (36), Required
    - `chatId`: String (36), Required
    - `title`: String (128), Required
    - `downloadUrl`: String (512), Required
    - `symmetricKey`: String (2048), Required
    - `adminIds`: String Array (36), Required
    - `isAdminOnly`: Boolean, Default: `false`
    - `timestamp`: Integer, Required
- **Indexes:**
    - `key_chatId`: Plain, Attribute: `chatId`

---

## 3. Moderation Collections (reported_*)
Create the following collections with consistent attributes:
- `reported_questions`
- `reported_collections`
- `reported_users`
- `reported_groups`
- `reported_messages`
- `reported_sessions`

**Common Attributes:**
- `reporterId`: String (36)
- `reason`: String (1024)
- `reportedAt`: Integer
- (Target-specific fields like `questionId`, `messageId`, etc.)

---

## 4. Security Configuration Guidelines

1. **Document Level Security (DLS):**
   - For `messages` and `chats`, it is highly recommended to enable **Document Level Security**.
   - When a chat is created, add the user's ID to the document permissions: `user:<userId>`.
   - When a participant is added, the app should also update the document's permissions in Appwrite.

2. **Storage:**
   - **Bucket ID:** Matches `BuildConfig.APPWRITE_BUCKET_ID`.
   - Permissions: `Users` (Create), `Owner` (Read/Update/Delete).

3. **Authentication:**
   - Enable **Email/Password** provider.
   - (Optional) Enable **Anonymous** for guest access if needed.
