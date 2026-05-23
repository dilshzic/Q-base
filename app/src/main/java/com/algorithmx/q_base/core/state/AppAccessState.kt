package com.algorithmx.q_base.core.state

sealed interface AppAccessState {
    data object Online : AppAccessState
    data object Offline : AppAccessState
    data object NotLoggedIn : AppAccessState
}