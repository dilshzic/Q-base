package com.algorithmx.q_base.data.backend

import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.services.Databases
import io.appwrite.services.Realtime
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
    private val databases: Databases
) : CoreDatabase {

    // Falls back to "qbase_db" (matching build config/local properties configuration)
    private val databaseId = "qbase_db"
    
    private val scope = CoroutineScope(Dispatchers.IO)

    @Suppress("UNCHECKED_CAST")
    private fun mapDocument(doc: io.appwrite.models.Document<*>): Map<String, Any> {
        val rawData = doc.data as? Map<String, Any> ?: emptyMap()
        // Ensure standard keys like document ID match firestore's id convention if needed,
        // but to keep them identical we preserve exactly the fields passed.
        // Appwrite uses '$id' as the document identifier. Let's make sure we include it or expose it!
        val mutableData = rawData.toMutableMap()
        mutableData["\$id"] = doc.id
        return mutableData
    }

    override suspend fun createDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            // Remove system fields to prevent write errors on Appwrite
            val cleanData = data.filterKeys { !it.startsWith("$") }
            val permissions = when (collectionId) {
                "users" -> listOf(
                    io.appwrite.Permission.read(io.appwrite.Role.users()),
                    io.appwrite.Permission.write(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.update(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.delete(io.appwrite.Role.user(documentId))
                )
                "user_private_settings" -> listOf(
                    io.appwrite.Permission.read(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.write(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.update(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.delete(io.appwrite.Role.user(documentId))
                )
                else -> listOf(
                    io.appwrite.Permission.read(io.appwrite.Role.users()),
                    io.appwrite.Permission.write(io.appwrite.Role.users()),
                    io.appwrite.Permission.update(io.appwrite.Role.users()),
                    io.appwrite.Permission.delete(io.appwrite.Role.users())
                )
            }
            databases.createDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = documentId,
                data = cleanData,
                permissions = permissions
            )
            Result.success(Unit)
        } catch (e: io.appwrite.exceptions.AppwriteException) {
            if (e.code == 409) {
                updateDocument(collectionId, documentId, data)
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDocument(collectionId: String, documentId: String): Result<Map<String, Any>?> {
        return try {
            val doc = databases.getDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = documentId
            )
            Result.success(mapDocument(doc))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            val cleanData = data.filterKeys { !it.startsWith("$") }
            val permissions = when (collectionId) {
                "users" -> listOf(
                    io.appwrite.Permission.read(io.appwrite.Role.users()),
                    io.appwrite.Permission.write(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.update(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.delete(io.appwrite.Role.user(documentId))
                )
                "user_private_settings" -> listOf(
                    io.appwrite.Permission.read(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.write(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.update(io.appwrite.Role.user(documentId)),
                    io.appwrite.Permission.delete(io.appwrite.Role.user(documentId))
                )
                else -> listOf(
                    io.appwrite.Permission.read(io.appwrite.Role.users()),
                    io.appwrite.Permission.write(io.appwrite.Role.users()),
                    io.appwrite.Permission.update(io.appwrite.Role.users()),
                    io.appwrite.Permission.delete(io.appwrite.Role.users())
                )
            }
            databases.updateDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = documentId,
                data = cleanData,
                permissions = permissions
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDocument(collectionId: String, documentId: String): Result<Unit> {
        return try {
            databases.deleteDocument(
                databaseId = databaseId,
                collectionId = collectionId,
                documentId = documentId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listDocuments(collectionId: String): Result<List<Map<String, Any>>> {
        return try {
            val response = databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId
            )
            val list = response.documents.map { mapDocument(it) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun queryDocuments(collectionId: String, queries: List<CoreQuery>): Result<List<Map<String, Any>>> {
        return try {
            val appwriteQueries = queries.map { q ->
                when (q.operator) {
                    CoreQueryOperator.EQUAL -> Query.equal(q.key, q.value)
                    CoreQueryOperator.NOTEQUAL -> Query.notEqual(q.key, q.value)
                    CoreQueryOperator.GREATER_THAN -> Query.greaterThan(q.key, q.value)
                    CoreQueryOperator.LESS_THAN -> Query.lessThan(q.key, q.value)
                    CoreQueryOperator.ARRAY_CONTAINS -> Query.contains(q.key, q.value)
                }
            }
            val response = databases.listDocuments(
                databaseId = databaseId,
                collectionId = collectionId,
                queries = appwriteQueries
            )
            val list = response.documents.map { mapDocument(it) }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeDocument(collectionId: String, documentId: String): Flow<Map<String, Any>?> {
        return callbackFlow {
            // Emits the initial state if available
            scope.launch {
                getDocument(collectionId, documentId).onSuccess { initialDoc ->
                    initialDoc?.let { trySend(it) }
                }
            }

            val realtime = Realtime(client)
            val channel = "databases.$databaseId.collections.$collectionId.documents.$documentId"
            val subscription = realtime.subscribe(channel) { event ->
                val payloadObj = event.payload as? Map<String, Any>
                if (payloadObj != null) {
                    val mutableData = payloadObj.toMutableMap()
                    mutableData["\$id"] = documentId
                    trySend(mutableData)
                }
            }
            awaitClose { subscription.close() }
        }
    }

    override fun observeCollection(collectionId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            // Emit current list of documents initially
            scope.launch {
                listDocuments(collectionId).onSuccess { initialList ->
                    trySend(initialList)
                }
            }

            val realtime = Realtime(client)
            val channel = "databases.$databaseId.collections.$collectionId.documents"
            val subscription = realtime.subscribe(channel) {
                // Fetch the updated collection list and emit
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
