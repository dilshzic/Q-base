package com.algorithmx.q_base.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(val state: NavigationState) {
    private fun normalizeRoute(route: NavKey): NavKey = when (route) {
        Screen.Collections -> Screen.Explore
        else -> route
    }

    private fun findTopLevelMatch(route: NavKey): NavKey? =
        state.backStacks.keys.find {
            it == route || (it::class == route::class && it is Screen.Sessions && route is Screen.Sessions)
        }

    private fun resetStack(root: NavKey, stack: androidx.navigation3.runtime.NavBackStack<NavKey>) {
        stack.clear()
        stack.add(root)
    }

    fun navigate(route: NavKey) {
        val normalizedRoute = normalizeRoute(route)
        android.util.Log.d("Navigator", "Navigating to: $normalizedRoute")
        
        // Find if this route is a top-level destination
        val topLevelMatch = findTopLevelMatch(normalizedRoute)
        
        if (topLevelMatch != null) {
            if (state.topLevelRoute == topLevelMatch) {
                // If already on this tab, pop to root
                val stack = state.backStacks[topLevelMatch]
                if ((stack?.size ?: 0) > 1) {
                    stack?.clear()
                    stack?.add(topLevelMatch)
                }
            } else {
                // Switch tab
                state.topLevelRoute = topLevelMatch
                
                // Track tab navigation history
                state.tabHistory.remove(topLevelMatch)
                state.tabHistory.add(topLevelMatch)
            }
        } else {
            // Push to current stack (guard against double-push from rapid taps)
            val currentStack = state.backStacks[state.topLevelRoute]
            if (currentStack?.lastOrNull() != normalizedRoute) {
                currentStack?.add(normalizedRoute)
            }
        }
    }

    fun resetTo(route: NavKey) {
        val normalizedRoute = normalizeRoute(route)
        val topLevelMatch = findTopLevelMatch(normalizedRoute)

        state.backStacks.forEach { (root, stack) ->
            resetStack(root, stack)
        }

        if (topLevelMatch != null) {
            state.topLevelRoute = topLevelMatch
            state.tabHistory.clear()
            state.tabHistory.add(topLevelMatch)
        } else {
            val currentStack = state.backStacks[state.topLevelRoute]
                ?: error("Stack for ${state.topLevelRoute} not found")
            currentStack.clear()
            currentStack.add(normalizedRoute)
            state.tabHistory.clear()
            state.tabHistory.add(state.topLevelRoute)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: run {
            android.util.Log.w("Navigator", "goBack: stack for ${state.topLevelRoute} not found, ignoring")
            return
        }
        val currentRoute = currentStack.lastOrNull() ?: return
        android.util.Log.d("Navigator", "goBack: topLevel=${state.topLevelRoute}, currentRoute=$currentRoute, stackSize=${currentStack.size}, tabHistorySize=${state.tabHistory.size}")

        // If we're at the base of the current route, go back to the previously visited tab.
        if (currentRoute == state.topLevelRoute) {
            if (state.tabHistory.size > 1) {
                state.tabHistory.removeLastOrNull()
                val prevTab = state.tabHistory.last()
                android.util.Log.d("Navigator", "goBack: switching tab from ${state.topLevelRoute} to $prevTab")
                state.topLevelRoute = prevTab
            } else {
                android.util.Log.d("Navigator", "goBack: at root of start tab, nothing to do")
            }
        } else {
            android.util.Log.d("Navigator", "goBack: popping sub-screen $currentRoute")
            currentStack.removeLastOrNull()
        }
    }
}
