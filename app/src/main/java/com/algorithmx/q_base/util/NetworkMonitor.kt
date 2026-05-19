package com.algorithmx.q_base.util

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext private val context: Context) {
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> get() = _isConnected
}
