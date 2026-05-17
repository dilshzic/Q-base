package com.algorithmx.q_base.data.backend

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDatabaseImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CoreDatabase {

    override suspend fun createDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(collectionId).document(documentId).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDocument(collectionId: String, documentId: String): Result<Map<String, Any>?> {
        return try {
            val snapshot = firestore.collection(collectionId).document(documentId).get().await()
            Result.success(snapshot.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(collectionId).document(documentId).update(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDocument(collectionId: String, documentId: String): Result<Unit> {
        return try {
            firestore.collection(collectionId).document(documentId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listDocuments(collectionId: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = firestore.collection(collectionId).get().await()
            val list = snapshot.documents.mapNotNull { it.data }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun queryDocuments(collectionId: String, queries: List<CoreQuery>): Result<List<Map<String, Any>>> {
        return try {
            var query: Query = firestore.collection(collectionId)
            for (q in queries) {
                query = when (q.operator) {
                    CoreQueryOperator.EQUAL -> query.whereEqualTo(q.key, q.value)
                    CoreQueryOperator.NOTEQUAL -> query.whereNotEqualTo(q.key, q.value)
                    CoreQueryOperator.GREATER_THAN -> query.whereGreaterThan(q.key, q.value)
                    CoreQueryOperator.LESS_THAN -> query.whereLessThan(q.key, q.value)
                    CoreQueryOperator.ARRAY_CONTAINS -> query.whereArrayContains(q.key, q.value)
                }
            }
            val snapshot = query.get().await()
            val list = snapshot.documents.mapNotNull { it.data }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeDocument(collectionId: String, documentId: String): Flow<Map<String, Any>?> {
        return callbackFlow {
            val listenerRegistration = firestore.collection(collectionId).document(documentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    trySend(snapshot?.data)
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    override fun observeCollection(collectionId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            val listenerRegistration = firestore.collection(collectionId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
                    trySend(list)
                }
            awaitClose { listenerRegistration.remove() }
        }
    }
}
