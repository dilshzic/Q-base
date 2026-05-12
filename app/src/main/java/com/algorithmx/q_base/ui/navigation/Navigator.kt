package com.algorithmx.q_base.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Handles navigation events (forward and back) by updating the navigation state.
 */
class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        android.util.Log.d("Navigator", "Navigating to: $route")
        
        // Find if this route (or its type) is in the top level routes
        val topLevelMatch = state.backStacks.keys.find { it == route || it::class == route::class }
        
        if (topLevelMatch != null) {
            if (state.topLevelRoute == topLevelMatch) {
                // If we're already on this top level route, pop to root
                val stack = state.backStacks[topLevelMatch]
                while ((stack?.size ?: 0) > 1) {
                    stack?.removeLastOrNull()
                }
            } else {
                // Switch to the top level route
                state.topLevelRoute = topLevelMatch
            }
        } else {
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
