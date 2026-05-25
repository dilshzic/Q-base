import hashlib
import os
from dotenv import load_dotenv
from appwrite.client import Client
from appwrite.permission import Permission
from appwrite.role import Role
from appwrite.query import Query
from appwrite.services.tables_db import TablesDB
from appwrite.exception import AppwriteException

load_dotenv()

ENDPOINT = os.getenv("APPWRITE_ENDPOINT", "https://syd.cloud.appwrite.io/v1")
PROJECT_ID = os.getenv("APPWRITE_PROJECT_ID")
API_KEY = os.getenv("APPWRITE_API_KEY")
DATABASE_ID = os.getenv("APPWRITE_DATABASE_ID", "qbase_db")
USERS_TABLE = "users"
PAGE_SIZE = 100

if not PROJECT_ID or not API_KEY:
    raise RuntimeError("Missing APPWRITE_PROJECT_ID or APPWRITE_API_KEY")

client = Client()
client.set_endpoint(ENDPOINT)
client.set_project(PROJECT_ID)
client.set_key(API_KEY)

tables = TablesDB(client)


def user_permissions(uid: str) -> list[str]:
    return [
        Permission.read(Role.users()),
        Permission.write(Role.user(uid)),
        Permission.update(Role.user(uid)),
        Permission.delete(Role.user(uid)),
    ]


def as_dict(obj):
    if isinstance(obj, dict):
        return obj
    return obj.__dict__ if hasattr(obj, "__dict__") else {}


def get_row_id(row) -> str:
    return getattr(row, "id", None) or as_dict(row).get("$id") or as_dict(row).get("id") or ""


def get_row_data(row) -> dict:
    data = getattr(row, "data", None)
    if isinstance(data, dict):
        return data
    return as_dict(row).get("data", {}) or {}


def fallback_friend_code(uid: str) -> str:
    digest = hashlib.sha256(uid.encode("utf-8")).hexdigest().upper()
    return f"QBS-{digest[:4]}-{digest[4:8]}"


def pick_first_non_blank(*values):
    for value in values:
        if isinstance(value, str) and value.strip():
            return value.strip()
    return ""


def list_all_rows() -> list:
    rows = []
    offset = 0
    while True:
        page = tables.list_rows(
            database_id=DATABASE_ID,
            table_id=USERS_TABLE,
            queries=[Query.limit(PAGE_SIZE), Query.offset(offset)],
        )
        page_rows = getattr(page, "rows", None) or as_dict(page).get("rows", [])
        if not page_rows:
            break
        rows.extend(page_rows)
        if len(page_rows) < PAGE_SIZE:
            break
        offset += PAGE_SIZE
    return rows


def merge_rows(rows_for_uid: list, uid: str) -> tuple[dict, list[str]]:
    merged = {
        "userId": uid,
        "displayName": "",
        "profilePictureUrl": "",
        "friendCode": "",
        "intro": "",
        "publicKey": "",
        "isBanned": False,
        "isPhotoVisible": True,
    }
    row_ids = []

    for row in rows_for_uid:
        row_id = get_row_id(row)
        if row_id:
            row_ids.append(row_id)
        data = get_row_data(row)
        merged["displayName"] = pick_first_non_blank(merged["displayName"], data.get("displayName"))
        merged["profilePictureUrl"] = pick_first_non_blank(merged["profilePictureUrl"], data.get("profilePictureUrl"))
        merged["friendCode"] = pick_first_non_blank(merged["friendCode"], data.get("friendCode"))
        merged["intro"] = pick_first_non_blank(merged["intro"], data.get("intro"))
        merged["publicKey"] = pick_first_non_blank(
            merged["publicKey"],
            data.get("publicKey"),
            data.get("public_key"),
            data.get("e2eePublicKey"),
        )
        if isinstance(data.get("isBanned"), bool):
            merged["isBanned"] = data["isBanned"]
        if isinstance(data.get("isPhotoVisible"), bool):
            merged["isPhotoVisible"] = data["isPhotoVisible"]

    if not merged["displayName"]:
        merged["displayName"] = f"User {uid[:6]}"
    if not merged["friendCode"]:
        merged["friendCode"] = fallback_friend_code(uid)

    return merged, row_ids


def repair_users_rows():
    all_rows = list_all_rows()
    by_uid: dict[str, list] = {}

    for row in all_rows:
        row_id = get_row_id(row)
        data = get_row_data(row)
        uid = pick_first_non_blank(data.get("userId"), row_id)
        if not uid:
            continue
        by_uid.setdefault(uid, []).append(row)

    repaired = 0
    normalized_ids = 0
    unresolved_public_keys = []

    for uid, rows_for_uid in by_uid.items():
        merged, row_ids = merge_rows(rows_for_uid, uid)
        try:
            tables.upsert_row(
                database_id=DATABASE_ID,
                table_id=USERS_TABLE,
                row_id=uid,
                data=merged,
                permissions=user_permissions(uid),
            )
            repaired += 1
        except AppwriteException as e:
            print(f"[ERROR] Failed to upsert canonical row for {uid}: code={e.code} message={e.message}")
            continue

        for row_id in set(row_ids):
            if row_id and row_id != uid:
                try:
                    tables.delete_row(
                        database_id=DATABASE_ID,
                        table_id=USERS_TABLE,
                        row_id=row_id,
                    )
                    normalized_ids += 1
                except AppwriteException as e:
                    print(f"[WARN] Failed to delete legacy row {row_id} for {uid}: code={e.code} message={e.message}")

        if not merged["publicKey"]:
            unresolved_public_keys.append(uid)

    print("=== users row repair summary ===")
    print(f"Users repaired: {repaired}")
    print(f"Legacy row IDs removed: {normalized_ids}")
    print(f"Users still missing publicKey: {len(unresolved_public_keys)}")
    if unresolved_public_keys:
        print("These users must sign in on latest app to publish key:")
        for uid in unresolved_public_keys:
            print(f" - {uid}")


if __name__ == "__main__":
    repair_users_rows()
