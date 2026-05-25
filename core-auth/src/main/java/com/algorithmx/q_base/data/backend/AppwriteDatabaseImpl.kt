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

@Singleton
class AppwriteDatabaseImpl @Inject constructor(
    private val client: Client,
    private val tablesDB: TablesDB,
) : CoreDatabase {

    private val databaseId = "qbase_db"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun permissionsFor(collectionId: String, documentId: String): List<String> {
        return when (collectionId) {
            "users" -> listOf(
                io.appwrite.Permission.read(io.appwrite.Role.users()),
                io.appwrite.Permission.create(io.appwrite.Role.user(documentId)),
                io.appwrite.Permission.update(io.appwrite.Role.user(documentId)),
                io.appwrite.Permission.delete(io.appwrite.Role.user(documentId))
            )
            "user_private_settings" -> listOf(
                io.appwrite.Permission.read(io.appwrite.Role.user(documentId)),
                io.appwrite.Permission.create(io.appwrite.Role.user(documentId)),
                io.appwrite.Permission.update(io.appwrite.Role.user(documentId)),
                io.appwrite.Permission.delete(io.appwrite.Role.user(documentId))
            )
            else -> listOf(
                io.appwrite.Permission.read(io.appwrite.Role.users()),
                io.appwrite.Permission.create(io.appwrite.Role.users()),
                io.appwrite.Permission.update(io.appwrite.Role.users()),
                io.appwrite.Permission.delete(io.appwrite.Role.users())
            )
        }
    }

    private fun cleanData(data: Map<String, Any>): Map<String, Any> {
        return data.filterKeys { !it.startsWith("$") }
    }

    private fun mapRow(row: Row<Map<String, Any>>): Map<String, Any> {
        val mutableData = row.data.toMutableMap()
        mutableData["\$id"] = row.id
        mutableData["\$createdAt"] = row.createdAt
        mutableData["\$updatedAt"] = row.updatedAt
        mutableData["\$databaseId"] = row.databaseId
        mutableData["\$tableId"] = row.tableId
        return mutableData
    }

    private fun mapRowList(rowList: RowList<Map<String, Any>>): List<Map<String, Any>> {
        return rowList.rows.map { mapRow(it) }
    }

    override suspend fun createDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit> {
        return runCatching {
            tablesDB.upsertRow(
                databaseId = databaseId,
                tableId = collectionId,
                rowId = documentId,
                data = cleanData(data),
                permissions = permissionsFor(collectionId, documentId)
            )
            Unit
        }
    }

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

    override suspend fun updateDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit> {
        return runCatching {
            tablesDB.updateRow(
                databaseId = databaseId,
                tableId = collectionId,
                rowId = documentId,
                data = cleanData(data),
                permissions = permissionsFor(collectionId, documentId)
            )
            Unit
        }
    }

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
