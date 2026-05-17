# How We Fixed: OS-Level Back Navigation in Navigation 3

## The Problem

When pressing the system back button (gesture or hardware), the app **exited entirely** instead of navigating back to the previous screen or tab. This affected:
- Sub-screens (e.g., ChatDetail → ChatList, CollectionOverview → Home)
- Tab-level back (e.g., Connect tab → Home tab)
- Multi-step wizards (session creation, collection import)

## Root Cause Analysis

### Why Nav3 Doesn't Handle This Automatically

Navigation 3 (unlike Nav 2.x) is a **developer-owned state** model. The backstack is a `MutableList<NavKey>` that you manage directly. There is:
- ❌ No automatic system back button interception
- ❌ No built-in tab history tracking
- ❌ No automatic `popUpTo`/`launchSingleTop` behavior

You must wire everything yourself using `BackHandler` from `androidx.activity.compose`.

### The Three Bugs

#### Bug 1: `AnimatedContent` wrapping `NavDisplay`

**This was the critical one.** The original `AppNavDisplay` looked like:

```kotlin
AnimatedContent(targetState = entries) { entriesState ->
    NavDisplay(
        entries = entriesState,
        onBack = { navigator.goBack() }
    )
}
```

`NavDisplay` internally registers its own `BackHandler` when `entries.size > 1`. The problem: `AnimatedContent` keeps **both old and new content composed simultaneously** during transitions. This means:

1. User pops a sub-screen → entries goes from `[Home, ChatDetail]` to `[Home]`
2. `AnimatedContent` starts exit animation
3. **OLD** `NavDisplay` (with `entries=[Home, ChatDetail]`, size=2) still has its `BackHandler` **enabled**
4. **NEW** `NavDisplay` (with `entries=[Home]`, size=1) has its `BackHandler` **disabled**
5. User presses back again → **OLD** `NavDisplay`'s stale `BackHandler` fires, consuming the event
6. Since the old handler calls `goBack()` but the stack is already at root → **nothing visible happens** or the app exits

**Diagnosis via logcat:**
```
10:56:30.953 canGoBack=true (entries=1, tabHistory=2)  ← Handler ENABLED
10:56:37.289 [NEW PID!] canGoBack=false                 ← App was KILLED
```

The 6-second gap with a PID change proved the back event was consumed but not handled properly — the app was force-closed by the system.

#### Bug 2: No Tab History Stack

When at the root of a non-Home tab (e.g., Connect), pressing back hardcoded a jump to `Screen.Home`:

```kotlin
// Old code
if (state.topLevelRoute != state.startRoute) {
    state.topLevelRoute = state.startRoute  // Always jumps to Home
}
```

This didn't track which tabs were visited in what order.

#### Bug 3: No BackHandler in Multi-Step Wizards

The `NewSessionWizardScreen` and `ImportWizardScreen` have internal step navigation (step 1 → 2 → 3) but **no `BackHandler`** to intercept the OS back button. Pressing back popped the entire wizard off the navigation stack instead of going to the previous step.

---

## The Fix

### Layer 1: Remove `AnimatedContent` Around `NavDisplay`

```diff
 @Composable
 fun AppNavDisplay(...) {
-    AnimatedContent(targetState = entries) { entriesState ->
-        NavDisplay(entries = entriesState, onBack = { navigator.goBack() })
-    }
+    NavDisplay(entries = entries, onBack = { navigator.goBack() })
 }
```

**Why:** A single `NavDisplay` instance means its internal `BackHandler` has clean, non-conflicting state. When `entries > 1`, NavDisplay handles sub-screen back. When `entries == 1`, NavDisplay's handler is disabled and falls through.

**Trade-off:** Lost tab-switch animation. Can be re-added with `Crossfade` at the tab level if needed.

### Layer 2: Tab History Stack

Added a `tabHistory: SnapshotStateList<NavKey>` to `NavigationState`, serialized with a custom `listSaver` using `NavKeySerializer` for process death survival.

```kotlin
// Navigator.navigate() — track tab switches
state.tabHistory.remove(topLevelMatch)
state.tabHistory.add(topLevelMatch)  // MRU ordering

// Navigator.goBack() — pop tab history
if (currentRoute == state.topLevelRoute) {
    if (state.tabHistory.size > 1) {
        state.tabHistory.removeLastOrNull()
        state.topLevelRoute = state.tabHistory.last()
    }
}
```

A separate `BackHandler` in `AppNavDisplay` covers the tab-level case:

```kotlin
val canGoBackTab = entries.size <= 1 && navigationState.tabHistory.size > 1
BackHandler(enabled = canGoBackTab) { navigator.goBack() }
```

### Layer 3: BackHandler in Wizard Screens

**NewSessionWizardScreen:**
```kotlin
BackHandler(enabled = step > 1) {
    viewModel.setWizardStep(step - 1)
}
```

**ImportWizardScreen:**
```kotlin
BackHandler(enabled = uiState !is ImportStep.Welcome) {
    when (uiState) {
        is ImportStep.MediaSelection -> viewModel.navigateTo(ImportStep.Welcome)
        is ImportStep.ImportConfig -> viewModel.navigateTo(ImportStep.MediaSelection)
        // ... etc
    }
}
```

---

## Files Changed

| File | Change |
|---|---|
| `NavigationState.kt` | Added `tabHistory` field + serialization |
| `Navigator.kt` | MRU tab tracking in `navigate()`, history-based back in `goBack()` |
| `MainActivity.kt` | Removed `AnimatedContent` wrapper, two-tier BackHandler |
| `NewSessionWizardScreen.kt` | Added step-level BackHandler |
| `ImportWizardScreen.kt` | Added step-level BackHandler |

## Key Takeaway

> In Navigation 3, **never wrap `NavDisplay` in `AnimatedContent`**. NavDisplay registers its own internal `BackHandler`, and AnimatedContent keeps stale copies alive during transitions that compete for back events. Use NavDisplay directly and handle tab-level animations separately if needed.

## How to Verify

1. **Sub-screen back**: Open collection → press back → returns to Home ✓
2. **Tab history**: Home → Explore → Connect → back → Explore → back → Home → back → exits ✓
3. **Wizard back**: Open New Session → step 2 → step 3 → back → step 2 → back → step 1 → back → exits wizard ✓
