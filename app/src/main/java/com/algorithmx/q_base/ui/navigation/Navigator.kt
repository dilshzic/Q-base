package com.algorithmx.q_base.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        android.util.Log.d("Navigator", "Navigating to: $route")
        
        // Find if this route is a top-level destination
        val topLevelMatch = state.backStacks.keys.find { 
            it == route || (it::class == route::class && it is Screen.Sessions && route is Screen.Sessions)
        }
        
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
            }
        } else {
            // Push to current stack
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute] ?: error("Stack for ${state.topLevelRoute} not found")
        val currentRoute = currentStack.last()

        // If we're at the base of the current route, go back to the start route stack.
        if (currentRoute == state.topLevelRoute) {
            if (state.topLevelRoute != state.startRoute) {
                state.topLevelRoute = state.startRoute
            }
        } else {
            currentStack.removeLastOrNull()
        }
    }
}
