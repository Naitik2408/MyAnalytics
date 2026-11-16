package com.lifeanalytics.app.data.backup

import com.lifeanalytics.app.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Interface for Supabase Auth, Storage (`user-backups`), and PostgREST (`backup_metadata`)
 * operations so unit tests can also inject an in-memory fake or test against live endpoints.
 */
interface CloudBackupRemoteDataSource {
    suspend fun signUpWithEmailPassword(email: String, password: String): Result<SupabaseSession>
    suspend fun signInWithEmailPassword(email: String, password: String): Result<SupabaseSession>
    suspend fun refreshSession(refreshToken: String): Result<SupabaseSession>
    suspend fun signOut(accessToken: String): Result<Unit>

    suspend fun uploadBackupObject(
        accessToken: String,
        storagePath: String,
        envelopeJsonBytes: ByteArray
    ): Result<Unit>

    suspend fun downloadBackupObject(
        accessToken: String,
        storagePath: String
    ): Result<ByteArray>

    suspend fun deleteBackupObject(
        accessToken: String,
        storagePath: String
    ): Result<Unit>

    suspend fun insertBackupMetadata(
        accessToken: String,
        metadata: CloudBackupMetadata
    ): Result<CloudBackupMetadata>

    suspend fun markBackupVerified(
        accessToken: String,
        backupId: String
    ): Result<Unit>

    suspend fun listVerifiedBackups(
        accessToken: String,
        userId: String
    ): Result<List<CloudBackupMetadata>>

    suspend fun deleteBackupMetadata(
        accessToken: String,
        backupId: String
    ): Result<Unit>
}

class SupabaseCloudClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/'),
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY
) : CloudBackupRemoteDataSource {

    companion object {
        const val BUCKET_NAME = "user-backups"
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    override suspend fun signUpWithEmailPassword(
        email: String,
        password: String
    ): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim()
            require(cleanEmail.isNotEmpty() && cleanEmail.contains("@")) {
                "Enter a valid email address."
            }
            require(password.length >= 6) {
                "Account password must be at least 6 characters."
            }
            val body = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }.toString()

            val response = executeJsonRequest(
                method = "POST",
                endpoint = "$supabaseUrl/auth/v1/signup",
                bearerToken = anonKey,
                bodyJson = body
            )
            val json = JSONObject(response)
            if (json.has("access_token") && !json.isNull("access_token")) {
                parseSessionJson(json)
            } else {
                // If signup returned user object without immediate token, perform token grant sign-in
                signInWithEmailPassword(cleanEmail, password).getOrThrow()
            }
        }
    }

    override suspend fun signInWithEmailPassword(
        email: String,
        password: String
    ): Result<SupabaseSession> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanEmail = email.trim()
            require(cleanEmail.isNotEmpty() && cleanEmail.contains("@")) {
                "Enter a valid email address."
            }
            require(password.isNotEmpty()) {
                "Enter your password."
            }
            val body = JSONObject().apply {
                put("email", cleanEmail)
                put("password", password)
            }.toString()

            val response = executeJsonRequest(
                method = "POST",
                endpoint = "$supabaseUrl/auth/v1/token?grant_type=password",
                bearerToken = anonKey,
                bodyJson = body
            )
            parseSessionJson(JSONObject(response))
        }
    }

    override suspend fun refreshSession(refreshToken: String): Result<SupabaseSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("refresh_token", refreshToken)
                }.toString()

                val response = executeJsonRequest(
                    method = "POST",
                    endpoint = "$supabaseUrl/auth/v1/token?grant_type=refresh_token",
                    bearerToken = anonKey,
                    bodyJson = body
                )
                parseSessionJson(JSONObject(response))
            }
        }

    override suspend fun signOut(accessToken: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                executeJsonRequest(
                    method = "POST",
                    endpoint = "$supabaseUrl/auth/v1/logout",
                    bearerToken = accessToken,
                    bodyJson = null
                )
                Unit
            }
        }

    override suspend fun uploadBackupObject(
        accessToken: String,
        storagePath: String,
        envelopeJsonBytes: ByteArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = storagePath.trimStart('/')
            val url = URL("$supabaseUrl/storage/v1/object/$BUCKET_NAME/$cleanPath")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/octet-stream")
                setRequestProperty("x-upsert", "true")
            }
            try {
                conn.outputStream.use { out ->
                    out.write(envelopeJsonBytes)
                }
                val code = conn.responseCode
                if (code !in 200..299) {
                    val err = readStream(conn.errorStream)
                    throw IllegalStateException(parseSupabaseError(code, err))
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    override suspend fun downloadBackupObject(
        accessToken: String,
        storagePath: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = storagePath.trimStart('/')
            val url = URL("$supabaseUrl/storage/v1/object/authenticated/$BUCKET_NAME/$cleanPath")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    val err = readStream(conn.errorStream)
                    throw IllegalStateException(parseSupabaseError(code, err))
                }
                conn.inputStream.use { it.readBytes() }
            } finally {
                conn.disconnect()
            }
        }
    }

    override suspend fun deleteBackupObject(
        accessToken: String,
        storagePath: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanPath = storagePath.trimStart('/')
            val url = URL("$supabaseUrl/storage/v1/object/$BUCKET_NAME/$cleanPath")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299 && code != 404) {
                    val err = readStream(conn.errorStream)
                    throw IllegalStateException(parseSupabaseError(code, err))
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    override suspend fun insertBackupMetadata(
        accessToken: String,
        metadata: CloudBackupMetadata
    ): Result<CloudBackupMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("id", metadata.id)
                put("user_id", metadata.userId)
                put("storage_path", metadata.storagePath)
                put("format_version", metadata.formatVersion)
                put("schema_version", metadata.schemaVersion)
                put("app_version", metadata.appVersion)
                put("size_bytes", metadata.sizeBytes)
                put("sha256_checksum", metadata.sha256Checksum)
                put("record_counts", BackupSerializer.encodeRecordCounts(metadata.recordCounts))
                put("device_label", metadata.deviceLabel)
                put("is_verified", metadata.isVerified)
            }.toString()

            val response = executeJsonRequest(
                method = "POST",
                endpoint = "$supabaseUrl/rest/v1/backup_metadata",
                bearerToken = accessToken,
                bodyJson = body,
                extraHeaders = mapOf("Prefer" to "return=representation")
            )
            val arr = JSONArray(response)
            parseMetadataRow(arr.getJSONObject(0))
        }
    }

    override suspend fun markBackupVerified(
        accessToken: String,
        backupId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("is_verified", true)
            }.toString()

            executeJsonRequest(
                method = "PATCH",
                endpoint = "$supabaseUrl/rest/v1/backup_metadata?id=eq.$backupId",
                bearerToken = accessToken,
                bodyJson = body
            )
            Unit
        }
    }

    override suspend fun listVerifiedBackups(
        accessToken: String,
        userId: String
    ): Result<List<CloudBackupMetadata>> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = "$supabaseUrl/rest/v1/backup_metadata" +
                "?user_id=eq.$userId&is_verified=eq.true&order=created_at.desc"
            val response = executeJsonRequest(
                method = "GET",
                endpoint = endpoint,
                bearerToken = accessToken,
                bodyJson = null
            )
            val arr = JSONArray(response)
            (0 until arr.length()).map { i ->
                parseMetadataRow(arr.getJSONObject(i))
            }
        }
    }

    override suspend fun deleteBackupMetadata(
        accessToken: String,
        backupId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            executeJsonRequest(
                method = "DELETE",
                endpoint = "$supabaseUrl/rest/v1/backup_metadata?id=eq.$backupId",
                bearerToken = accessToken,
                bodyJson = null
            )
            Unit
        }
    }

    private fun executeJsonRequest(
        method: String,
        endpoint: String,
        bearerToken: String,
        bodyJson: String?,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Authorization", "Bearer $bearerToken")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            for ((k, v) in extraHeaders) {
                setRequestProperty(k, v)
            }
            if (method == "PATCH") {
                requestMethod = "POST"
                setRequestProperty("X-HTTP-Method-Override", "PATCH")
            } else {
                requestMethod = method
            }
            if (bodyJson != null) {
                doOutput = true
            }
        }

        try {
            if (bodyJson != null) {
                conn.outputStream.use { out ->
                    out.write(bodyJson.toByteArray(StandardCharsets.UTF_8))
                }
            }
            val code = conn.responseCode
            if (code in 200..299) {
                return readStream(conn.inputStream)
            } else {
                val err = readStream(conn.errorStream)
                throw IllegalStateException(parseSupabaseError(code, err))
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseSessionJson(json: JSONObject): SupabaseSession {
        val accessToken = json.getString("access_token")
        val refreshToken = json.getString("refresh_token")
        val expiresIn = json.optLong("expires_in", 3600L)
        val expiresAt = json.optLong(
            "expires_at",
            (System.currentTimeMillis() / 1000L) + expiresIn
        )
        val userObj = json.getJSONObject("user")
        val userId = userObj.getString("id")
        val email = userObj.optString("email", "")
        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = email,
            expiresAtEpochSeconds = expiresAt
        )
    }

    private fun parseMetadataRow(obj: JSONObject): CloudBackupMetadata {
        val createdAtIso = obj.optString("created_at", "")
        val createdMillis = runCatching {
            Instant.parse(createdAtIso).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())

        val countsObj = when (val raw = obj.opt("record_counts")) {
            is JSONObject -> raw
            is String -> runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
            else -> JSONObject()
        }

        return CloudBackupMetadata(
            id = obj.getString("id"),
            userId = obj.getString("user_id"),
            storagePath = obj.getString("storage_path"),
            formatVersion = obj.optInt("format_version", 1),
            schemaVersion = obj.optInt("schema_version", 1),
            appVersion = obj.optString("app_version", "1.1.0"),
            sizeBytes = obj.optLong("size_bytes", 0L),
            sha256Checksum = obj.optString("sha256_checksum", ""),
            recordCounts = BackupSerializer.decodeRecordCounts(countsObj),
            deviceLabel = obj.optString("device_label", "Android Device"),
            isVerified = obj.optBoolean("is_verified", false),
            createdAtIso = createdAtIso,
            createdAtEpochMillis = createdMillis
        )
    }

    private fun readStream(stream: java.io.InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use {
            it.readText()
        }
    }

    private fun parseSupabaseError(statusCode: Int, rawError: String): String {
        val parsedMsg = runCatching {
            val obj = JSONObject(rawError)
            obj.optString("msg").takeIf { it.isNotBlank() }
                ?: obj.optString("error_description").takeIf { it.isNotBlank() }
                ?: obj.optString("message").takeIf { it.isNotBlank() }
                ?: obj.optString("error").takeIf { it.isNotBlank() }
        }.getOrNull()
        return parsedMsg ?: "Cloud service error (HTTP $statusCode)."
    }
}
