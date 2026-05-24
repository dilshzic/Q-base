# Post-Refactor Structure (Phase 6)

Date: 2026-05-24
Branch: refactor/phase-migration

Summary
- This document captures the post-refactor project structure, temporary adapters/wrappers discovered, package naming consistency notes, and an action plan to finish Phase 6 cleanup.

Module Layout
- app: Android application entrypoint and UI features. Depends on: `:core-chat`, `:core-auth`, `:core-crypto`.
- core-chat: Chat feature data, local/remote repos, sync orchestration.
- core-auth: Authentication + user/profile data + backend adapters.
- core-crypto: Crypto utilities and key management.

Observed temporary adapters / wrappers (candidates for removal or consolidation)
- `core-auth/src/main/java/.../AppwriteDatabaseImpl.kt` — contains Gson `TypeAdapter` inner classes (`AppwriteDocumentModel.Adapter`) used for JSON mapping. Verify whether these remain necessary or can be moved to a single serialization adapter location.
- `app/src/main/java/com/algorithmx/q_base/core/navigation/AppEntryWrappers.kt` — a set of small wrapper functions for navigation entry points. These are lightweight and may be retained as stable navigation adapters; mark for review before removal.
- UI temporary fields: usages of `temporaryAiExplanation` in content-import presentation layers. These are UI-only temporary state holders and should be renamed/removed once the AI feature is finalized.

Package naming consistency
- Root package used across modules is `com.algorithmx.q_base`.
- Core modules use `com.algorithmx.q_base.core.*` namespaces for internal APIs. No core -> app sideways imports were detected in the audit.
- Minor inconsistencies flagged during scans:
  - A small number of fully-qualified type errors were previously introduced and fixed (e.g., `com.algorithmx_q_base` underscored typo). None remain.

Docs and architecture diagrams to update
- `docs/refactor_plan/03_execution_phases.md` — phase status updated; archive old timelines into `docs/reports/generated/`.
- `docs/PROJECT_STRUCTURE_AND_INTEGRATION.md` — recommended to be updated with current module graph and dependency list (attached below).

Current module dependency graph (simplified)
- app -> core-chat, core-auth, core-crypto
- core-chat -> core-auth?, core-crypto
- core-auth -> core-crypto

Phase 6 Action Plan (safe order)
1. Create this post-refactor document and commit it. (done)
2. Mark candidate adapters/wrappers in code with TODO("REMOVE_IN_PHASE6") comments for review, do not delete yet.
3. Replace UI-only temporary fields (e.g., `temporaryAiExplanation`) with stable names or remove if unused; run incremental compile/tests after each change.
4. Consolidate serialization adapters (Gson/other) into `core-auth` or a new `core-serialization` module; update imports and run `./gradlew build`.
5. Remove wrapper files only after their call-sites are migrated or verified safe (compile + smoke tests pass).
6. Update architecture diagrams and `docs/PROJECT_STRUCTURE_AND_INTEGRATION.md` with the final module layout and publish.

Notes and safety
- Avoid large deletes in one commit. Prefer small, reviewable changes with a compile/build check after each.
- Keep the migration branch `refactor/phase-migration` alive for multi-commit work and PRs.

Artifacts produced
- This file: `docs/refactor_plan/04_post_refactor_structure.md`

Next recommended step (I can execute):
- Annotate the identified temporary fields/wrappers in source with `// TODO: REMOVE_IN_PHASE6` comments, run incremental compile, and then open a PR-ready commit.

If you want me to proceed automatically, confirm and I'll start by adding TODO comments to the candidate locations and running an incremental compile.