# Qbase Project Structure After Refactor (Planning Document)

This is a target-state structure document to be finalized after migration execution.

## Planned Structure

```text
Qbase/
  app/
    src/main/java/com/algorithmx/q_base/
      app/
      core/
        ai/
        navigation/
        designsystem/
        util/
      feature/
        auth/
          data/
          domain/
          presentation/
        chat/
          data/
          domain/
          presentation/
        sessions/
          data/
          domain/
          presentation/
        explore/
          data/
          domain/
          presentation/
        settings/
          data/
          domain/
          presentation/
        content_import/
          data/
          domain/
          presentation/
      sync/
        local/
        remote/
        orchestration/
      di/
  core-auth/
    src/main/java/com/algorithmx/q_base/
      auth/
      profile/
      backend/
      di/
  core-chat/
    src/main/java/com/algorithmx/q_base/
      chat/
        data/
        storage/
        network/
  core-crypto/
    src/main/java/com/algorithmx/q_base/
      crypto/
        keystore/
        envelope/
        exchange/
  docs/
    architecture/
    audits/
    implementation-plans/
    release-readiness/
    legal/
    refactor_plan/
  tooling/
    scripts/
    appwrite/
  reports/
    generated/
    snapshots/
  ops/
    diagnostics/
  archive/
```

## Ownership Model
- app: feature orchestration and UI/presentation.
- core-auth: identity/profile/auth backend integration.
- core-chat: chat persistence and messaging data handling.
- core-crypto: cryptographic operations and key management.
- docs/tooling/reports/ops: non-runtime artifacts separated for discoverability.

## Post-Migration Acceptance Criteria
1. Every Kotlin file belongs to one of: app feature, app core shared, sync, or core module domain.
2. Shared UI components live only in core/designsystem.
3. Root has no mixed generated report clutter.
4. Architecture diagram and module docs match actual folder tree.
