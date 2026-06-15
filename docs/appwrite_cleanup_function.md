# Appwrite 7-Day Cleanup Function

This document contains the Node.js function needed to automatically clean up Appwrite Cloud Storage (ZIP files) and Database documents (chat messages) that are older than 7 days.

## Prerequisites
1. You need the Appwrite CLI installed (`npm install -g appwrite-cli`).
2. Log in and initialize:
   ```bash
   appwrite login
   appwrite init project
   appwrite init function # Select Node.js, name it cleanup-expired-data
   ```
3. Inside the new function folder, run `npm install node-appwrite`.

## Function Code (`src/main.js`)
Replace the contents of `src/main.js` with the following code. Make sure to replace `'your_bucket_id_here'` with your actual Storage bucket ID where collection ZIPs are uploaded.

```javascript
const { Client, Storage, Databases, Query } = require('node-appwrite');

module.exports = async ({ req, res, log, error }) => {
    // Initialize the Appwrite Client
    const client = new Client()
        .setEndpoint(process.env.APPWRITE_FUNCTION_ENDPOINT)
        .setProject(process.env.APPWRITE_FUNCTION_PROJECT_ID)
        .setKey(process.env.APPWRITE_API_KEY);

    const storage = new Storage(client);
    const databases = new Databases(client);
    
    // Config IDs (make sure BUCKET_ID matches your Appwrite bucket)
    const BUCKET_ID = 'your_bucket_id_here'; 
    const DATABASE_ID = 'qbase_db'; 
    const MESSAGES_COLLECTION_ID = 'messages'; 
    
    // Calculate 7 days ago in ISO 8601 format
    const SEVEN_DAYS_AGO = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();

    let deletedFilesCount = 0;
    let deletedMessagesCount = 0;

    try {
        // --- 1. CLEAN UP STORAGE FILES ---
        let hasMoreFiles = true;
        while (hasMoreFiles) {
            const response = await storage.listFiles(BUCKET_ID, [
                Query.limit(100),
                Query.lessThan('$createdAt', SEVEN_DAYS_AGO)
            ]);
            
            if (response.files.length === 0) {
                hasMoreFiles = false;
                break;
            }

            for (const file of response.files) {
                await storage.deleteFile(BUCKET_ID, file.$id);
                deletedFilesCount++;
            }
        }
        log(`Successfully deleted ${deletedFilesCount} expired storage files.`);

        // --- 2. CLEAN UP DATABASE MESSAGES ---
        let hasMoreMessages = true;
        while (hasMessagesMore) {
            const response = await databases.listDocuments(DATABASE_ID, MESSAGES_COLLECTION_ID, [
                Query.limit(100),
                Query.lessThan('$createdAt', SEVEN_DAYS_AGO)
            ]);
            
            if (response.documents.length === 0) {
                hasMoreMessages = false;
                break;
            }

            for (const doc of response.documents) {
                await databases.deleteDocument(DATABASE_ID, MESSAGES_COLLECTION_ID, doc.$id);
                deletedMessagesCount++;
            }
        }
        log(`Successfully deleted ${deletedMessagesCount} expired messages.`);

        return res.json({ 
            success: true, 
            deletedFiles: deletedFilesCount,
            deletedMessages: deletedMessagesCount 
        });
        
    } catch (err) {
        error(`Cleanup failed: ${err.message}`);
        return res.json({ success: false, error: err.message }, 500);
    }
};
```

## Deployment and Configuration
1. Open `appwrite.json` and set the schedule to run daily at midnight:
   ```json
   "schedule": "0 0 * * *",
   ```
2. Deploy the function:
   ```bash
   appwrite deploy function
   ```
3. **Appwrite Console Setup:**
   * Go to **Overview > Integrations > API Keys**. Create a new key with `files.read`, `files.write`, `documents.read`, and `documents.write` scopes.
   * Go to your newly deployed **Function > Settings > Variables**.
   * Add a new environment variable: `APPWRITE_API_KEY` and paste the secret key.
