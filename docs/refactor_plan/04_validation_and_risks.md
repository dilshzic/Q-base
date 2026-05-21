# Validation and Risks

## Key Risks
1. Broken imports and package-private assumptions after moves.
2. Hilt binding resolution failures due to module relocation.
3. Navigation route breakage from UI package refactors.
4. Room/serialization issues if entities are moved without migration awareness.
5. Hidden coupling between app and core modules becoming compile blockers.

## Risk Controls
1. Move in thin vertical slices (one feature area at a time).
2. Keep temporary compatibility shims only where necessary and short-lived.
3. Run compile/test/lint after each migration slice.
4. Use IDE-assisted safe move/rename to preserve references.
5. Maintain a live migration map document with source->destination paths.

## Validation Checklist
- All modules compile in debug and release variants.
- Unit tests and instrumentation smoke tests pass.
- DI graph builds without missing bindings.
- Navigation flows verified manually:
  - auth
  - home/explore
  - sessions
  - chat detail
  - settings
- Sync pipelines validated with offline/online toggling.
- No unresolved TODOs from migration wrappers.

## Definition of Done
1. New package layout is adopted across app and core modules.
2. Root folder has clear separation for source, docs, tooling, reports, and ops artifacts.
3. Architecture docs reflect actual structure.
4. Team onboarding can locate any feature from folder names alone.

## Tracking Template
Use this lightweight table while executing:

| Source Path | Target Path | Status | Build Green |
|---|---|---|---|
| app/ui/chat | app/feature/chat/presentation | planned | no |
| app/data/sessions | app/feature/sessions/data | planned | no |
| app/ui/components | app/core/designsystem/components | planned | no |
