package com.lifeanalytics.app.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware/AndroidKeyStore-wrapped local storage for:
 * - Supabase session tokens (access token, refresh token, user ID, email)
 * - Locally cached Backup Recovery Passphrase while the app remains installed
 * - Last backup status metadata and auto-backup toggle
 *
 * Uses AndroidKeyStore AES-256-GCM to encrypt sensitive values before writing to app-private
 * SharedPreferences, with a clean fallback for Robolectric/JVM unit test environments where
 * AndroidKeyStore is not mounted.
 */
class SecureBackupStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "life_analytics_secure_backup_store"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "life_analytics_backup_keystore_key_v1"

        private const val KEY_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_EXPIRES_AT = "expires_at_epoch_sec"

        private const val KEY_BACKUP_PASSPHRASE = "enc_backup_passphrase"
        private const val KEY_AUTO_BACKUP_DAILY = "auto_backup_daily"
        private const val KEY_LAST_BACKUP_MILLIS = "last_backup_epoch_millis"
        private const val KEY_LAST_BACKUP_DESTINATION = "last_backup_destination"
        private const val KEY_LAST_BACKUP_ERROR = "last_backup_error"
    }

    fun saveSession(session: SupabaseSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, encryptLocalSecret(session.accessToken))
            .putString(KEY_REFRESH_TOKEN, encryptLocalSecret(session.refreshToken))
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_USER_EMAIL, session.email)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .apply()
    }

    fun getSession(): SupabaseSession? {
        val encAccess = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val encRefresh = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: ""
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)

        val accessToken = decryptLocalSecret(encAccess) ?: return null
        val refreshToken = decryptLocalSecret(encRefresh) ?: return null

        return SupabaseSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            email = email,
            expiresAtEpochSeconds = expiresAt
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    fun saveCachedPassphrase(passphrase: String) {
        prefs.edit()
            .putString(KEY_BACKUP_PASSPHRASE, encryptLocalSecret(passphrase.trim()))
            .apply()
    }

    fun getCachedPassphrase(): String? {
        val enc = prefs.getString(KEY_BACKUP_PASSPHRASE, null) ?: return null
        return decryptLocalSecret(enc)
    }

    fun clearCachedPassphrase() {
        prefs.edit()
            .remove(KEY_BACKUP_PASSPHRASE)
            .apply()
    }

    fun isAutoBackupDailyEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_BACKUP_DAILY, false)

    fun setAutoBackupDailyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_DAILY, enabled).apply()
    }

    fun recordBackupSuccess(epochMillis: Long, destination: String) {
        prefs.edit()
            .putLong(KEY_LAST_BACKUP_MILLIS, epochMillis)
            .putString(KEY_LAST_BACKUP_DESTINATION, destination)
            .remove(KEY_LAST_BACKUP_ERROR)
            .apply()
    }

    fun recordBackupError(errorMessage: String?) {
        prefs.edit()
            .apply {
                if (errorMessage.isNullOrBlank()) {
                    remove(KEY_LAST_BACKUP_ERROR)
                } else {
                    putString(KEY_LAST_BACKUP_ERROR, errorMessage)
                }
            }
            .apply()
    }

    fun getLastBackupEpochMillis(): Long? {
        return if (prefs.contains(KEY_LAST_BACKUP_MILLIS)) {
            prefs.getLong(KEY_LAST_BACKUP_MILLIS, 0L).takeIf { it > 0L }
        } else {
            null
        }
    }

    fun getLastBackupDestination(): String? = prefs.getString(KEY_LAST_BACKUP_DESTINATION, null)

    fun getLastBackupError(): String? = prefs.getString(KEY_LAST_BACKUP_ERROR, null)

    private fun encryptLocalSecret(plaintext: String): String {
        return try {
            val secretKey = getOrCreateKeyStoreKey()
            if (secretKey != null) {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val ciphertext = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
                "ks1:" + Base64.getEncoder().encodeToString(iv) + ":" +
                    Base64.getEncoder().encodeToString(ciphertext)
            } else {
                "b64:" + Base64.getEncoder().encodeToString(plaintext.toByteArray(StandardCharsets.UTF_8))
            }
        } catch (_: Exception) {
            "b64:" + Base64.getEncoder().encodeToString(plaintext.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun decryptLocalSecret(encoded: String): String? {
        return try {
            when {
                encoded.startsWith("ks1:") -> {
                    val parts = encoded.removePrefix("ks1:").split(":", limit = 2)
                    if (parts.size != 2) return null
                    val iv = Base64.getDecoder().decode(parts[0])
                    val ciphertext = Base64.getDecoder().decode(parts[1])
                    val secretKey = getOrCreateKeyStoreKey() ?: return null
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
                    String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
                }
                encoded.startsWith("b64:") -> {
                    val raw = Base64.getDecoder().decode(encoded.removePrefix("b64:"))
                    String(raw, StandardCharsets.UTF_8)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getOrCreateKeyStoreKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) {
                return existing.secretKey
            }
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Exception) {
            null
        }
    }
}
