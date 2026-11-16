package com.lifeanalytics.app.data.backup

import com.lifeanalytics.app.data.local.entity.DailyReviewEntity
import com.lifeanalytics.app.data.local.entity.FinanceTransactionEntity
import com.lifeanalytics.app.data.local.entity.FocusPauseIntervalEntity
import com.lifeanalytics.app.data.local.entity.FocusSessionEntity
import com.lifeanalytics.app.data.local.entity.HabitCompletionEntity
import com.lifeanalytics.app.data.local.entity.HabitEntity
import com.lifeanalytics.app.data.local.entity.HabitSchedulePeriodEntity
import com.lifeanalytics.app.data.local.entity.TaskEntity
import com.lifeanalytics.app.domain.model.AppPreferences

data class BackupRecordCounts(
    val tasks: Int = 0,
    val focusSessions: Int = 0,
    val focusPauseIntervals: Int = 0,
    val habits: Int = 0,
    val habitSchedulePeriods: Int = 0,
    val habitCompletions: Int = 0,
    val financeTransactions: Int = 0,
    val dailyReviews: Int = 0
) {
    val totalRecords: Int
        get() = tasks + focusSessions + focusPauseIntervals + habits +
            habitSchedulePeriods + habitCompletions + financeTransactions + dailyReviews
}

data class BackupManifest(
    val backupId: String,
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersion: String,
    val createdAtEpochMillis: Long,
    val deviceLabel: String,
    val recordCounts: BackupRecordCounts,
    val preferences: AppPreferences
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class BackupPayload(
    val manifest: BackupManifest,
    val tasks: List<TaskEntity>,
    val focusSessions: List<FocusSessionEntity>,
    val focusPauseIntervals: List<FocusPauseIntervalEntity>,
    val habits: List<HabitEntity>,
    val habitSchedulePeriods: List<HabitSchedulePeriodEntity>,
    val habitCompletions: List<HabitCompletionEntity>,
    val financeTransactions: List<FinanceTransactionEntity>,
    val dailyReviews: List<DailyReviewEntity>
)

/**
 * Unencrypted outer header + authenticated-encrypted inner payload (.labackup format).
 * Allows inspecting basic metadata and KDF parameters before decrypting, while keeping
 * all personal records (tasks, focus, habits, finance, reviews, preferences) encrypted
 * with AES-256-GCM.
 */
data class EncryptedBackupEnvelope(
    val backupId: String,
    val formatVersion: Int,
    val schemaVersion: Int,
    val appVersion: String,
    val createdAtEpochMillis: Long,
    val deviceLabel: String,
    val recordCounts: BackupRecordCounts,
    val kdfAlgorithm: String,
    val kdfIterations: Int,
    val saltBase64: String,
    val ivBase64: String,
    val ciphertextSha256: String,
    val plaintextSha256: String,
    val ciphertextBase64: String
)

enum class RestoreMode(val displayName: String, val description: String) {
    REPLACE_LOCAL(
        displayName = "Replace Local Data",
        description = "Creates a local safety backup first, then replaces current local records with this backup."
    ),
    MERGE_WITH_LOCAL(
        displayName = "Merge with Local Data",
        description = "Preserves newer local edits and adds missing records from this backup without deleting local-only items."
    )
}

data class CloudBackupMetadata(
    val id: String,
    val userId: String,
    val storagePath: String,
    val formatVersion: Int,
    val schemaVersion: Int,
    val appVersion: String,
    val sizeBytes: Long,
    val sha256Checksum: String,
    val recordCounts: BackupRecordCounts,
    val deviceLabel: String,
    val isVerified: Boolean,
    val createdAtIso: String,
    val createdAtEpochMillis: Long
)

data class SupabaseSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String,
    val expiresAtEpochSeconds: Long
)

data class BackupStatusState(
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val userId: String? = null,
    val hasCachedPassphrase: Boolean = false,
    val autoBackupDailyEnabled: Boolean = false,
    val lastBackupEpochMillis: Long? = null,
    val lastBackupDestination: String? = null,
    val lastBackupError: String? = null,
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val cloudBackups: List<CloudBackupMetadata> = emptyList()
)
