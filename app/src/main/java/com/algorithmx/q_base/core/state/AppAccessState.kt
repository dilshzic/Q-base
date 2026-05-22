package com.algorithmx.q_base.core.state

sealed interface AppAccessState {
    data object RestoringSession : AppAccessState
    data object OnlineReady : AppAccessState
    data object SignedInOffline : AppAccessState
    data object GuestOnline : AppAccessState
    data object OfflineGuest : AppAccessState
}