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
            "users" -> listOf(
                io.appwrite.Permission.read(io.appwrite.Role.users()), // Anyone logged in can read public profiles
                io.appwrite.Permission.write(io.appwrite.Role.user(documentId))
            )
            "user_private_settings" -> listOf(
                io.appwrite.Permission.read(io.appwrite.Role.user(documentId)), // Only owner can read
                io.appwrite.Permission.write(io.appwrite.Role.user(documentId))
            )
            "chats" -> {
                val participants = data?.get("participantIds") as? List<*>
                if (participants != null) {
                    val perms = mutableListOf<String>()
                    // Grant read/write access to every participant in the chat
                    participants.filterIsInstance<String>().forEach { uid ->
                        perms.add(io.appwrite.Permission.read(io.appwrite.Role.user(uid)))
                        perms.add(io.appwrite.Permission.write(io.appwrite.Role.user(uid)))
                    }
                    perms
                } else null // Return null on simple updates to preserve existing permissions
            }
            "messages" -> {
                val senderId = data?.get("senderId") as? String
                val receiverId = data?.get("receiverId") as? String
                if (senderId != null) {
                    val perms = mutableListOf<String>()
                    perms.add(io.appwrite.Permission.read(io.appwrite.Role.user(senderId)))
                    perms.add(io.appwrite.Permission.write(io.appwrite.Role.user(senderId)))
                    receiverId?.let { 
                        perms.add(io.appwrite.Permission.read(io.appwrite.Role.user(it))) 
                    }
                    perms
                } else null
            }
            else -> null // Fallback to collection-level settings in Appwrite Console
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
     */
    private fun mapRow(row: Row<Map<String, Any>>): Map<String, Any> {
        val mutableData = row.data.toMutableMap()
        mutableData["\$id"] = row.id
        mutableData["\$createdAt"] = row.createdAt
        mutableData["\$updatedAt"] = row.updatedAt
        mutableData["\$databaseId"] = row.databaseId
        mutableData["\$tableId"] = row.tableId
        return mutableData
    }

    /**
     * Maps a list of Appwrite Rows into a list of Maps.
     */
    private fun mapRowList(rowList: RowList<Map<String, Any>>): List<Map<String, Any>> {
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
            mapRow(
                tablesDB.getRow(
                    databaseId = databaseId,
                    tableId = collectionId,
                    rowId = documentId
                )
            )
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
            mapRowList(
                tablesDB.listRows(
                    databaseId = databaseId,
                    tableId = collectionId
                )
            )
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

            mapRowList(
                tablesDB.listRows(
                    databaseId = databaseId,
                    tableId = collectionId,
                    queries = appwriteQueries
                )
            )
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
                val payloadObj = event.payload as? Map<*, *>
                if (payloadObj != null) {
                    val mutableData = payloadObj.entries.associate { (key, value) ->
                        key.toString() to (value ?: "")
                    }.toMutableMap()
                    mutableData["\$id"] = documentId
                    trySend(mutableData)
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
