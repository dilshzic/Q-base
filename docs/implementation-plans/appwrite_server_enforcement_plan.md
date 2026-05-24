# Appwrite Server-Side Enforcement Plan

Date: 2026-05-19

Purpose
- Capture a concrete, staged plan to implement server-side enforcement for `isAdminOnly` shared collection/session writes in Appwrite. This document is a future-playbook: deployable steps, required artifacts, testing checklist, rollback, and ownership.

Goals
- Ensure only chat admins may create/update documents that set or modify `isAdminOnly` or publish updates to `shared_collections` and `shared_sessions`.
- Provide an auditable server-side logging trail for grant/rescind and unauthorized attempts.
- Keep client-side checks for UX but make the server the source of truth.

Scope
- Appwrite backend (Tables DB / Functions). Does not include Firestore rules here.

High-level options (tradeoffs)
- Option A (Preferred if supported): Pre-write enforcement via Appwrite row-level security or server-side rules (prevents unauthorized write before it is persisted). This is most secure but depends on Appwrite capabilities and your deployment model.
- Option B (Practical + recommended): Use an Appwrite Function triggered on create/update events for `shared_collections` and `shared_sessions` that validates the actor against the chat's `adminIds`, writes an `audit_logs` entry, and (for unauthorized writes) reverts/removes the document. This is reactive but easy to deploy.
- Option C (Server-side middleware): Require privileged operations (changing `isAdminOnly`, publishing) to go through a server-side API (service) that enforces authorization before writing to Appwrite. Most robust but needs extra infra.

Current repo status
- Client: `adminIds` moved to `List<String>` and Room TypeConverters implemented (`core-chat/src/.../ChatEntity.kt`, `ChatTypeConverters.kt`). Client checks remain in UI (e.g. `SharedLibraryView.kt`).
- Appwrite: `app_write.py` updated (many `create_text_column` changes). An `audit_logs` collection and `validate_admin_writes` function were added under `Q_base appwrite/`.

Planned implementation (recommended path / Option B -> later upgrade to Option A)

1) Prepare Appwrite environment
- Ensure Appwrite project id, endpoint and service API key are available. Store them in the `Q_base appwrite/.env` or set in CI/CD secrets.
- Required env vars (examples):

  - `APPWRITE_ENDPOINT` (e.g. https://cloud.appwrite.io/v1)
  - `APPWRITE_PROJECT_ID`
  - `APPWRITE_API_KEY` (service key used by `app_write.py` and by deployment steps)
  - `APPWRITE_FUNCTION_API_KEY` (service key for functions to call admin APIs)

2) Database schema
- Run (or re-run) `Q_base appwrite/app_write.py` from the `Q_base appwrite` folder to create/update collections. Note: ensure `app_write.py` matches your Appwrite SDK version — we replaced deprecated `create_string_column` with `create_text_column` in the helper.
- Confirm the following collections exist and have expected attributes:
  - `shared_collections` (fields: `collectionId`, `chatId`, `adminIds` (array/text), `isAdminOnly` boolean, `wrappedKeys`, etc.)
  - `shared_sessions` (similar fields)
  - `audit_logs` (fields: `eventType`, `collection`, `documentId`, `actorId`, `details`, `timestamp`)

3) Deploy enforcement function (reactive)
- Function artifact: `Q_base appwrite/functions/validate_admin_writes.zip` contains `main.py` implementing validation and audit-logging.
- Create Appwrite Function (runtime: python-3.10) and upload the ZIP via Console or `appwrite` CLI.
- Set environment variables for the Function: `APPWRITE_FUNCTION_API_KEY` (service key), `APPWRITE_PROJECT_ID`, `APPWRITE_ENDPOINT`, `APPWRITE_DATABASE_ID`.
- Configure triggers: enable on the following events:
  - `collections.shared_collections.documents.create`
  - `collections.shared_collections.documents.update`
  - `collections.shared_sessions.documents.create`
  - `collections.shared_sessions.documents.update`

4) Function behavior (what `validate_admin_writes` does)
- On event, extract `actorId` (user who triggered the event) and `chatId` from the document payload.
- Fetch `chats` table row for `chatId` and normalize `adminIds` to a list.
- If `actorId` ∉ adminIds:
  - Create an `audit_logs` entry with `eventType=unauthorized_write` and the payload details.
  - Attempt to delete the offending document (best-effort). Log failures.
- If authorized: create an `audit_logs` entry with `eventType=authorized_write` (optional) and let the write stand.

5) Deploy testing plan
- Use a staging Appwrite project.
- Tests to run:
  1. Admin user creates shared collection with `isAdminOnly=true` → write allowed; `audit_logs` entry present.
  2. Non-admin user attempts same write → write created (since function is reactive) then the function should delete it and write `unauthorized_write` to `audit_logs`. Confirm deletion and log.
  3. Simulate function failure: verify audit entries indicate failure and follow rollback runbook.

6) Monitoring and alerts
- Configure function logs and a simple alert (e.g., daily check for `audit_logs` unauthorized events). Consider integrating with PagerDuty/Slack for repeated unauthorized attempts.

7) Migration & hardening (future / Option A)
- If Appwrite supports pre-write row-level security or attribute-level preconditions in your deployment, migrate enforcement to a pre-write rule that rejects the request before persistence. The rule must evaluate `actorId ∈ chat.adminIds`.
- Alternatively provide a server-side service API (trusted) that clients must call for privileged operations; only that service uses the Appwrite service key to write.

8) Rollout and rollback
- Rollout: deploy function to staging → smoke test → deploy to production in low-traffic window.
- Rollback: disable function triggers immediately (or unpublish function) and alert team; review `audit_logs` for missed unauthorized writes during downtime.

9) Ownership and timeline
- Owner: Backend/Platform team (name/role).
- Timeline (estimate):
  - Function deployment + testing: 1 day
  - Staged rollout and monitoring: 1 day
  - Optional pre-write rules or server API: 2–3 days

10) Notes & caveats
- Reactive function can remove unauthorized writes but cannot prevent short-lived exposure or race conditions; pre-write enforcement is preferred.
- Ensure system clocks and timestamp units are consistent (we use epoch ms in `audit_logs`).
- Keep all service keys in a secure secrets store; never commit them to repo.

Artifacts in repo (where to look)
- Appwrite function: `Q_base appwrite/functions/validate_admin_writes/main.py` and `functions/validate_admin_writes.zip`.
- Appwrite schema helper: `Q_base appwrite/app_write.py` (updated to use `create_text_column`).
- Client changes: `core-chat/src/main/java/com/algorithmx/q_base/data/chat/ChatEntity.kt`, `ChatTypeConverters.kt`.
- Audit report: `docs/shared_collection_admin_only_audit.md` and `docs/shared_collection_admin_only_audit.pdf`.

Next actions (pick one)
- (A) I can produce a ready-to-paste Appwrite Function deployment guide with exact Console/CLI steps and environment variable examples. 
- (B) I can draft example pre-write rule snippets for Appwrite/Firestore (if you want to target Firestore instead). 
- (C) I can create a PR with all code changes and this doc included.
