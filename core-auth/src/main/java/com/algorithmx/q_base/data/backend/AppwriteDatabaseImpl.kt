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
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.Continuation
import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

@Singleton
class AppwriteDatabaseImpl @Inject constructor(
    private val client: Client,
    private val databases: Databases,
    private val tablesClient: Any?
) : CoreDatabase {

    // Falls back to "qbase_db" (matching build config/local properties configuration)
    private val databaseId = "qbase_db"
    
    private val scope = CoroutineScope(Dispatchers.IO)

    private suspend fun invokeSuspendReflective(
        methodName: String,
        parameterTypes: Array<Class<*>>,
        args: Array<Any?>
    ): Any? = suspendCoroutineUninterceptedOrReturn { uCont ->
        val tables = tablesClient ?: return@suspendCoroutineUninterceptedOrReturn null
        try {
            android.util.Log.d("QbaseReflection", "=== Tables Client Methods ===")
            tables.javaClass.methods.forEach { method ->
                if (method.name in listOf("listRows", "getRow", "createRow", "updateRow")) {
                    val params = method.parameterTypes.map { it.name }.joinToString(", ")
                    android.util.Log.d("QbaseReflection", "  method ${method.name}($params) returning ${method.returnType.name}")
                }
            }
            
            val typesWithContinuation = parameterTypes + Continuation::class.java
            val method = tables.javaClass.getMethod(methodName, *typesWithContinuation)
            val argsWithContinuation = args + uCont
            method.invoke(tables, *argsWithContinuation)
        } catch (e: Exception) {
            android.util.Log.e("QbaseReflection", "Reflective call to $methodName failed", e)
            throw e
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapRow(row: Any): Map<String, Any> {
        return try {
            val idField = row.javaClass.getMethod("getId")
            val id = idField.invoke(row) as? String ?: ""

            android.util.Log.d("QbaseReflection", "=== Inspecting row object ===")
            row.javaClass.declaredFields.forEach { field ->
                try {
                    field.isAccessible = true
                    android.util.Log.d("QbaseReflection", "  row field ${field.name} = ${field.get(row)}")
                } catch (e: Exception) {
                    android.util.Log.d("QbaseReflection", "  failed to inspect row field ${field.name}")
                }
            }

            val dataField = row.javaClass.getMethod("getData")
            val rawData = dataField.invoke(row)
            android.util.Log.d("QbaseReflection", "mapRow: row id=$id, rawData class=${rawData?.javaClass?.name}")
            if (rawData != null) {
                rawData.javaClass.declaredFields.forEach { field ->
                    try {
                        field.isAccessible = true
                        android.util.Log.d("QbaseReflection", "  field ${field.name} = ${field.get(rawData)}")
                    } catch (e: Exception) {
                        android.util.Log.d("QbaseReflection", "  failed to inspect field ${field.name}")
                    }
                }
            }

            val dataMap = if (rawData is AppwriteDocumentModel) {
                rawData.data
            } else {
                // Keep the fallback strategies for non-AppwriteDocumentModel classes
                var extracted: Map<String, Any>? = null
                if (rawData is Map<*, *>) {
                    extracted = rawData.entries.associate { it.key.toString() to (it.value ?: "") }
                } else if (rawData != null) {
                    try {
                        val toMap = rawData.javaClass.getMethod("toMap")
                        extracted = toMap.invoke(rawData) as? Map<String, Any>
                    } catch (e: Exception) {
                        // try getters
                        val map = mutableMapOf<String, Any>()
                        rawData.javaClass.methods.forEach { method ->
                            val mName = method.name
                            val key = when {
                                mName.startsWith("get") && mName.length > 3 ->
                                    mName[3].lowercaseChar() + mName.substring(4)
                                mName.startsWith("is") && mName.length > 2 ->
                                    mName[2].lowercaseChar() + mName.substring(3)
                                else -> null
                            }
                            if (key != null && key !in setOf("class", "hashCode") && method.parameterCount == 0) {
                                try {
                                    method.invoke(rawData)?.let { map[key] = it }
                                } catch (e: Exception) {}
                            }
                        }
                        extracted = map
                    }
                }
                extracted ?: emptyMap()
            }

            val mutableData = dataMap.toMutableMap()
            mutableData["\$id"] = id
            
            // Add other metadata fields if available
            try {
                row.javaClass.getMethod("getCreatedAt").invoke(row)?.let { mutableData["\$createdAt"] = it }
                row.javaClass.getMethod("getUpdatedAt").invoke(row)?.let { mutableData["\$updatedAt"] = it }
                row.javaClass.getMethod("getDatabaseId").invoke(row)?.let { mutableData["\$databaseId"] = it }
                row.javaClass.getMethod("getTableId").invoke(row)?.let { mutableData["\$tableId"] = it }
            } catch (e: Exception) {
                // ignore
            }
            android.util.Log.d("QbaseReflection", "mapRow final result keys: ${mutableData.keys}")
            mutableData
        } catch (e: Exception) {
            android.util.Log.e("QbaseReflection", "Failed to map row", e)
            emptyMap<String, Any>()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapRowList(rowList: Any): List<Map<String, Any>> {
        return try {
            val getRowsMethod = rowList.javaClass.getMethod("getRows")
            val rows = getRowsMethod.invoke(rowList) as? List<*> ?: emptyList<Any?>()
            rows.map { mapRow(it!!) }
        } catch (e: Exception) {
            emptyList<Map<String, Any>>()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDocument(doc: io.appwrite.models.Document<*>): Map<String, Any> {
        val rawData = doc.data as? Map<String, Any> ?: emptyMap()
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

            if (tablesClient != null) {
                try {
                    invokeSuspendReflective(
                        methodName = "createRow",
                        parameterTypes = arrayOf<Class<*>>(
                            String::class.java,
                            String::class.java,
                            String::class.java,
                            Any::class.java,
                            List::class.java,
                            Class::class.java
                        ),
                        args = arrayOf(
                            databaseId,
                            collectionId,
                            documentId,
                            cleanData,
                            permissions,
                            AppwriteDocumentModel::class.java
                        )
                    )
                    return Result.success(Unit)
                } catch (e: Exception) {
                    val unwrapped = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
                    if (unwrapped is io.appwrite.exceptions.AppwriteException && unwrapped.code == 409) {
                        return updateDocument(collectionId, documentId, data)
                    }
                }
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
            if (tablesClient != null) {
                try {
                    val row = invokeSuspendReflective(
                        methodName = "getRow",
                        parameterTypes = arrayOf<Class<*>>(
                            String::class.java,
                            String::class.java,
                            String::class.java,
                            Class::class.java
                        ),
                        args = arrayOf(
                            databaseId,
                            collectionId,
                            documentId,
                            AppwriteDocumentModel::class.java
                        )
                    )
                    if (row != null) {
                        return Result.success(mapRow(row))
                    }
                } catch (e: Exception) {
                    // Fallback to legacy databases
                }
            }
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

            if (tablesClient != null) {
                try {
                    invokeSuspendReflective(
                        methodName = "updateRow",
                        parameterTypes = arrayOf<Class<*>>(
                            String::class.java,
                            String::class.java,
                            String::class.java,
                            Any::class.java,
                            List::class.java,
                            Class::class.java
                        ),
                        args = arrayOf(
                            databaseId,
                            collectionId,
                            documentId,
                            cleanData,
                            permissions,
                            AppwriteDocumentModel::class.java
                        )
                    )
                    return Result.success(Unit)
                } catch (e: Exception) {
                    // Fallback to legacy databases
                }
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
            if (tablesClient != null) {
                try {
                    invokeSuspendReflective(
                        methodName = "deleteRow",
                        parameterTypes = arrayOf<Class<*>>(
                            String::class.java,
                            String::class.java,
                            String::class.java
                        ),
                        args = arrayOf(
                            databaseId,
                            collectionId,
                            documentId
                        )
                    )
                    return Result.success(Unit)
                } catch (e: Exception) {
                    // Fallback to legacy databases
                }
            }

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
            if (tablesClient != null) {
                try {
                    val rowList = invokeSuspendReflective(
                        methodName = "listRows",
                        parameterTypes = arrayOf<Class<*>>(
                            String::class.java,
                            String::class.java,
                            List::class.java,
                            Class::class.java
                        ),
                        args = arrayOf(
                            databaseId,
                            collectionId,
                            null,
                            AppwriteDocumentModel::class.java
                        )
                    )
                    if (rowList != null) {
                        return Result.success(mapRowList(rowList))
                    }
                } catch (e: Exception) {
                    // Fallback to legacy databases
                }
            }

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
            // Appwrite SDK requires query values to be List<Any> for typed operators.
            // Wrapping raw scalar values ensures correct serialisation regardless of SDK version.
            fun wrapValue(v: Any): List<Any> = if (v is List<*>) v.filterNotNull() else listOf(v)

            val appwriteQueries = queries.map { q ->
                when (q.operator) {
                    CoreQueryOperator.EQUAL -> Query.equal(q.key, wrapValue(q.value))
                    CoreQueryOperator.NOTEQUAL -> Query.notEqual(q.key, wrapValue(q.value))
                    CoreQueryOperator.GREATER_THAN -> Query.greaterThan(q.key, wrapValue(q.value).first())
                    CoreQueryOperator.LESS_THAN -> Query.lessThan(q.key, wrapValue(q.value).first())
                    CoreQueryOperator.ARRAY_CONTAINS -> Query.contains(q.key, wrapValue(q.value))
                }
            }

            if (tablesClient != null) {
                try {
                    val rowList = invokeSuspendReflective(
                        methodName = "listRows",
                        parameterTypes = arrayOf<Class<*>>(
                            String::class.java,
                            String::class.java,
                            List::class.java,
                            Class::class.java
                        ),
                        args = arrayOf(
                            databaseId,
                            collectionId,
                            appwriteQueries,
                            AppwriteDocumentModel::class.java
                        )
                    )
                    if (rowList != null) {
                        return Result.success(mapRowList(rowList))
                    }
                } catch (e: Exception) {
                    // Fallback to legacy databases
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

@JsonAdapter(AppwriteDocumentModel.Adapter::class)
class AppwriteDocumentModel(
    val data: Map<String, Any> = emptyMap()
) {
    class Adapter : TypeAdapter<AppwriteDocumentModel>() {
        override fun write(out: JsonWriter, value: AppwriteDocumentModel?) {
            out.beginObject()
            value?.data?.forEach { (k, v) ->
                out.name(k)
                writeValue(out, v)
            }
            out.endObject()
        }

        // Recursive writer to safely serialize nested Maps and Lists
        private fun writeValue(out: JsonWriter, value: Any?) {
            when (value) {
                null -> out.nullValue()
                is String -> out.value(value)
                is Number -> out.value(value)
                is Boolean -> out.value(value)
                is Map<*, *> -> {
                    out.beginObject()
                    value.forEach { (k, v) ->
                        out.name(k.toString())
                        writeValue(out, v)
                    }
                    out.endObject()
                }
                is List<*> -> {
                    out.beginArray()
                    value.forEach { item -> writeValue(out, item) }
                    out.endArray()
                }
                else -> out.nullValue()
            }
        }

        override fun read(reader: JsonReader): AppwriteDocumentModel {
            val map = mutableMapOf<String, Any>()
            reader.beginObject()
            while (reader.hasNext()) {
                val name = reader.nextName()
                val value = readValue(reader)
                if (value != null) {
                    map[name] = value
                }
            }
            reader.endObject()
            return AppwriteDocumentModel(map)
        }

        private fun readValue(reader: JsonReader): Any? {
            return when (reader.peek()) {
                JsonToken.STRING -> reader.nextString()
                JsonToken.NUMBER -> {
                    val str = reader.nextString()
                    str.toLongOrNull() ?: str.toDoubleOrNull() ?: str
                }
                JsonToken.BOOLEAN -> reader.nextBoolean()
                JsonToken.NULL -> {
                    reader.nextNull()
                    null
                }
                JsonToken.BEGIN_ARRAY -> {
                    val list = mutableListOf<Any>()
                    reader.beginArray()
                    while (reader.hasNext()) {
                        readValue(reader)?.let { list.add(it) }
                    }
                    reader.endArray()
                    list
                }
                JsonToken.BEGIN_OBJECT -> {
                    val map = mutableMapOf<String, Any>()
                    reader.beginObject()
                    while (reader.hasNext()) {
                        val name = reader.nextName()
                        readValue(reader)?.let { map[name] = it }
                    }
                    reader.endObject()
                    map
                }
                else -> {
                    reader.skipValue()
                    null
                }
            }
        }
    }
}
