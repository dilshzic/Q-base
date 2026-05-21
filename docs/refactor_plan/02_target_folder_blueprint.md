# Target Folder Blueprint

## Root-Level Target Layout (Planning)

```text
Qbase/
  app/
  core-auth/
  core-chat/
  core-crypto/
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
  archive/
  ops/
    diagnostics/
  gradle/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  firestore.rules
```

## app Module Target Package Layout (Planning)

```text
app/src/main/java/com/algorithmx/q_base/
  app/
    QbaseApplication.kt
    MainActivity.kt
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
```

## Mapping Guidance From Current Packages
- ui/auth -> feature/auth/presentation
- ui/chat and ui/chat/components -> feature/chat/presentation
- ui/sessions and ui/sessions/components -> feature/sessions/presentation
- ui/explore -> feature/explore/presentation
- ui/settings and ui/settings/components -> feature/settings/presentation
- ui/content_import -> feature/content_import/presentation
- ui/components and ui/components/reusable -> core/designsystem
- ui/navigation -> core/navigation
- ui/theme -> core/designsystem/theme
- data/collections -> feature/content_import/data or feature/sessions/data (split by ownership)
- data/sessions -> feature/sessions/data
- data/ai and core_ai -> core/ai
- data/sync -> sync/orchestration (+ subpackages by source)
- util -> core/util

## Module-Level Conventions
- core-auth: keep authentication and profile concerns only; no feature UI beyond reusable auth UI contracts.
- core-chat: keep chat entities/dao/repositories and pure chat data concerns.
- core-crypto: keep cryptographic primitives/managers only.
- app: keep feature orchestration and presentation, avoid deep infra details.

## Naming Standards
- Use lowercase snake-free package segments (example: corecrypto, not core_crypto).
- Prefer feature names over generic layer buckets at top package level.
- Keep one source of truth for shared UI in core/designsystem.
