# Privacy Policy for Q-base Quiz App

**Last Updated:** March 21, 2026

This Privacy Policy explains how Q-base ("we", "us", or "our") collects, uses, and discloses your information when you use our mobile application (the "App").

## 1. Information We Collect

### A. Information You Provide to Us
*   **Account Information:** When you create an account, we collect your email address and authentication details via our cloud authentication provider.
*   **User-Generated Content:** Questions, collections, study sessions, chat messages, custom AI prompts, and your interactions with questions (such as answer selections, study attempts, performance telemetry, and custom tags) you create or upload are stored to provide the App's core functionality.

### B. Information Collected Automatically
*   **Device Information:** We may collect basic device information required for crash reporting and analytics to improve the App's stability.
*   **Usage Data:** We track your interaction with the AI features (e.g., token usage, model preferences) to manage API quotas and improve the "Brain Engine" performance. We also collect aggregated, non-identifiable chat and interaction statistics (such as message counts, active chat channels, and session metrics) to monitor sync reliability and system health.

## 2. How We Use Your Information

We use the collected information for the following purposes:
*   To provide, maintain, and improve the App's features (e.g., syncing your progress, enabling multi-device access).
*   To facilitate Universal Collection Sharing and chat sessions via Appwrite and our cloud synchronization layer.
*   To process AI requests (such as generating or extracting questions or question answers) by securely transmitting your prompts to selected third-party AI providers.

## 3. Data Storage and Third-Party Services

Your data is processed and stored using the following trusted third-party services. By using the App, you consent to their respective privacy policies:

*   **Appwrite:** We use Appwrite Authentication for login identity and Appwrite Storage for temporarily hosting ZIP/JSON files when you share Universal Collections with other users.
*   **Cloud Synchronization:** We use our backend synchronization layer for syncing chat messages and real-time sessions.
*   **AI Providers (Gemini, Groq):** When you interact with the Qbase AI Chatbot, Note Generator, or Question Extractor, the specific text prompts and context you provide are sent to the AI provider you have selected in your settings to generate a response. AI processing is subject to the respective provider's terms (Google Gemini and Groq/GroqCloud for text generation, question extraction, and high-performance inference). We do not use your personal data to train these third-party models.
*   **Appwrite and Cloud Sync Data Minimization:** Apart from basic user identification details (such as your account email/profile) and structural metadata (specifically Group IDs, Chat IDs, and Group/Shared Collection IDs), no other user data is stored persistently in the Appwrite backend. When sharing data—including peer-to-peer (P2P) or group collections, real-time study sessions, and chat messages—it is temporarily hosted on our cloud synchronization layer in an end-to-end encrypted format (**AES-GCM-256**). This transient data is automatically deleted immediately after successful delivery to the recipient, or purged after a maximum 7-day window if undelivered. If you delete chat messages or collections from your device, they are immediately and permanently removed from our cloud synchronization layer as well.

## 4. End-to-End Encryption (E2EE)

To ensure maximum privacy for your interpersonal communications:
*   **Chat Messages:** All chat messages are encrypted on your device using the **AES-GCM-256** algorithm before being sent to our synchronization backend. The symmetric keys used for encryption are derived from user-specific key pairs managed via the Android Keystore system.
*   **File Transfers:** Question bank ZIP files shared via Appwrite are similarly encrypted.
*   **Zero Access:** Because the encryption keys remain on the participants' devices, neither Q-base staff nor our cloud providers can read the contents of your private messages or shared collections.
*   **Encrypted Key Backups:** To facilitate account recovery, you have the option to back up your cryptographic encryption keys. If enabled, backup keys are encrypted on your device using a key derived from your self-selected passphrase (which is never sent to our servers). You have full control over whether to store these encrypted backup keys on our cloud servers or manage them manually and offline.

## 5. Local Storage

A significant portion of your data (such as your personal questions, study attempts, and detailed collection data) is stored locally on your device within a standard SQLite (Room) database. Operating system-level sandboxing ensures device-isolated integrity and secure access to your files, while cryptographic protection is applied to data transmitted in transit via End-to-End Encryption (E2EE).

## 6. Security

We take reasonable measures to help protect your personal information from loss, theft, misuse, unauthorized access, disclosure, alteration, and destruction. However, no internet or electronic storage system is 100% secure.

## 7. Your Choices and Controls

*   **Account Deletion:** You can request to delete your account and associated cloud data at any time through the App's settings or by contacting us.
*   **Master AI Freeze:** You have the ability to explicitly disable all background and foreground AI processing via the "Master AI Freeze" option in your settings.
*   **Chat Deletion:** Users can "Leave Chat" or delete chat history locally. If you are not the admin of a chat, deleting the chat will remove your participant record from the cloud metadata, ensuring your association with the chat is severed.
*   **Local Data:** You can clear your local database or app cache via your device's system settings.

## 8. Contact Us

If you have any questions or suggestions about our Privacy Policy, do not hesitate to contact us at:
supportqbase@gmail.com
