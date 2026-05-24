package com.algorithmx.q_base.core.data.auth

interface ProfileCache {
    suspend fun upsert(profile: UserProfile)
}
