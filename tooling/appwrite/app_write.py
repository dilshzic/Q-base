import os
import time
from dotenv import load_dotenv
from appwrite.client import Client
from appwrite.services.tables_db import TablesDB
from appwrite.permission import Permission
from appwrite.role import Role
from appwrite.exception import AppwriteException

# Load environment variables from .env file
load_dotenv()

# --- CONFIGURATION ---
ENDPOINT = os.getenv('APPWRITE_ENDPOINT', 'https://syd.cloud.appwrite.io/v1') # Change if self-hosting
PROJECT_ID = os.getenv('APPWRITE_PROJECT_ID', 'your_project_id_here')
API_KEY = os.getenv('APPWRITE_API_KEY', 'your_api_key_here')
DATABASE_ID = os.getenv('APPWRITE_DATABASE_ID', 'qbase_db')
DATABASE_NAME = os.getenv('APPWRITE_DATABASE_NAME', 'Q-base core')

client = Client()
client.set_endpoint(ENDPOINT)
client.set_project(PROJECT_ID)
client.set_key(API_KEY)

db = TablesDB(client)

# --- SCHEMA DEFINITION ---
collections = {
    "users": {
        "name": "users",
        "dls": True,
        "permissions": [
            Permission.read(Role.users()),
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "users", "userId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "users", "displayName", 128, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "users", "profilePictureUrl", 512, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "users", "friendCode", 20, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "users", "intro", 256, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "users", "publicKey", 2048, False]},
            {"method": db.create_boolean_column, "args": [DATABASE_ID, "users", "isBanned", False, False]},
            {"method": db.create_boolean_column, "args": [DATABASE_ID, "users", "isPhotoVisible", False, True]}
        ],
        "indexes": [
            {"key": "key_userId", "type": "unique", "attrs": ["userId"]},
            {"key": "key_friendCode", "type": "unique", "attrs": ["friendCode"]}
        ]
    },
    "user_private_settings": {
        "name": "user_private_settings",
        "dls": True,
        "permissions": [
            Permission.create(Role.users()),
            Permission.read(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "user_private_settings", "userId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "user_private_settings", "email", 128, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "user_private_settings", "e2eeBackup", 16384, False]}
        ],
        "indexes": [
            {"key": "key_userId", "type": "unique", "attrs": ["userId"]}
        ]
    },
    "app_config": {
        "name": "app_config",
        "dls": False,
        "permissions": [
            Permission.read(Role.any())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "app_config", "gemini_api_key", 4096, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "app_config", "groq_api_key", 4096, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "app_config", "deepseek_api_key", 4096, False]}
        ],
        "indexes": []
    },
    "chats": {
        "name": "chats",
        "dls": True,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "chats", "chatId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "chats", "chatName", 128, False]},
            {"method": db.create_boolean_column, "args": [DATABASE_ID, "chats", "isGroup", False, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "chats", "participantIds", 36, True, None, True]}, # array=True
            {"method": db.create_text_column, "args": [DATABASE_ID, "chats", "adminId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "chats", "adminIds", 36, False, None, True]}, # array=True
            {"method": db.create_integer_column, "args": [DATABASE_ID, "chats", "createdAt", True]}
        ],
        "indexes": [
            {"key": "key_chatId", "type": "unique", "attrs": ["chatId"]}
        ]
    },
    "messages": {
        "name": "messages",
        "dls": True,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "messages", "chatId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "messages", "senderId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "messages", "type", 32, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "messages", "payload", 16384, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "messages", "wrappedKey", 16384, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "messages", "keyFingerprint", 64, False]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "messages", "timestamp", True]}
        ],
        "indexes": [
            {"key": "key_chatId", "type": "key", "attrs": ["chatId"]}
        ]
    },
    "notifications": {
        "name": "notifications",
        "dls": True,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "notifications", "targetUserId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "notifications", "senderId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "notifications", "title", 128, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "notifications", "body", 256, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "notifications", "data", 4096, True]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "notifications", "timestamp", True]}
        ],
        "indexes": [
            {"key": "key_targetUserId", "type": "key", "attrs": ["targetUserId"]}
        ]
    },
    "sync_requests": {
        "name": "sync_requests",
        "dls": True,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "sync_requests", "senderId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "sync_requests", "targetUserId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "sync_requests", "targetCollectionId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "sync_requests", "status", 20, False, "PENDING"]}
        ],
        "indexes": [
            {"key": "key_targetUser_status", "type": "key", "attrs": ["targetUserId", "status"]}
        ]
    },
    "shared_collections": {
        "name": "shared_collections",
        "dls": True,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "collectionId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "chatId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "name", 128, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "description", 512, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "downloadUrl", 512, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "symmetricKey", 2048, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "wrappedKeys", 16384, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "sharedBy", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "adminIds", 36, True, None, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_collections", "pendingDownloads", 36, False, None, True]},
            {"method": db.create_boolean_column, "args": [DATABASE_ID, "shared_collections", "isAdminOnly", False, False]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "shared_collections", "updatedAt", True]}
        ],
        "indexes": [
            {"key": "key_chatId", "type": "key", "attrs": ["chatId"]}
        ]
    },
    "access_requests": {
        "name": "access_requests",
        "dls": True,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "access_requests", "chatId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "access_requests", "collectionId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "access_requests", "requesterId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "access_requests", "status", 20, False, "PENDING"]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "access_requests", "timestamp", True]}
        ],
        "indexes": [
            {"key": "key_chatId_status", "type": "key", "attrs": ["chatId", "status"]}
        ]
    },
    "shared_sessions": {
        "name": "shared_sessions",
        "dls": True,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_sessions", "sessionId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_sessions", "chatId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_sessions", "title", 128, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_sessions", "downloadUrl", 512, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_sessions", "symmetricKey", 2048, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "shared_sessions", "adminIds", 36, True, None, True]},
            {"method": db.create_boolean_column, "args": [DATABASE_ID, "shared_sessions", "isAdminOnly", False, False]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "shared_sessions", "timestamp", True]}
        ],
        "indexes": [
            {"key": "key_chatId", "type": "key", "attrs": ["chatId"]}
        ]
    },
    "reported_questions": {
        "name": "reported_questions",
        "dls": False,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_questions", "reporterId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_questions", "reason", 1024, True]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "reported_questions", "reportedAt", True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_questions", "questionId", 36, True]}
        ],
        "indexes": []
    },
    "reported_collections": {
        "name": "reported_collections",
        "dls": False,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_collections", "reporterId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_collections", "reason", 1024, True]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "reported_collections", "reportedAt", True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_collections", "collectionId", 36, True]}
        ],
        "indexes": []
    },
    "reported_users": {
        "name": "reported_users",
        "dls": False,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_users", "reporterId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_users", "reason", 1024, True]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "reported_users", "reportedAt", True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_users", "reportedUserId", 36, True]}
        ],
        "indexes": []
    },
    "reported_groups": {
        "name": "reported_groups",
        "dls": False,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_groups", "reporterId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_groups", "reason", 1024, True]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "reported_groups", "reportedAt", True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_groups", "groupId", 36, True]}
        ],
        "indexes": []
    },
    "reported_messages": {
        "name": "reported_messages",
        "dls": False,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_messages", "reporterId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_messages", "reason", 1024, True]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "reported_messages", "reportedAt", True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_messages", "messageId", 36, True]}
        ],
        "indexes": []
    },
    "reported_sessions": {
        "name": "reported_sessions",
        "dls": False,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_sessions", "reporterId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_sessions", "reason", 1024, True]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "reported_sessions", "reportedAt", True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "reported_sessions", "sessionId", 36, True]}
        ],
        "indexes": []
    }
    ,
    "audit_logs": {
        "name": "audit_logs",
        "dls": False,
        "permissions": [
            Permission.create(Role.users())
        ],
        "attributes": [
            {"method": db.create_text_column, "args": [DATABASE_ID, "audit_logs", "eventType", 64, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "audit_logs", "collection", 64, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "audit_logs", "documentId", 36, True]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "audit_logs", "actorId", 36, False]},
            {"method": db.create_text_column, "args": [DATABASE_ID, "audit_logs", "details", 8192, False]},
            {"method": db.create_integer_column, "args": [DATABASE_ID, "audit_logs", "timestamp", True]}
        ],
        "indexes": []
    }
}

# --- EXECUTION ---
def wait_for_attributes(db_id, collection_id):
    print(f"Waiting for attributes in {collection_id} to be available...")
    while True:
        attrs = db.list_columns(db_id, collection_id)
        columns = getattr(attrs, "columns", None)
        if columns is None:
            columns = attrs.get("columns", [])

        statuses = []
        for col in columns:
            status = getattr(col, "status", None)
            if status is None:
                status = col.get("status")
            statuses.append(getattr(status, "value", status))

        all_available = all(status == "available" for status in statuses)
        if all_available:
            break
        time.sleep(1)

def setup_database():
    try:
        print(f"Creating Database: {DATABASE_NAME}")
        db.create(DATABASE_ID, DATABASE_NAME)
    except AppwriteException as e:
        if e.code == 409:
            print("Database already exists.")
        else:
            try:
                db.get(DATABASE_ID)
                print("Database already exists.")
            except AppwriteException:
                raise e

    for coll_id, config in collections.items():
        try:
            print(f"\n--- Creating Collection: {config['name']} ---")
            try:
                db.create_table(
                    DATABASE_ID,
                    coll_id,
                    config['name'],
                    permissions=config['permissions'],
                    row_security=config['dls']
                )
            except AppwriteException as e:
                if e.code == 409:
                    print("Collection already exists.")
                else:
                    raise e

            db.update_table(
                DATABASE_ID,
                coll_id,
                name=config['name'],
                permissions=config['permissions'],
                row_security=config['dls']
            )
            
            # Create Attributes
            for attr in config['attributes']:
                print(f"  Adding attribute: {attr['args'][2]}")
                try:
                    # Legacy scripts used a numeric "size" argument as the 4th
                    # positional parameter. Current Appwrite TablesDB column
                    # creation signatures do not take a size parameter. If a
                    # numeric size is present, drop it before calling the
                    # SDK to preserve backwards compatibility with the
                    # existing `collections` configuration.
                    call_args = list(attr['args'])
                    if len(call_args) >= 4 and type(call_args[3]) is int:
                        # Only remove a true integer size sentinel; don't treat
                        # booleans (subclass of int) as sizes.
                        call_args.pop(3)
                    attr['method'](*call_args)
                except AppwriteException as e:
                    if e.code != 409: raise e
            
            # Wait for Appwrite to process attributes
            wait_for_attributes(DATABASE_ID, coll_id)
            
            # Create Indexes
            for idx in config['indexes']:
                print(f"  Adding index: {idx['key']}")
                try:
                    db.create_index(DATABASE_ID, coll_id, idx['key'], idx['type'], idx['attrs'])
                except AppwriteException as e:
                    # Some SQL engines enforce a maximum index length; if the
                    # server rejects the index for being too large, retry with
                    # conservative per-column lengths (e.g. 191) so the index
                    # can be created as a prefix index.
                    msg = getattr(e, 'message', '') or str(e)
                    if 'Index length is longer' in msg:
                        try:
                            lengths = [191] * len(idx['attrs'])
                            print(f"    Index too long; retrying with lengths={lengths}")
                            db.create_index(DATABASE_ID, coll_id, idx['key'], idx['type'], idx['attrs'], lengths=lengths)
                        except AppwriteException:
                            raise
                    elif e.code != 409:
                        raise e

        except Exception as e:
            print(f"Error processing {coll_id}: {e}")

if __name__ == "__main__":
    setup_database()
    print("\n✅ Initialization Complete!")
