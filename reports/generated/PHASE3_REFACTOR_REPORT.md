Phase 3 Refactor Inventory & Plan
=================================

Date: 2026-05-22

Summary
-------
This report captures the Phase 3 inventory (data, sync, AI) and provides a concrete execution plan to re-home data and sync code per the refactor objectives.

Inventory (selected paths)
--------------------------
- Data (collections, DAOs, entities)
  - app/src/main/java/com/algorithmx/q_base/data/collections/
    - CollectionDao.kt
    - QuestionDao.kt
    - ProblemReportDao.kt
  - app/src/main/java/com/algorithmx/q_base/feature/content_import/data/
    - Question.kt, Answer.kt, StudyCollection.kt (includes StudyCollectionWithCount), ImportRepository.kt, ExploreRepository.kt, etc.
  - app/src/main/java/com/algorithmx/q_base/core/ai/data/
    - BrainUsageEntity.kt
    - BrainUsageDao.kt
    - AiResponseEntity.kt, AiResponseDao.kt
  - app/src/main/java/com/algorithmx/q_base/core/data/
    - AppDatabase.kt, DatabaseModule.kt, HomeRepository.kt, ConfigRepository.kt

- Sync / Orchestration
  - app/src/main/java/com/algorithmx/q_base/sync/orchestration/
    - CollectionSyncRepository.kt
    - CollectionSyncFileExtensions.kt
    - CollectionSyncPatchExtensions.kt
    - MessageSyncRepository.kt
    - MessageSyncIncomingExtensions.kt
    - MessageSyncOutgoingExtensions.kt (defines `MissingEncryptionKeysException`)
    - PatchEvent.kt, SyncRequest.kt, UniversalQueueManager.kt, OfflineActionEntity.kt

- Generated artifacts (KSP / Hilt / Room)
  - app/build/generated/ksp/debug/
  - app/build/generated/hilt/component_trees/debug/
  - app/build/generated/ksp/debug/kotlin/com/algorithmx/q_base/data/collections/CollectionDao_Impl.kt
  - Room-generated DAO implementations reference `com.algorithmx.q_base.data.collections.StudyCollectionWithCount`.

Findings / Risks
----------------
1. Duplicate/ambiguous DTOs: `StudyCollectionWithCount` appears in `feature/content_import` and is expected by generated code under `data.collections` — canonicalization needed.
2. Sync code references types across packages; moving data types requires updating imports and generated KSP outputs (re-run KSP after changes).
3. `MissingEncryptionKeysException` is defined in `sync/orchestration/MessageSyncOutgoingExtensions.kt`; callers already import that package, so keep exception where it is or centralize if required.

Phase 3 Execution Plan (stepwise)
---------------------------------
1. Canonicalize data models
   - Move or alias DTOs to `com.algorithmx.q_base.data.collections` (canonical location for DB/view models).
   - Update all consumers' imports (`HomeRepository`, `ExploreRepository`, ViewModels, UI screens).
   - Re-run `./gradlew :app:kspDebugKotlin` to regenerate DAO/Room code and surface missing types.

2. Re-home sync/orchestration
   - Move cross-feature sync code into `sync/orchestration` (already present) and ensure package declarations are `com.algorithmx.q_base.sync.orchestration`.
   - Update callers across features to import from the new sync package.
   - Re-run KSP/compile after this slice.

3. AI-specific code
   - Ensure AI data and modules remain under `core/ai` (currently in `core/ai/data` and `core/ai/di`).
   - Update DI modules to reference canonical AI classes.

4. DI realignment for data
   - Move `DatabaseModule` and other DB providers to `core/data/di` and ensure Hilt modules are placed close to owning code.
   - Validate Hilt aggregated dependencies via `:app:kspDebugKotlin` and `:app:compileDebugKotlin`.

5. Validation & Cleanup
   - For each sub-step: run `./gradlew :app:kspDebugKotlin` then `./gradlew :app:compileDebugKotlin`.
   - Run `./gradlew :app:assembleDebug` once the full phase is complete.
   - Remove temporary adapters/wrappers created to bridge old/new packages.

Developer checklist for each slice
----------------------------------
- Update package declarations in moved files.
- Update imports in every consumer file (use search/replace limited to the feature slice).
- Re-run KSP and compilation; fix errors iteratively.
- Commit in small, named commits following cadence: `refactor(data): move X to data.collections`.

Next immediate action (I can perform):
- Create a patch to move remaining `StudyCollectionWithCount` (if still duplicated) into the canonical `app/src/main/java/com/algorithmx/q_base/data/collections` and update consumer imports, then run `:app:kspDebugKotlin` and `:app:compileDebugKotlin` to verify.

Report saved to: `reports/generated/PHASE3_REFACTOR_REPORT.md`

If you want me to start executing Phase 3 slices now, tell me which slice to start ("data models" / "sync" / "AI" / "DI").
