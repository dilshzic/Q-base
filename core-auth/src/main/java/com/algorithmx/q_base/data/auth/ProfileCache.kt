package com.algorithmx.q_base.data.auth

interface ProfileCache {
    suspend fun upsert(profile: UserProfile)
}
