# QuestionViewer Ask AI -> Interactive Chat Bottom Sheet (Plan)

Status: Planning artifact only. No implementation changes included in this plan file.

## Goal
Replace the current static AI response bottom sheet (single generated markdown answer) triggered from `QuestionViewer.onAskAi` with an interactive Material 3 `ModalBottomSheet` chat experience.

## Current Behavior (Baseline)
1. `QuestionViewer` exposes `onAskAi` callback and renders an `Ask AI` button.
2. `ExploreQuestionPagerScreen` passes `onAskAi(page, "EXPLAIN")` into `QuestionViewer`.
3. `ExploreViewModel.askAi(...)` generates one-off AI text and stores it into `ExploreQuestionState.aiResponse`.
4. `ExploreQuestionPagerScreen` opens a bottom sheet when `aiResponse != null`, showing markdown + save/close actions.

## Target Behavior
1. Tapping `Ask AI` opens an interactive `ModalBottomSheet`.
2. Bottom sheet presents conversational AI chat scoped to the current question context.
3. First message is seeded with question stem/options context.
4. User can send follow-up prompts in the same sheet.
5. Sheet supports swipe-down dismissal and uses `imePadding()` for keyboard safety.

## Proposed Architecture

### State Ownership
- Keep sheet visibility and selected question context in `ExploreQuestionPagerScreen` (parent), not in `QuestionViewer`.
- Keep `QuestionViewer` as a stateless trigger-only UI element.

### Data Flow
1. `QuestionViewer.onAskAi` -> parent sets `showAiChatSheet = true` and captures active question payload.
2. Parent invokes AI chat session bootstrap via `ChatViewModel` (preferred) when sheet opens.
3. Bottom sheet displays chat transcript + input bound to AI chat state.
4. Sending message routes through existing `ChatViewModel.sendMessage(...)` to reuse moderation, queueing, E2EE-safe path, and feedback events.

### Component Reuse Strategy
- Reuse from chat module where possible:
  - `MessageBubble` from `feature/chat/presentation/components/MessageBubble.kt` for transcript rows.
  - Styling tokens from `ChatDetailComponents.kt` for input/actions.
- Do not embed full `ChatDetailScreen` in the sheet (too much unrelated behavior and side effects).
- Build a focused `AiChatBottomSheet` composable that consumes chat VM state.

## File-Level Implementation Plan

### 1) Add bottom-sheet composable
Create:
- `app/src/main/java/com/algorithmx/q_base/feature/explore/presentation/AiChatBottomSheet.kt`

Responsibilities:
- Render `ModalBottomSheet`.
- Header: short question context preview.
- Body: message list (AI + user) using `MessageBubble`.
- Footer: text input + send button.
- Apply `Modifier.imePadding()`.

### 2) Update Explore screen to use interactive sheet
Update:
- `app/src/main/java/com/algorithmx/q_base/feature/explore/presentation/ExploreScreens.kt`

Changes:
- Replace current static `aiResponse` modal section with `AiChatBottomSheet` host logic.
- Add sheet state variables:
  - `showAiChatSheet: Boolean`
  - `selectedQuestionContext: QuestionContextPayload`
- `onAskAi` from `QuestionViewer` opens sheet instead of requesting one-off generation.

### 3) Bridge with existing ChatViewModel
Update wrappers / injections where needed:
- `app/src/main/java/com/algorithmx/q_base/core/navigation/AppEntryWrappers.kt`

Changes:
- Ensure `ExploreQuestionPagerScreen` can access `ChatViewModel` (directly via `hiltViewModel()` in screen OR passed from wrapper if required by nav scope).
- On sheet-open:
  - call `startAiChat()` if no active AI conversation.
  - set chat focus to AI chat id.
  - seed initial context prompt exactly once per open.

### 4) Maintain temporary compatibility with existing one-off AI APIs
Update (phase-gated, optional in first PR):
- `app/src/main/java/com/algorithmx/q_base/feature/explore/presentation/ExploreViewModelAiExtensions.kt`

Plan:
- Keep existing `askAi(...)` for compatibility during migration.
- Mark one-off `aiResponse` flow as deprecated in comments.
- Remove static sheet rendering path after chat sheet is verified.

## Suggested Chat Seed Prompt
When sheet opens:
- "I need help with this question: <stem>. Options: <A..E>. Explain reasoning and guide me, but do not reveal final answer unless I ask directly."

## UX Requirements
1. Bottom sheet opens with smooth animation and drag handle.
2. Input remains visible while keyboard is open (`imePadding`).
3. Dismissal clears only local sheet state, not full chat history unless explicitly requested.
4. If AI backend unavailable, show inline retry state (not crash/no-op).

## Acceptance Criteria
1. Ask AI button opens interactive chat bottom sheet from `QuestionViewer` path.
2. User can send multiple follow-up messages without leaving sheet.
3. Context prompt is auto-seeded once per sheet open.
4. Existing app compile succeeds with no regressions in Explore pager flow.
5. Old static `aiResponse` sheet is removed (or behind temporary fallback flag during rollout).

## Risks and Mitigations
- Risk: Scope mismatch between Explore and Chat view models.
  - Mitigation: Validate nav back stack scope in wrapper before implementation.
- Risk: Duplicated AI chat creation each open.
  - Mitigation: Guard with active AI chat id check before `startAiChat()`.
- Risk: Message bubble reuse requires fields not available in AI-only context.
  - Mitigation: Introduce a small adapter model in `AiChatBottomSheet`.

## Rollout Order
1. Introduce `AiChatBottomSheet` + parent trigger state.
2. Wire `ChatViewModel` integration and seed prompt.
3. Remove static `aiResponse` sheet path.
4. Cleanup deprecated one-off AI methods if no longer used.

## Out of Scope (for this change)
- Reworking AI model/provider settings UX.
- Changing encryption architecture.
- Replacing global chat screen behavior.
