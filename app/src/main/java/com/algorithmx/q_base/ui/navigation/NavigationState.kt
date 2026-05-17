package com.algorithmx.q_base.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.key
import androidx.compose.runtime.toMutableStateList
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer

import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * Create a navigation state that persists config changes and process death.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {

    val routes = remember(startRoute, topLevelRoutes) { (topLevelRoutes + startRoute).toList() }
    val backStacks = remember(routes) { mutableMapOf<NavKey, NavBackStack<NavKey>>() }
    for (route in routes) {
        key(route) {
            backStacks[route] = rememberNavBackStack(route)
        }
    }

    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer())
    ) {
        mutableStateOf(startRoute)
    }

    val serializer = remember { NavKeySerializer<NavKey>() }
    val tabHistory = androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { list ->
                list.map { key ->
                    kotlinx.serialization.json.Json.encodeToString<NavKey>(serializer, key)
                }
            },
            restore = { strings ->
                strings.map { str ->
                    kotlinx.serialization.json.Json.decodeFromString<NavKey>(serializer, str)
                }.toMutableStateList()
            }
        )
    ) {
        androidx.compose.runtime.mutableStateListOf(startRoute)
    }

    return remember(startRoute, topLevelRoute, backStacks, tabHistory) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks,
            tabHistory = tabHistory
        )
    }
}

/**
 * State holder for navigation state.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the current top level route
 * @param backStacks - the back stacks for each top level route
 * @param tabHistory - the history stack of visited top-level routes
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>,
    val tabHistory: SnapshotStateList<NavKey>
) {
    var topLevelRoute: NavKey by topLevelRoute
    val stacksInUse: List<NavKey>
        get() = listOf(topLevelRoute)
}

/**
 * Convert NavigationState into NavEntries.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): SnapshotStateList<NavEntry<NavKey>> {
    val activeStack = backStacks[topLevelRoute] ?: error("Stack for $topLevelRoute not found")

    val decorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
    )

    val activeEntries = rememberDecoratedNavEntries(
        backStack = activeStack,
        entryDecorators = decorators,
        entryProvider = entryProvider
    )

    return remember(activeEntries) {
        activeEntries.toMutableStateList()
    }
}
