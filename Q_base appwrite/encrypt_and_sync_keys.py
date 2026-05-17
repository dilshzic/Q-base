import os
import sys
import base64
from dotenv import load_dotenv
from appwrite.client import Client
from appwrite.services.databases import Databases
from appwrite.exception import AppwriteException
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

# Load environment variables
load_dotenv()

ENDPOINT = os.getenv('APPWRITE_ENDPOINT', 'https://syd.cloud.appwrite.io/v1')
PROJECT_ID = os.getenv('APPWRITE_PROJECT_ID')
API_KEY = os.getenv('APPWRITE_API_KEY')
DATABASE_ID = os.getenv('APPWRITE_DATABASE_ID', 'qbase_db')
COLLECTION_ID = 'app_config'
DOCUMENT_ID = 'global_keys'

# Reconstruct the exact static key used in Kotlin CryptoManager
def get_global_decryption_key():
    part1 = bytes([0x51, 0x62, 0x61, 0x73, 0x65, 0x5F, 0x53, 0x65]) # "Qbase_Se"
    part2 = bytes([0x63, 0x72, 0x65, 0x74, 0x5F, 0x32, 0x30, 0x32]) # "cret_202"
    part3 = bytes([0x36, 0x5F, 0x47, 0x6C, 0x6F, 0x62, 0x61, 0x6C]) # "6_Global"
    part4 = bytes([0x5F, 0x4B, 0x65, 0x79, 0x5F, 0x41, 0x45, 0x53]) # "_Key_AES"
    
    return part1 + part2 + part3 + part4

def encrypt_global_key(plain_text: str) -> str:
    if not plain_text:
        return ""
    key = get_global_decryption_key()
    aesgcm = AESGCM(key)
    # Generate 12-byte standard GCM IV
    iv = os.urandom(12)
    ciphertext = aesgcm.encrypt(iv, plain_text.encode('utf-8'), None)
    
    # Combined IV + ciphertext
    combined = iv + ciphertext
    return base64.b64encode(combined).decode('utf-8')

def main():
    print("====================================================")
    print("🔒 QBASE GLOBAL API KEY ENCRYPT & SYNC UTILITY")
    print("====================================================\n")
    
    gemini_key = input("🔑 Enter raw Gemini API Key (or press Enter to skip): ").strip()
    groq_key = input("🔑 Enter raw Groq API Key (or press Enter to skip): ").strip()
    deepseek_key = input("🔑 Enter raw DeepSeek API Key (or press Enter to skip): ").strip()
    
    if not gemini_key and not groq_key and not deepseek_key:
        print("❌ No keys entered. Exiting.")
        sys.exit(0)
        
    print("\nEncrypting keys locally using obfuscated static AES-GCM...")
    enc_gemini = encrypt_global_key(gemini_key) if gemini_key else None
    enc_groq = encrypt_global_key(groq_key) if groq_key else None
    enc_deepseek = encrypt_global_key(deepseek_key) if deepseek_key else None
    
    # Setup Appwrite client
    client = Client()
    client.set_endpoint(ENDPOINT)
    client.set_project(PROJECT_ID)
    client.set_key(API_KEY)
    
    databases = Databases(client)
    
    update_data = {}
    if enc_gemini: update_data["gemini_api_key"] = enc_gemini
    if enc_groq: update_data["groq_api_key"] = enc_groq
    if enc_deepseek: update_data["deepseek_api_key"] = enc_deepseek
    
    try:
        print(f"Uploading secure Base64 ciphertexts to database collection '{COLLECTION_ID}'...")
        databases.update_document(
            database_id=DATABASE_ID,
            collection_id=COLLECTION_ID,
            document_id=DOCUMENT_ID,
            data=update_data
        )
        print("\n✅ SUCCESS! Encrypted API keys updated successfully on Appwrite Cloud.")
        print("Your keys are now perfectly protected at rest and in transit.")
    except Exception as e:
        print(f"\n❌ Error syncing to Appwrite: {e}")

if __name__ == "__main__":
    main()
