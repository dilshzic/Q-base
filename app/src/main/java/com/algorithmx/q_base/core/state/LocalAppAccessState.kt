package com.algorithmx.q_base.core.state

import androidx.compose.runtime.compositionLocalOf

val LocalAppAccessState = compositionLocalOf<AppAccessState> {
    AppAccessState.RestoringSession
}