package com.algorithmx.q_base.core.backend.di

import android.content.Context
import com.algorithmx.q_base.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.appwrite.Client
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppwriteModule {

    @Provides
    @Singleton
    fun provideAppwriteClient(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
    ): Client {
        val client = Client(context)
            .setEndpoint("https://syd.cloud.appwrite.io/v1")
            .setProject(BuildConfig.APPWRITE_PROJECT_ID)

        try {
            val httpField = Class.forName("io.appwrite.Client").getDeclaredField("http")
            httpField.isAccessible = true

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val originalOkHttpClient = httpField.get(client) as OkHttpClient
            val customOkHttpClient = originalOkHttpClient.newBuilder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)

                    if (!response.isSuccessful && response.code != 101 && response.code != 401 && response.code != 409) {
                        val code = response.code
                        val bodyString = try {
                            val source = response.body?.source()
                            source?.request(Long.MAX_VALUE)
                            val buffer = source?.buffer
                            buffer?.clone()?.readString(java.nio.charset.Charset.forName("UTF-8"))
                        } catch (e: Exception) {
                            null
                        }

                        android.util.Log.e("QbaseAppwriteError", "Appwrite error response - URL: ${request.url}, Status: $code, Body: $bodyString")

                        if (bodyString != null && (!bodyString.trim().startsWith("{") || !bodyString.trim().endsWith("}"))) {
                            val fakeJsonError = "{\"message\": ${com.google.gson.Gson().toJson(bodyString)}, \"code\": $code, \"type\": \"appwrite_error\"}"
                            val mediaType = response.body?.contentType()
                            val newBody = okhttp3.ResponseBody.create(mediaType, fakeJsonError)
                            return@addInterceptor response.newBuilder()
                                .body(newBody)
                                .build()
                        }
                    } else {
                        val bodyString = try {
                            val source = response.body?.source()
                            source?.request(Long.MAX_VALUE)
                            val buffer = source?.buffer
                            buffer?.clone()?.readString(java.nio.charset.Charset.forName("UTF-8"))
                        } catch (e: Exception) {
                            null
                        }

                        if (bodyString != null && bodyString.trim().startsWith("{") && bodyString.trim().endsWith("}")) {
                            try {
                                val json = org.json.JSONObject(bodyString)
                                var modified = false

                                fun wrapTablesDbRow(obj: org.json.JSONObject): Boolean {
                                    if (obj.has("\$id") && obj.has("\$tableId") && !obj.has("data")) {
                                        val dataObj = org.json.JSONObject()
                                        val keys = obj.keys()
                                        val toMove = mutableListOf<String>()
                                        while (keys.hasNext()) {
                                            val key = keys.next()
                                            if (!key.startsWith("\$") && key != "permissions") {
                                                toMove.add(key)
                                            }
                                        }
                                        if (toMove.isNotEmpty()) {
                                            toMove.forEach { key ->
                                                dataObj.put(key, obj.get(key))
                                                obj.remove(key)
                                            }
                                            obj.put("data", dataObj)
                                            return true
                                        }
                                    }
                                    return false
                                }

                                if (json.has("rows")) {
                                    val rows = json.optJSONArray("rows")
                                    if (rows != null) {
                                        for (i in 0 until rows.length()) {
                                            val row = rows.optJSONObject(i)
                                            if (row != null) {
                                                if (wrapTablesDbRow(row)) modified = true
                                            }
                                        }
                                    }
                                } else {
                                    if (wrapTablesDbRow(json)) modified = true
                                }

                                if (modified) {
                                    val shieldedBodyString = json.toString()
                                    val mediaType = response.body?.contentType()
                                    val newBody = okhttp3.ResponseBody.create(mediaType, shieldedBodyString)
                                    return@addInterceptor response.newBuilder()
                                        .body(newBody)
                                        .build()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("QbaseReflection", "Failed to parse/shield response JSON", e)
                            }
                        }
                    }

                    response
                }
                .build()

            httpField.set(client, customOkHttpClient)
            android.util.Log.d("QbaseReflection", "Successfully injected custom OkHttpClient with HTTP logging and error shielding!")
        } catch (e: Exception) {
            android.util.Log.e("QbaseReflection", "Failed to inject custom OkHttpClient", e)
        }

        return client
    }

    @Provides
    @Singleton
    fun provideAppwriteStorage(client: Client): Storage = Storage(client)

    @Provides
    @Singleton
    fun provideAppwriteDatabases(client: Client): Databases = Databases(client)

    @Provides
    @Singleton
    fun provideAppwriteTablesDB(client: Client): io.appwrite.services.TablesDB = io.appwrite.services.TablesDB(client)

    @Provides
    @Singleton
    fun provideAppwriteTables(client: Client): Any? {
        return try {
            val cls = try {
                Class.forName("io.appwrite.services.Tables")
            } catch (e: ClassNotFoundException) {
                Class.forName("io.appwrite.services.TablesDB")
            }
            val constructor = cls.getConstructor(io.appwrite.Client::class.java)
            constructor.newInstance(client)
        } catch (e: Exception) {
            android.util.Log.w("QbaseReflection", "Tables client not available in SDK", e)
            null
        }
    }
}
