package com.algorithmx.q_base.core.data.backend

import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.models.Row
import io.appwrite.models.RowList
import io.appwrite.services.Realtime
import io.appwrite.services.TablesDB
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Appwrite implementation of the CoreDatabase interface.
 * Handles document CRUD, querying, and realtime subscriptions using the Appwrite SDK.
 */
@Singleton
class AppwriteDatabaseImpl @Inject constructor(
    private val client: Client,
    private val tablesDB: TablesDB,
) : CoreDatabase {

    private val databaseId = "qbase_db"
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Defines Document-Level Security (DLS) rules based on the collection type.
     * This ensures only authorized users can read or modify specific data.
     */
    private fun permissionsFor(collectionId: String, documentId: String, data: Map<String, Any>? = null): List<String>? {
        return when (collectionId) {
            "users" -> null
            "user_private_settings" -> listOf(
                io.appwrite.Permission.read(io.appwrite.Role.user(documentId)),
                io.appwrite.Permission.write(io.appwrite.Role.user(documentId))
            )
            // CRITICAL FIX: Client SDK cannot grant access to other UIDs.
            // Return null to allow the Appwrite console's collection-level permissions to handle access.
            "chats" -> null
            "messages" -> null
            else -> null
        }
    }

    /**
     * Removes system metadata keys (starting with $) from the data map before
     * sending it to Appwrite to avoid conflicts with internal attributes.
     */
    private fun cleanData(data: Map<String, Any>): Map<String, Any> {
        return data.filterKeys { !it.startsWith("$") }
    }

    /**
     * Converts an Appwrite Row object into a flat Map, injecting system metadata like ID and timestamps.
     * Leverages the Network Interceptor which wraps custom fields into the 'data' property.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> mapRow(row: Row<T>): Map<String, Any> {
        val rawData = row.data
        
        // 1. Try to get data from the 'data' property (populated by our network interceptor)
        val dataMap = if (rawData is Map<*, *>) {
            rawData.entries.associate { it.key.toString() to (it.value ?: "") }.toMutableMap()
        } else {
            // Fallback: Serialize the whole Row and extract non-system fields
            // This handles cases where the interceptor might have missed something
            try {
                val gson = com.google.gson.Gson()
                val json = gson.toJson(row)
                val fullMap = gson.fromJson<Map<String, Any>>(json, Map::class.java) ?: emptyMap()
                fullMap.filterKeys { !listOf("id", "createdAt", "updatedAt", "databaseId", "tableId", "permissions").contains(it) && !it.startsWith("$") }.toMutableMap()
            } catch (e: Exception) {
                mutableMapOf<String, Any>()
            }
        }

        // 2. Inject system metadata using standard $ prefix
        dataMap["\$id"] = row.id
        dataMap["\$createdAt"] = row.createdAt
        dataMap["\$updatedAt"] = row.updatedAt
        dataMap["\$databaseId"] = row.databaseId
        dataMap["\$tableId"] = row.tableId
        
        return dataMap
    }

    /**
     * Maps a list of Appwrite Rows into a list of Maps.
     */
    private fun <T : Any> mapRowList(rowList: RowList<T>): List<Map<String, Any>> {
        return rowList.rows.map { mapRow(it) }
    }

    /** Creates or overwrites a document with specific permissions. */
    override suspend fun createDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit> {
        return runCatching {
            tablesDB.upsertRow(
                databaseId = databaseId,
                tableId = collectionId,
                rowId = documentId,
                data = cleanData(data),
                permissions = permissionsFor(collectionId, documentId, data)
            )
            Unit
        }
    }

    /** Fetches a single document by ID. */
    override suspend fun getDocument(collectionId: String, documentId: String): Result<Map<String, Any>?> {
        return runCatching {
            val row = tablesDB.getRow(
                databaseId = databaseId,
                tableId = collectionId,
                rowId = documentId
            )
            mapRow(row)
        }
    }

    /** Updates an existing document's fields. */
    override suspend fun updateDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit> {
        return runCatching {
            tablesDB.updateRow(
                databaseId = databaseId,
                tableId = collectionId,
                rowId = documentId,
                data = cleanData(data),
                // Only overwrite permissions if we generated new ones (e.g., participant list changed)
                permissions = permissionsFor(collectionId, documentId, data)
            )
            Unit
        }
    }

    /** Deletes a document from a collection. */
    override suspend fun deleteDocument(collectionId: String, documentId: String): Result<Unit> {
        return runCatching {
            tablesDB.deleteRow(
                databaseId = databaseId,
                tableId = collectionId,
                rowId = documentId
            )
            Unit
        }
    }

    /** Lists all documents in a specific collection. */
    override suspend fun listDocuments(collectionId: String): Result<List<Map<String, Any>>> {
        return runCatching {
            val rowList = tablesDB.listRows(
                databaseId = databaseId,
                tableId = collectionId
            )
            mapRowList(rowList)
        }
    }

    /**
     * Executes a query against a collection.
     * Maps internal CoreQuery operators to Appwrite's Query syntax.
     */
    override suspend fun queryDocuments(collectionId: String, queries: List<CoreQuery>): Result<List<Map<String, Any>>> {
        return runCatching {
            fun wrapValue(value: Any): List<Any> = if (value is List<*>) value.filterNotNull() else listOf(value)

            val appwriteQueries = queries.map { query ->
                when (query.operator) {
                    CoreQueryOperator.EQUAL -> Query.equal(query.key, wrapValue(query.value))
                    CoreQueryOperator.NOTEQUAL -> Query.notEqual(query.key, wrapValue(query.value))
                    CoreQueryOperator.GREATER_THAN -> Query.greaterThan(query.key, wrapValue(query.value).first())
                    CoreQueryOperator.LESS_THAN -> Query.lessThan(query.key, wrapValue(query.value).first())
                    CoreQueryOperator.ARRAY_CONTAINS -> Query.contains(query.key, wrapValue(query.value))
                }
            }

            val rowList = tablesDB.listRows(
                databaseId = databaseId,
                tableId = collectionId,
                queries = appwriteQueries
            )
            mapRowList(rowList)
        }
    }

    /**
     * Provides a Flow that emits the current state of a document and updates
     * whenever changes are detected via Appwrite Realtime.
     */
    override fun observeDocument(collectionId: String, documentId: String): Flow<Map<String, Any>?> {
        return callbackFlow {
            scope.launch {
                getDocument(collectionId, documentId).onSuccess { initialDoc ->
                    initialDoc?.let { trySend(it) }
                }
            }

            val realtime = Realtime(client)
            val channel = "tablesdb.$databaseId.tables.$collectionId.rows.$documentId"
            val subscription = realtime.subscribe(channel) { event ->
                try {
                    val payload = event.payload
                    val data = if (payload is Map<*, *>) {
                        val map = payload as Map<String, Any>
                        // If it's already wrapped (unlikely for Realtime but good for consistency)
                        if (map.containsKey("data") && map["data"] is Map<*, *>) {
                            (map["data"] as Map<String, Any>).toMutableMap().apply {
                                // Put back system fields if they are at the top level
                                map.filterKeys { it.startsWith("$") }.forEach { (k, v) -> put(k, v) }
                            }
                        } else {
                            map
                        }
                    } else {
                        val gson = com.google.gson.Gson()
                        val json = gson.toJson(payload)
                        val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type
                        gson.fromJson<Map<String, Any>>(json, mapType) ?: emptyMap()
                    }

                    if (data.isNotEmpty()) {
                        val mutableData = data.toMutableMap()
                        if (!mutableData.containsKey("\$id")) {
                            mutableData["\$id"] = documentId
                        }
                        trySend(mutableData)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppwriteDatabase", "Failed to parse realtime payload for $documentId", e)
                }
            }
            awaitClose { subscription.close() }
        }
    }

    /**
     * Provides a Flow that emits the entire list of documents in a collection
     * and refreshes whenever any row in that collection is modified.
     */
    override fun observeCollection(collectionId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            scope.launch {
                listDocuments(collectionId).onSuccess { initialList ->
                    trySend(initialList)
                }
            }

            val realtime = Realtime(client)
            val channel = "tablesdb.$databaseId.tables.$collectionId.rows"
            val subscription = realtime.subscribe(channel) {
                scope.launch {
                    listDocuments(collectionId).onSuccess { updatedList ->
                        trySend(updatedList)
                    }
                }
            }
            awaitClose { subscription.close() }
        }
    }
}
