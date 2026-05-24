import os
from dotenv import load_dotenv
from appwrite.client import Client
from appwrite.services.databases import Databases
from appwrite.exception import AppwriteException

# Load environment variables from .env
load_dotenv()

ENDPOINT = os.getenv('APPWRITE_ENDPOINT', 'https://syd.cloud.appwrite.io/v1')
PROJECT_ID = os.getenv('APPWRITE_PROJECT_ID')
API_KEY = os.getenv('APPWRITE_API_KEY')
DATABASE_ID = os.getenv('APPWRITE_DATABASE_ID', 'qbase_db')
COLLECTION_ID = 'app_config'
DOCUMENT_ID = 'global_keys'

client = Client()
client.set_endpoint(ENDPOINT)
client.set_project(PROJECT_ID)
client.set_key(API_KEY)

databases = Databases(client)

def init_global_keys_doc():
    try:
        print(f"Checking if document '{DOCUMENT_ID}' exists in '{COLLECTION_ID}'...")
        databases.get_document(DATABASE_ID, COLLECTION_ID, DOCUMENT_ID)
        print(f"✅ Document '{DOCUMENT_ID}' already exists.")
    except AppwriteException as e:
        if e.code == 404:
            print(f"Document not found. Creating '{DOCUMENT_ID}' document...")
            try:
                databases.create_document(
                    database_id=DATABASE_ID,
                    collection_id=COLLECTION_ID,
                    document_id=DOCUMENT_ID,
                    data={
                        "gemini_api_key": "",
                        "groq_api_key": "",
                        "deepseek_api_key": ""
                    }
                )
                print("✅ Successfully initialized the 'global_keys' document with placeholders!")
            except Exception as ex:
                print(f"❌ Failed to create document: {ex}")
        else:
            print(f"❌ Error checking document: {e}")

if __name__ == "__main__":
    init_global_keys_doc()
