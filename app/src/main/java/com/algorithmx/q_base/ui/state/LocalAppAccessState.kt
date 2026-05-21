package com.algorithmx.q_base.ui.state

import androidx.compose.runtime.compositionLocalOf

val LocalAppAccessState = compositionLocalOf<AppAccessState> {
    AppAccessState.RestoringSession
}