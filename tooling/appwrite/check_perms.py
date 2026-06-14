import os
from dotenv import load_dotenv
from appwrite.client import Client
from appwrite.services.databases import Databases

load_dotenv()
client = Client()
client.set_endpoint(os.getenv('APPWRITE_ENDPOINT', 'https://syd.cloud.appwrite.io/v1'))
client.set_project(os.getenv('APPWRITE_PROJECT_ID'))
client.set_key(os.getenv('APPWRITE_API_KEY'))

db = Databases(client)
database_id = os.getenv('APPWRITE_DATABASE_ID')

for col in ['messages', 'chats', 'shared_collections']:
    try:
        c = db.get_collection(database_id, col)
        print(f"--- {col} ---")
        print(f"DLS: {c['documentSecurity']}")
        print(f"Permissions: {c['$permissions']}")
    except Exception as e:
        print(f"Error for {col}: {e}")
