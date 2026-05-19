#!/usr/bin/env python3
"""
Appwrite function: validate_admin_writes

This function is intended as a best-effort Appwrite Function that runs on database
document create/update events. It checks writes to `shared_collections` and
`shared_sessions` and ensures the acting user is listed in the referenced chat's
`adminIds`. If a write appears unauthorized, the function writes an `audit_logs`
entry and attempts to delete the offending document.

Deployment: create an Appwrite function with runtime python-3.10, set it to trigger
on database collection events for the relevant collections, and grant the function
an API key via environment variable APPWRITE_API_KEY.
"""

import os
import sys
import json
import time
from appwrite.client import Client
from appwrite.services.tables_db import TablesDB
from appwrite.exception import AppwriteException

def read_stdin():
    try:
        data = sys.stdin.read()
        if not data:
            return None
        return json.loads(data)
    except Exception:
        return None

def make_client():
    client = Client()
    endpoint = os.getenv('APPWRITE_FUNCTION_ENDPOINT') or os.getenv('APPWRITE_ENDPOINT')
    project = os.getenv('APPWRITE_FUNCTION_PROJECT_ID') or os.getenv('APPWRITE_PROJECT_ID')
    key = os.getenv('APPWRITE_FUNCTION_API_KEY') or os.getenv('APPWRITE_API_KEY')
    if endpoint:
        client.set_endpoint(endpoint)
    if project:
        client.set_project(project)
    if key:
        client.set_key(key)
    return client

def main():
    payload = read_stdin()
    if not payload:
        print('No event payload received')
        return

    client = make_client()
    db = TablesDB(client)

    # Try to extract useful fields from common event shapes
    # Appwrite publishes different event shapes; be defensive.
    event_type = payload.get('events') or payload.get('event') or payload.get('type')
    data = payload.get('payload') or payload.get('document') or payload.get('data') or payload

    collection = None
    document_id = None
    actor_id = None
    chat_id = None

    # Attempt common keys
    if isinstance(data, dict):
        collection = data.get('collectionId') or data.get('collection')
        document_id = data.get('documentId') or data.get('id') or data.get('$id')
        actor_id = data.get('userId') or data.get('actor') or os.getenv('APPWRITE_FUNCTION_USER_ID')
        chat_id = data.get('chatId') or data.get('chat')

    # Fallback to scanning nested structures
    if not collection:
        # payload may include resource/collection
        collection = payload.get('resource', {}).get('collection') if isinstance(payload.get('resource'), dict) else None

    # Only enforce for these collections
    if collection not in ('shared_collections', 'shared_sessions'):
        print('Event not for shared collections/sessions; skipping enforcement')
        # Still attempt to log
        try:
            db.create_document(os.getenv('APPWRITE_DATABASE_ID'), 'audit_logs', json.dumps({
                'eventType': str(event_type),
                'collection': str(collection),
                'documentId': str(document_id),
                'actorId': str(actor_id),
                'details': json.dumps(data)
            }), permissions=[])
        except Exception:
            pass
        return

    # If we don't have actor id or chat id, log and exit
    if not actor_id or not chat_id:
        print('Missing actor_id or chat_id; logging and skipping enforcement')
        try:
            db.create_document(os.getenv('APPWRITE_DATABASE_ID'), 'audit_logs', json.dumps({
                'eventType': 'missing_fields',
                'collection': str(collection),
                'documentId': str(document_id),
                'actorId': str(actor_id),
                'details': json.dumps(data)
            }), permissions=[])
        except Exception:
            pass
        return

    # Fetch chat document and check adminIds
    try:
        chat_doc = db.get_document(os.getenv('APPWRITE_DATABASE_ID'), 'chats', chat_id)
        chat_admins = getattr(chat_doc, 'adminIds', None) or chat_doc.get('adminIds') if isinstance(chat_doc, dict) else None
        # Normalize admin list
        if isinstance(chat_admins, str):
            try:
                admin_list = json.loads(chat_admins)
            except Exception:
                admin_list = [s.strip() for s in chat_admins.split(',') if s.strip()]
        else:
            admin_list = list(chat_admins) if chat_admins else []
    except AppwriteException as e:
        print('Failed to fetch chat document:', e)
        admin_list = []

    if actor_id not in admin_list:
        # Unauthorized write: log and attempt to remove the document
        details = {
            'reason': 'actor_not_in_admins',
            'actorId': actor_id,
            'adminList': admin_list,
            'payload': data
        }
        try:
            db.create_document(os.getenv('APPWRITE_DATABASE_ID'), 'audit_logs', json.dumps({
                'eventType': 'unauthorized_write',
                'collection': collection,
                'documentId': document_id,
                'actorId': actor_id,
                'details': json.dumps(details),
                'timestamp': int(time.time() * 1000)
            }), permissions=[])
        except Exception as e:
            print('Failed to write audit log:', e)

        # Attempt to delete the offending document
        try:
            if document_id:
                db.delete_document(os.getenv('APPWRITE_DATABASE_ID'), collection, document_id)
                print('Deleted unauthorized document', document_id)
        except Exception as e:
            print('Failed to delete unauthorized document:', e)

    else:
        print('Actor is admin; write allowed')

if __name__ == '__main__':
    main()
