# Qbase Reorganization Master Plan

## Objective
Reorganize project folders for clarity, ownership, and lower coupling while preserving behavior.

## Scope Considered
- Root project layout, Gradle modules, app source packages, documentation, scripts, and appwrite helper workspace.
- Kotlin/Java source inventory sampled from:
  - app/src/main/java/com/algorithmx/q_base
  - core-auth/src/main/java
  - core-chat/src/main/java
  - core-crypto/src/main/java

## Baseline Facts
- Active Gradle modules: :app, :core-auth, :core-chat, :core-crypto.
- Approx source size in these modules: 190 Kotlin/Java files.
- app package concentration:
  - ui: 94 files
  - data: 60 files
  - core_ai: 10 files
  - util/di/app entry: small remainder

## Desired End State
- Feature-first app package structure with explicit layer boundaries per feature.
- Cross-cutting concerns moved to dedicated core/common packages.
- Documentation grouped by domain and lifecycle stage.
- Scripts and operational assets separated from application source.

## Split Documents
1. 01_current_state_inventory.md
2. 02_target_folder_blueprint.md
3. 03_execution_phases.md
4. 04_validation_and_risks.md
5. ../PROJECT_STRUCTURE_AFTER_REFACTOR_PLAN.md

## Non-Goals
- No functional refactor in this planning step.
- No package renames executed in this planning step.
- No module creation/removal executed in this planning step.
