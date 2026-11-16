package com.lifeanalytics.app.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Standard, reviewed authenticated encryption for portable Life Analytics backups
 * (docs/11_DATA_ARCHITECTURE.md Section 14 & docs/12_TECHNICAL_ARCHITECTURE.md Section 7.4).
 *
 * - Key derivation: PBKDF2WithHmacSHA256 (210,000 iterations, 16-byte random salt, 256-bit key)
 * - Authenticated encryption: AES/GCM/NoPadding (12-byte random IV, 128-bit authentication tag)
 * - Compression: GZIP before encryption
 * - Integrity: AES-GCM authentication tag + SHA-256 checksums on both ciphertext and plaintext
 */
object BackupCrypto {
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val DEFAULT_KDF_ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val MIN_PASSPHRASE_LENGTH = 8

    private val secureRandom = SecureRandom()

    fun validatePassphrase(passphrase: String): Result<String> {
        val trimmed = passphrase.trim()
        if (trimmed.length < MIN_PASSPHRASE_LENGTH) {
            return Result.failure(
                IllegalArgumentException("Backup Recovery Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters.")
            )
        }
        return Result.success(trimmed)
    }

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun encryptPayload(
        plaintextJson: String,
        passphrase: String,
        manifest: BackupManifest,
        kdfIterations: Int = DEFAULT_KDF_ITERATIONS
    ): Result<EncryptedBackupEnvelope> = runCatching {
        val validPassphrase = validatePassphrase(passphrase).getOrThrow()
        val plaintextBytes = plaintextJson.toByteArray(StandardCharsets.UTF_8)
        val plaintextHash = sha256Hex(plaintextBytes)
        val compressedBytes = gzipCompress(plaintextBytes)

        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val secretKey = deriveAesKey(validPassphrase, salt, kdfIterations)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val aad = buildAssociatedData(
            backupId = manifest.backupId,
            formatVersion = manifest.formatVersion,
            schemaVersion = manifest.schemaVersion
        )
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(aad)
        val ciphertextBytes = cipher.doFinal(compressedBytes)
        val ciphertextHash = sha256Hex(ciphertextBytes)

        EncryptedBackupEnvelope(
            backupId = manifest.backupId,
            formatVersion = manifest.formatVersion,
            schemaVersion = manifest.schemaVersion,
            appVersion = manifest.appVersion,
            createdAtEpochMillis = manifest.createdAtEpochMillis,
            deviceLabel = manifest.deviceLabel,
            recordCounts = manifest.recordCounts,
            kdfAlgorithm = KDF_ALGORITHM,
            kdfIterations = kdfIterations,
            saltBase64 = Base64.getEncoder().encodeToString(salt),
            ivBase64 = Base64.getEncoder().encodeToString(iv),
            ciphertextSha256 = ciphertextHash,
            plaintextSha256 = plaintextHash,
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertextBytes)
        )
    }

    fun decryptPayload(
        envelope: EncryptedBackupEnvelope,
        passphrase: String
    ): Result<String> {
        val validPassphrase = validatePassphrase(passphrase).getOrElse {
            return Result.failure(
                IllegalArgumentException("Incorrect or invalid Backup Recovery Passphrase.")
            )
        }
        return try {
            if (envelope.formatVersion != BackupManifest.CURRENT_FORMAT_VERSION) {
                return Result.failure(
                    IllegalStateException("Unsupported backup format version (${envelope.formatVersion}).")
                )
            }
            if (envelope.schemaVersion > BackupManifest.CURRENT_SCHEMA_VERSION) {
                return Result.failure(
                    IllegalStateException(
                        "Backup requires database schema version ${envelope.schemaVersion}, " +
                            "but this app supports up to version ${BackupManifest.CURRENT_SCHEMA_VERSION}."
                    )
                )
            }

            val salt = Base64.getDecoder().decode(envelope.saltBase64)
            val iv = Base64.getDecoder().decode(envelope.ivBase64)
            val ciphertextBytes = Base64.getDecoder().decode(envelope.ciphertextBase64)

            val actualCiphertextHash = sha256Hex(ciphertextBytes)
            if (!actualCiphertextHash.equals(envelope.ciphertextSha256, ignoreCase = true)) {
                return Result.failure(
                    IllegalStateException("Backup file integrity check failed (ciphertext SHA-256 mismatch).")
                )
            }

            val secretKey = deriveAesKey(validPassphrase, salt, envelope.kdfIterations)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val aad = buildAssociatedData(
                backupId = envelope.backupId,
                formatVersion = envelope.formatVersion,
                schemaVersion = envelope.schemaVersion
            )
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            cipher.updateAAD(aad)

            val compressedBytes = cipher.doFinal(ciphertextBytes)
            val plaintextBytes = gzipDecompress(compressedBytes)
            val actualPlaintextHash = sha256Hex(plaintextBytes)
            if (!actualPlaintextHash.equals(envelope.plaintextSha256, ignoreCase = true)) {
                return Result.failure(
                    IllegalStateException("Backup payload checksum mismatch after decryption.")
                )
            }

            Result.success(String(plaintextBytes, StandardCharsets.UTF_8))
        } catch (_: AEADBadTagException) {
            Result.failure(
                IllegalArgumentException("Incorrect Backup Recovery Passphrase or tampered backup archive.")
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("Could not decrypt backup archive: ${e.message ?: "invalid archive format"}")
            )
        }
    }

    private fun deriveAesKey(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun buildAssociatedData(
        backupId: String,
        formatVersion: Int,
        schemaVersion: Int
    ): ByteArray {
        return "LifeAnalyticsBackup|id=$backupId|fmt=$formatVersion|schema=$schemaVersion"
            .toByteArray(StandardCharsets.UTF_8)
    }

    private fun gzipCompress(input: ByteArray): ByteArray {
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { gzip ->
            gzip.write(input)
        }
        return baos.toByteArray()
    }

    private fun gzipDecompress(compressed: ByteArray): ByteArray {
        val bais = ByteArrayInputStream(compressed)
        return GZIPInputStream(bais).use { gzip ->
            gzip.readBytes()
        }
    }
}
