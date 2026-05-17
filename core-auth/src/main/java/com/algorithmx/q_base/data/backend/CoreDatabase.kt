package com.algorithmx.q_base.data.backend

import kotlinx.coroutines.flow.Flow

enum class CoreQueryOperator {
    EQUAL,
    NOTEQUAL,
    GREATER_THAN,
    LESS_THAN,
    ARRAY_CONTAINS
}

data class CoreQuery(
    val key: String,
    val operator: CoreQueryOperator,
    val value: Any
)

interface CoreDatabase {
    suspend fun createDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit>
    suspend fun getDocument(collectionId: String, documentId: String): Result<Map<String, Any>?>
    suspend fun updateDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit>
    suspend fun deleteDocument(collectionId: String, documentId: String): Result<Unit>
    suspend fun listDocuments(collectionId: String): Result<List<Map<String, Any>>>
    suspend fun queryDocuments(collectionId: String, queries: List<CoreQuery>): Result<List<Map<String, Any>>>
    fun observeDocument(collectionId: String, documentId: String): Flow<Map<String, Any>?>
    fun observeCollection(collectionId: String): Flow<List<Map<String, Any>>>
}
