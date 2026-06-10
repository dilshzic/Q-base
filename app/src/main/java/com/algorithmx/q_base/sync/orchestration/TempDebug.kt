package com.algorithmx.q_base.sync.orchestration
import android.util.Log
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object TempDebug {
    fun runDebug(databases: CoreDatabase) {
        GlobalScope.launch {
            try {
                val docs = databases.queryDocuments("shared_collections", emptyList()).getOrThrow()
                Log.e("TempDebug", "Fetched ${docs.size} docs from shared_collections")
                docs.forEach { 
                    Log.e("TempDebug", "Doc: id=${it["\$id"]} chatId=${it["chatId"]} name=${it["name"]} collectionId=${it["collectionId"]}")
                }
            } catch (e: Exception) {
                Log.e("TempDebug", "Error fetching", e)
            }
        }
    }
}
