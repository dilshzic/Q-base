# Current State Inventory

## Root Observations
Current root contains application modules mixed with generated reports, archive assets, ad-hoc logs, and operational scripts.

Primary top-level buckets currently visible:
- Android app/modules: app, core-auth, core-chat, core-crypto
- Build system: gradle, gradlew, settings.gradle.kts, build.gradle.kts
- Docs and reports: docs plus multiple report files in root
- Tooling/scripts: scripts, Q_base appwrite
- Temporary/generated artifacts checked into workspace view: build, app/build, module build folders, logcat.txt, uidump.xml

## App Module Package Snapshot
Path: app/src/main/java/com/algorithmx/q_base

Top-level areas:
- core_ai
- data
- di
- ui
- util
- app entry files (MainActivity, QbaseApplication)

Known imbalance:
- ui package is broad and likely contains multiple features plus shared components.
- data package includes multiple concerns (ai, collections, sessions, sync, util, di) under one branch.

## Library Modules Snapshot
- core-auth:
  - data/auth
  - data/backend
  - data/di
  - ui/components
- core-chat:
  - data/chat
- core-crypto:
  - core_crypto

## Clarity Issues
1. Mixed concerns at root (product code, reports, runtime artifacts, legal html, migration docs).
2. app module packages are partly layer-first, partly feature-first.
3. Naming inconsistency across modules (for example core_crypto path style vs other package patterns).
4. Long-term docs and one-off planning docs are colocated without lifecycle grouping.
5. Operational appwrite workspace has a spaced directory name, reducing shell ergonomics and script portability.

## Reorganization Principle
Use feature-first organization at app level, keep shared cross-feature code explicit, and separate product source from operational/reporting artifacts.
