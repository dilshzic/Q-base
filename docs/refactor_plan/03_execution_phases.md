# Execution Phases

## Phase 0: Safety Net and Freeze
1. Freeze feature development for touched packages.
2. Ensure clean baseline build and test pass.
3. Record baseline metrics:
   - compile time
   - APK size
   - startup timing
   - lint/test status
4. Create branch for structural migration.

## Phase 1: Root Folder Hygiene
1. Move root reports into reports/generated or reports/snapshots.
2. Move ad-hoc logs and dumps into ops/diagnostics.
3. Move scripts and appwrite helpers into tooling/scripts and tooling/appwrite.
4. Group docs into architecture, audits, implementation-plans, release-readiness, legal.
5. Keep compatibility aliases (README notes and temporary symlink strategy if needed).

## Phase 2: app Presentation Re-homing
1. Create target packages under feature/*/presentation and core/designsystem.
2. Move ui files feature by feature (auth -> chat -> sessions -> explore -> settings -> content_import).
3. Move shared composables from ui/components to core/designsystem/components.
4. Keep wrappers/redirect imports temporarily where risky.
5. Compile after each feature slice.

## Phase 3: app Data and Sync Re-homing
1. Split data by feature ownership.
2. Move cross-feature sync code into sync/orchestration.
3. Keep AI-specific code under core/ai.
4. Refactor package names and imports incrementally.
5. Re-run compile and focused tests after each sub-step.

## Phase 4: DI Realignment
1. Re-locate DI modules close to owning feature/core areas.
2. Keep app-wide component wiring in top-level di.
3. Remove redundant/obsolete DI bindings.
4. Validate Hilt graph builds across variants.

## Phase 5: Module Boundary Tightening
1. Audit cross-module dependencies (app -> core-* only where required).
2. Remove accidental sideways dependencies.
3. Optionally add new modules only if package movement exposes stable seams:
   - core-sync (optional)
   - core-ui-designsystem (optional)
4. Keep this optional unless compile and ownership metrics justify it.

## Phase 6: Cleanup and Documentation
1. Remove temporary adapters/wrappers.
2. Enforce package naming consistency.
3. Update all docs references and architecture diagrams.
4. Publish post-refactor structure document.

## Recommended Commit Cadence
1. chore(structure): root docs/reports/tooling move
2. refactor(app): move auth presentation packages
3. refactor(app): move chat presentation packages
4. refactor(app): move sessions/explore/settings/content_import
5. refactor(app): split data + sync packages
6. refactor(di): align hilt modules to ownership
7. docs(architecture): publish final project structure

## Build/Validation Gate Per Commit
- assembleDebug
- testDebugUnitTest
- lintDebug
- critical instrumentation smoke tests
