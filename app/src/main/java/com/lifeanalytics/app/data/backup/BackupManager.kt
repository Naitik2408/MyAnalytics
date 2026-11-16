package com.lifeanalytics.app.data.backup

import android.content.Context
import android.os.Build
import androidx.room.withTransaction
import com.lifeanalytics.app.BuildConfig
import com.lifeanalytics.app.core.time.SystemTimeProvider
import com.lifeanalytics.app.core.time.TimeProvider
import com.lifeanalytics.app.data.local.LifeAnalyticsDatabase
import com.lifeanalytics.app.data.repository.PreferencesRepository
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/**
 * Coordinates:
 * 1. Consistent local snapshot extraction across all 8 Room entities + DataStore preferences.
 * 2. Authenticated encryption (PBKDF2-HMAC-SHA256 + AES-256-GCM) and integrity verification.
 * 3. Local file export/import (.labackup) and pre-restore safety snapshot creation.
 * 4. Supabase Cloud Auth, upload, download-back verification, 5-version retention, and restore.
 * 5. Transactional restore supporting both REPLACE_LOCAL and MERGE_WITH_LOCAL modes.
 */
class BackupManager(
    private val context: Context,
    private val database: LifeAnalyticsDatabase,
    private val preferencesRepository: PreferencesRepository,
    private val secureStore: SecureBackupStore = SecureBackupStore(context),
    private val remoteClient: CloudBackupRemoteDataSource = SupabaseCloudClient(),
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val kdfIterations: Int = BackupCrypto.DEFAULT_KDF_ITERATIONS
) {
    companion object {
        const val MAX_RETAINED_CLOUD_BACKUPS = 5
        private const val SAFETY_BACKUP_DIR = "pre_restore_safety_backups"
    }

    private val taskDao = database.taskDao()
    private val focusDao = database.focusDao()
    private val habitDao = database.habitDao()
    private val financeDao = database.financeDao()
    private val reviewDao = database.dailyReviewDao()

    private val _statusState = MutableStateFlow(loadInitialStatusState())
    val statusState: StateFlow<BackupStatusState> = _statusState.asStateFlow()

    private fun loadInitialStatusState(): BackupStatusState {
        val session = secureStore.getSession()
        return BackupStatusState(
            isSignedIn = session != null,
            userEmail = session?.email,
            userId = session?.userId,
            hasCachedPassphrase = !secureStore.getCachedPassphrase().isNullOrBlank(),
            autoBackupDailyEnabled = secureStore.isAutoBackupDailyEnabled(),
            lastBackupEpochMillis = secureStore.getLastBackupEpochMillis(),
            lastBackupDestination = secureStore.getLastBackupDestination(),
            lastBackupError = secureStore.getLastBackupError()
        )
    }

    // =========================================================================
    // SNAPSHOT EXTRACTION & ARCHIVE CREATION
    // =========================================================================

    suspend fun extractDatabasePayload(
        backupId: String = UUID.randomUUID().toString(),
        deviceLabel: String = defaultDeviceLabel()
    ): BackupPayload {
        val preferences = preferencesRepository.preferencesFlow.first()
        val now = timeProvider.nowEpochMillis()

        return database.withTransaction {
            val tasks = taskDao.getAllTasks()
            val focusSessions = focusDao.getAllSessionEntities()
            val focusPauseIntervals = focusDao.getAllPauseIntervalEntities()
            val habits = habitDao.getAllHabitEntities()
            val habitSchedulePeriods = habitDao.getAllSchedulePeriodEntities()
            val habitCompletions = habitDao.getAllCompletionEntities()
            val financeTransactions = financeDao.getAllTransactionEntities()
            val dailyReviews = reviewDao.getAllReviewEntities()

            val counts = BackupRecordCounts(
                tasks = tasks.size,
                focusSessions = focusSessions.size,
                focusPauseIntervals = focusPauseIntervals.size,
                habits = habits.size,
                habitSchedulePeriods = habitSchedulePeriods.size,
                habitCompletions = habitCompletions.size,
                financeTransactions = financeTransactions.size,
                dailyReviews = dailyReviews.size
            )

            val manifest = BackupManifest(
                backupId = backupId,
                formatVersion = BackupManifest.CURRENT_FORMAT_VERSION,
                schemaVersion = BackupManifest.CURRENT_SCHEMA_VERSION,
                appVersion = BuildConfig.VERSION_NAME,
                createdAtEpochMillis = now,
                deviceLabel = deviceLabel,
                recordCounts = counts,
                preferences = preferences
            )

            BackupPayload(
                manifest = manifest,
                tasks = tasks,
                focusSessions = focusSessions,
                focusPauseIntervals = focusPauseIntervals,
                habits = habits,
                habitSchedulePeriods = habitSchedulePeriods,
                habitCompletions = habitCompletions,
                financeTransactions = financeTransactions,
                dailyReviews = dailyReviews
            )
        }
    }

    /**
     * Creates an encrypted backup archive (.labackup JSON string) and immediately verifies it
     * by decrypting and validating the manifest and record counts before returning success.
     */
    suspend fun createVerifiedEncryptedArchive(
        passphrase: String,
        deviceLabel: String = defaultDeviceLabel()
    ): Result<Pair<EncryptedBackupEnvelope, String>> {
        return runCatching {
            val validPassphrase = BackupCrypto.validatePassphrase(passphrase).getOrThrow()
            val payload = extractDatabasePayload(deviceLabel = deviceLabel)
            val payloadJson = BackupSerializer.encodePayloadToJson(payload)
            val envelope = BackupCrypto.encryptPayload(
                plaintextJson = payloadJson,
                passphrase = validPassphrase,
                manifest = payload.manifest,
                kdfIterations = kdfIterations
            ).getOrThrow()

            val envelopeJson = BackupSerializer.encodeEnvelopeToJson(envelope)

            // Read-back verification before reporting success (docs/12_TECHNICAL_ARCHITECTURE.md 7.4 rule 8)
            val parsedEnvelope = BackupSerializer.decodeEnvelopeFromJson(envelopeJson).getOrThrow()
            val decryptedJson = BackupCrypto.decryptPayload(parsedEnvelope, validPassphrase).getOrThrow()
            val verifiedPayload = BackupSerializer.decodeAndValidatePayload(decryptedJson).getOrThrow()
            check(verifiedPayload.manifest.recordCounts == payload.manifest.recordCounts) {
                "Backup verification failed: record counts did not match."
            }

            secureStore.saveCachedPassphrase(validPassphrase)
            _statusState.update { it.copy(hasCachedPassphrase = true) }

            envelope to envelopeJson
        }
    }

    /**
     * Decrypts and validates a `.labackup` archive in memory without modifying the database,
     * returning the full [BackupPayload] so the UI can show a Restore Preview dialog first.
     */
    fun inspectAndDecryptArchive(
        envelopeJson: String,
        passphrase: String
    ): Result<BackupPayload> {
        val envelope = BackupSerializer.decodeEnvelopeFromJson(envelopeJson).getOrElse {
            return Result.failure(it)
        }
        val decryptedJson = BackupCrypto.decryptPayload(envelope, passphrase).getOrElse {
            return Result.failure(it)
        }
        return BackupSerializer.decodeAndValidatePayload(decryptedJson)
    }

    fun inspectEnvelopeHeaderOnly(envelopeJson: String): Result<EncryptedBackupEnvelope> {
        return BackupSerializer.decodeEnvelopeFromJson(envelopeJson)
    }

    // =========================================================================
    // TRANSACTIONAL RESTORE (REPLACE OR MERGE) WITH PRE-RESTORE SAFETY SNAPSHOT
    // =========================================================================

    /**
     * Restores a validated [BackupPayload] into the live Room database and DataStore preferences.
     *
     * Safety guarantees:
     * 1. Validates payload relational integrity and counts before touching Room.
     * 2. Writes an automatic pre-restore safety snapshot before replacing local data.
     * 3. Executes all table mutations inside a single atomic `database.withTransaction` block.
     *    If any error occurs, SQLite rolls back the entire transaction leaving pre-restore data intact.
     */
    suspend fun restoreValidatedPayload(
        payload: BackupPayload,
        mode: RestoreMode,
        safetyPassphrase: String? = null
    ): Result<BackupRecordCounts> = runCatching {
        BackupSerializer.validatePayloadIntegrity(payload)

        // Create pre-restore safety copy before replacement if local data exists
        val preRestorePayload = extractDatabasePayload()
        if (preRestorePayload.manifest.recordCounts.totalRecords > 0) {
            val effectivePassphrase = safetyPassphrase?.takeIf { it.length >= 8 }
                ?: secureStore.getCachedPassphrase()
                ?: "LifeAnalyticsSafetySnapshotKey"
            writeLocalSafetySnapshot(preRestorePayload, effectivePassphrase)
        }

        // Normalize any task_id references in focus_sessions where the referenced task is not present
        val taskIdsInBackup = payload.tasks.map { it.id }.toSet()

        database.withTransaction {
            when (mode) {
                RestoreMode.REPLACE_LOCAL -> {
                    // Delete children first, then parents
                    focusDao.deleteAllPauseIntervals()
                    focusDao.deleteAllSessions()
                    habitDao.deleteAllCompletions()
                    habitDao.deleteAllSchedulePeriods()
                    habitDao.deleteAllHabits()
                    taskDao.deleteAllTasks()
                    financeDao.deleteAllTransactions()
                    reviewDao.deleteAllReviews()

                    // Insert parents first, then children
                    taskDao.upsertAllTasks(payload.tasks)
                    habitDao.upsertAllHabits(payload.habits)
                    financeDao.upsertAllTransactions(payload.financeTransactions)
                    reviewDao.upsertAllReviews(payload.dailyReviews)

                    val safeSessions = payload.focusSessions.map { session ->
                        if (session.taskId != null && session.taskId !in taskIdsInBackup) {
                            session.copy(taskId = null)
                        } else {
                            session
                        }
                    }
                    focusDao.upsertAllSessions(safeSessions)
                    focusDao.upsertAllPauseIntervals(payload.focusPauseIntervals)
                    habitDao.upsertAllSchedulePeriods(payload.habitSchedulePeriods)
                    habitDao.upsertAllCompletions(payload.habitCompletions)
                }

                RestoreMode.MERGE_WITH_LOCAL -> {
                    // Merge Tasks by ID, keeping whichever record has newer updatedAtEpochMillis
                    val localTasksById = taskDao.getAllTasks().associateBy { it.id }
                    val mergedTasks = payload.tasks.mapNotNull { incoming ->
                        val local = localTasksById[incoming.id]
                        if (local == null || incoming.updatedAtEpochMillis >= local.updatedAtEpochMillis) {
                            incoming
                        } else {
                            null
                        }
                    }
                    if (mergedTasks.isNotEmpty()) {
                        taskDao.upsertAllTasks(mergedTasks)
                    }

                    val allKnownTaskIds = taskDao.getAllTasks().map { it.id }.toSet()

                    // Merge Habits by ID
                    val localHabitsById = habitDao.getAllHabitEntities().associateBy { it.id }
                    val mergedHabits = payload.habits.mapNotNull { incoming ->
                        val local = localHabitsById[incoming.id]
                        if (local == null || incoming.updatedAtEpochMillis >= local.updatedAtEpochMillis) {
                            incoming
                        } else {
                            null
                        }
                    }
                    if (mergedHabits.isNotEmpty()) {
                        habitDao.upsertAllHabits(mergedHabits)
                    }

                    // Merge Habit Schedule Periods by ID (avoiding duplicates for the same habit + effectiveFrom)
                    val localPeriods = habitDao.getAllSchedulePeriodEntities()
                    val localPeriodIds = localPeriods.map { it.id }.toSet()
                    val localHabitEffectivePairs = localPeriods.map { it.habitId to it.effectiveFrom }.toSet()
                    val mergedPeriods = payload.habitSchedulePeriods.filter { incoming ->
                        incoming.id !in localPeriodIds &&
                            (incoming.habitId to incoming.effectiveFrom) !in localHabitEffectivePairs
                    }
                    if (mergedPeriods.isNotEmpty()) {
                        habitDao.upsertAllSchedulePeriods(mergedPeriods)
                    }

                    // Merge Habit Completions respecting unique (habit_id, completion_date)
                    val localCompletionsByPair = habitDao.getAllCompletionEntities()
                        .associateBy { it.habitId to it.completionDate }
                    val mergedCompletions = payload.habitCompletions.mapNotNull { incoming ->
                        val local = localCompletionsByPair[incoming.habitId to incoming.completionDate]
                        when {
                            local == null -> incoming
                            incoming.updatedAtEpochMillis >= local.updatedAtEpochMillis ->
                                incoming.copy(id = local.id)
                            else -> null
                        }
                    }
                    if (mergedCompletions.isNotEmpty()) {
                        habitDao.upsertAllCompletions(mergedCompletions)
                    }

                    // Merge Focus Sessions by ID
                    val localSessionsById = focusDao.getAllSessionEntities().associateBy { it.id }
                    val mergedSessions = payload.focusSessions.mapNotNull { incoming ->
                        val local = localSessionsById[incoming.id]
                        if (local == null || incoming.updatedAtEpochMillis >= local.updatedAtEpochMillis) {
                            if (incoming.taskId != null && incoming.taskId !in allKnownTaskIds) {
                                incoming.copy(taskId = null)
                            } else {
                                incoming
                            }
                        } else {
                            null
                        }
                    }
                    if (mergedSessions.isNotEmpty()) {
                        focusDao.upsertAllSessions(mergedSessions)
                    }

                    val localPausesById = focusDao.getAllPauseIntervalEntities().associateBy { it.id }
                    val mergedPauses = payload.focusPauseIntervals.filter { incoming ->
                        incoming.id !in localPausesById
                    }
                    if (mergedPauses.isNotEmpty()) {
                        focusDao.upsertAllPauseIntervals(mergedPauses)
                    }

                    // Merge Finance Transactions by ID
                    val localTxById = financeDao.getAllTransactionEntities().associateBy { it.id }
                    val mergedTx = payload.financeTransactions.mapNotNull { incoming ->
                        val local = localTxById[incoming.id]
                        if (local == null || incoming.updatedAtEpochMillis >= local.updatedAtEpochMillis) {
                            incoming
                        } else {
                            null
                        }
                    }
                    if (mergedTx.isNotEmpty()) {
                        financeDao.upsertAllTransactions(mergedTx)
                    }

                    // Merge Daily Reviews respecting unique review_date
                    val localReviewsByDate = reviewDao.getAllReviewEntities()
                        .associateBy { it.reviewDate }
                    val mergedReviews = payload.dailyReviews.mapNotNull { incoming ->
                        val local = localReviewsByDate[incoming.reviewDate]
                        when {
                            local == null -> incoming
                            incoming.updatedAtEpochMillis >= local.updatedAtEpochMillis ->
                                incoming.copy(id = local.id)
                            else -> null
                        }
                    }
                    if (mergedReviews.isNotEmpty()) {
                        reviewDao.upsertAllReviews(mergedReviews)
                    }
                }
            }
        }

        preferencesRepository.restorePreferences(payload.manifest.preferences)
        payload.manifest.recordCounts
    }

    private fun writeLocalSafetySnapshot(payload: BackupPayload, passphrase: String) {
        runCatching {
            val dir = File(context.filesDir, SAFETY_BACKUP_DIR).apply { mkdirs() }
            val json = BackupSerializer.encodePayloadToJson(payload)
            val env = BackupCrypto.encryptPayload(
                plaintextJson = json,
                passphrase = passphrase,
                manifest = payload.manifest,
                kdfIterations = kdfIterations
            ).getOrThrow()
            val envJson = BackupSerializer.encodeEnvelopeToJson(env)
            val file = File(dir, "pre_restore_${payload.manifest.createdAtEpochMillis}.labackup")
            file.writeText(envJson, StandardCharsets.UTF_8)
            // Keep at most 3 local pre-restore safety snapshots
            dir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?.drop(3)
                ?.forEach { it.delete() }
        }
    }

    // =========================================================================
    // SUPABASE AUTHENTICATION
    // =========================================================================

    suspend fun signUp(email: String, password: String): Result<SupabaseSession> {
        _statusState.update { it.copy(isBusy = true, statusMessage = null, lastBackupError = null) }
        val result = remoteClient.signUpWithEmailPassword(email, password)
        result.fold(
            onSuccess = { session ->
                secureStore.saveSession(session)
                _statusState.update {
                    it.copy(
                        isBusy = false,
                        isSignedIn = true,
                        userEmail = session.email,
                        userId = session.userId,
                        statusMessage = "Signed in as ${session.email}"
                    )
                }
                refreshCloudBackupsList()
            },
            onFailure = { err ->
                _statusState.update {
                    it.copy(
                        isBusy = false,
                        lastBackupError = err.message ?: "Sign-up failed."
                    )
                }
            }
        )
        return result
    }

    suspend fun signIn(email: String, password: String): Result<SupabaseSession> {
        _statusState.update { it.copy(isBusy = true, statusMessage = null, lastBackupError = null) }
        val result = remoteClient.signInWithEmailPassword(email, password)
        result.fold(
            onSuccess = { session ->
                secureStore.saveSession(session)
                _statusState.update {
                    it.copy(
                        isBusy = false,
                        isSignedIn = true,
                        userEmail = session.email,
                        userId = session.userId,
                        statusMessage = "Signed in as ${session.email}"
                    )
                }
                refreshCloudBackupsList()
            },
            onFailure = { err ->
                _statusState.update {
                    it.copy(
                        isBusy = false,
                        lastBackupError = err.message ?: "Sign-in failed."
                    )
                }
            }
        )
        return result
    }

    suspend fun signOut() {
        val currentSession = secureStore.getSession()
        if (currentSession != null) {
            remoteClient.signOut(currentSession.accessToken)
        }
        secureStore.clearSession()
        _statusState.update {
            it.copy(
                isSignedIn = false,
                userEmail = null,
                userId = null,
                cloudBackups = emptyList(),
                statusMessage = "Signed out of Cloud Backup."
            )
        }
    }

    fun saveRecoveryPassphrase(passphrase: String): Result<Unit> {
        val valid = BackupCrypto.validatePassphrase(passphrase).getOrElse {
            return Result.failure(it)
        }
        secureStore.saveCachedPassphrase(valid)
        _statusState.update {
            it.copy(
                hasCachedPassphrase = true,
                statusMessage = "Backup Recovery Passphrase saved on this device.",
                lastBackupError = null
            )
        }
        return Result.success(Unit)
    }

    fun getCachedPassphrase(): String? = secureStore.getCachedPassphrase()

    fun setAutoBackupDaily(enabled: Boolean) {
        secureStore.setAutoBackupDailyEnabled(enabled)
        if (enabled) {
            BackupScheduler.scheduleDailyBackup(context)
        } else {
            BackupScheduler.cancelDailyBackup(context)
        }
        _statusState.update {
            it.copy(
                autoBackupDailyEnabled = enabled,
                statusMessage = if (enabled) {
                    "Daily automatic cloud backup enabled."
                } else {
                    "Daily automatic cloud backup turned off."
                }
            )
        }
    }

    // =========================================================================
    // SUPABASE CLOUD BACKUP, VERIFICATION & RETENTION
    // =========================================================================

    suspend fun performCloudBackup(
        explicitPassphrase: String? = null
    ): Result<CloudBackupMetadata> {
        _statusState.update { it.copy(isBusy = true, statusMessage = null, lastBackupError = null) }

        val passphrase = explicitPassphrase?.takeIf { it.isNotBlank() }
            ?: secureStore.getCachedPassphrase()
        if (passphrase.isNullOrBlank()) {
            val err = IllegalStateException("Set your Backup Recovery Passphrase before backing up.")
            secureStore.recordBackupError(err.message)
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        val session = getValidSessionOrRefresh().getOrElse { err ->
            secureStore.recordBackupError(err.message)
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        val archiveResult = createVerifiedEncryptedArchive(passphrase)
        val (envelope, envelopeJson) = archiveResult.getOrElse { err ->
            secureStore.recordBackupError(err.message)
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        val envelopeBytes = envelopeJson.toByteArray(StandardCharsets.UTF_8)
        val archiveSha256 = BackupCrypto.sha256Hex(envelopeBytes)
        val storagePath = "${session.userId}/${envelope.backupId}.labackup"

        // 1. Upload encrypted archive to private user-backups bucket
        remoteClient.uploadBackupObject(
            accessToken = session.accessToken,
            storagePath = storagePath,
            envelopeJsonBytes = envelopeBytes
        ).getOrElse { err ->
            secureStore.recordBackupError(err.message)
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        // 2. Insert initial metadata record (is_verified = false until read-back check passes)
        val initialMetadata = CloudBackupMetadata(
            id = envelope.backupId,
            userId = session.userId,
            storagePath = storagePath,
            formatVersion = envelope.formatVersion,
            schemaVersion = envelope.schemaVersion,
            appVersion = envelope.appVersion,
            sizeBytes = envelopeBytes.size.toLong(),
            sha256Checksum = archiveSha256,
            recordCounts = envelope.recordCounts,
            deviceLabel = envelope.deviceLabel,
            isVerified = false,
            createdAtIso = Instant.ofEpochMilli(envelope.createdAtEpochMillis).toString(),
            createdAtEpochMillis = envelope.createdAtEpochMillis
        )
        remoteClient.insertBackupMetadata(session.accessToken, initialMetadata).getOrElse { err ->
            secureStore.recordBackupError(err.message)
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        // 3. Download back and verify SHA-256 + AES-GCM decryption before marking verified
        val downloadedBytes = remoteClient.downloadBackupObject(
            accessToken = session.accessToken,
            storagePath = storagePath
        ).getOrElse { err ->
            secureStore.recordBackupError("Upload verification failed: ${err.message}")
            _statusState.update {
                it.copy(isBusy = false, lastBackupError = "Upload verification failed: ${err.message}")
            }
            return Result.failure(err)
        }

        val downloadedSha256 = BackupCrypto.sha256Hex(downloadedBytes)
        if (!downloadedSha256.equals(archiveSha256, ignoreCase = true)) {
            val err = IllegalStateException("Cloud backup verification failed: SHA-256 mismatch.")
            secureStore.recordBackupError(err.message)
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        val downloadedEnvelopeJson = String(downloadedBytes, StandardCharsets.UTF_8)
        inspectAndDecryptArchive(downloadedEnvelopeJson, passphrase).getOrElse { err ->
            secureStore.recordBackupError("Cloud backup verification failed: ${err.message}")
            _statusState.update {
                it.copy(isBusy = false, lastBackupError = "Cloud backup verification failed: ${err.message}")
            }
            return Result.failure(err)
        }

        // 4. Mark metadata row as verified
        remoteClient.markBackupVerified(session.accessToken, envelope.backupId).getOrElse { err ->
            secureStore.recordBackupError(err.message)
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        // 5. Prune older backups beyond MAX_RETAINED_CLOUD_BACKUPS (5) only AFTER new backup is verified
        val allVerified = remoteClient.listVerifiedBackups(session.accessToken, session.userId)
            .getOrDefault(emptyList())
        if (allVerified.size > MAX_RETAINED_CLOUD_BACKUPS) {
            val toDelete = allVerified.drop(MAX_RETAINED_CLOUD_BACKUPS)
            for (old in toDelete) {
                remoteClient.deleteBackupObject(session.accessToken, old.storagePath)
                remoteClient.deleteBackupMetadata(session.accessToken, old.id)
            }
        }

        val retainedList = remoteClient.listVerifiedBackups(session.accessToken, session.userId)
            .getOrDefault(allVerified.take(MAX_RETAINED_CLOUD_BACKUPS))

        val verifiedMetadata = initialMetadata.copy(isVerified = true)
        secureStore.recordBackupSuccess(envelope.createdAtEpochMillis, "Supabase Cloud")
        _statusState.update {
            it.copy(
                isBusy = false,
                lastBackupEpochMillis = envelope.createdAtEpochMillis,
                lastBackupDestination = "Supabase Cloud",
                lastBackupError = null,
                cloudBackups = retainedList,
                statusMessage = "Encrypted cloud backup verified (${envelope.recordCounts.totalRecords} records)."
            )
        }
        return Result.success(verifiedMetadata)
    }

    suspend fun refreshCloudBackupsList(): Result<List<CloudBackupMetadata>> {
        val session = getValidSessionOrRefresh().getOrElse { err ->
            return Result.failure(err)
        }
        val listResult = remoteClient.listVerifiedBackups(session.accessToken, session.userId)
        listResult.onSuccess { backups ->
            _statusState.update { it.copy(cloudBackups = backups) }
        }
        return listResult
    }

    suspend fun downloadAndDecryptCloudBackup(
        metadata: CloudBackupMetadata,
        passphrase: String
    ): Result<BackupPayload> {
        _statusState.update { it.copy(isBusy = true, statusMessage = null, lastBackupError = null) }
        val session = getValidSessionOrRefresh().getOrElse { err ->
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        val bytes = remoteClient.downloadBackupObject(session.accessToken, metadata.storagePath)
            .getOrElse { err ->
                _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
                return Result.failure(err)
            }

        val actualSha = BackupCrypto.sha256Hex(bytes)
        if (metadata.sha256Checksum.isNotBlank() && !actualSha.equals(metadata.sha256Checksum, ignoreCase = true)) {
            val err = IllegalStateException("Downloaded backup checksum does not match verified metadata.")
            _statusState.update { it.copy(isBusy = false, lastBackupError = err.message) }
            return Result.failure(err)
        }

        val json = String(bytes, StandardCharsets.UTF_8)
        val payloadResult = inspectAndDecryptArchive(json, passphrase)
        payloadResult.fold(
            onSuccess = {
                secureStore.saveCachedPassphrase(passphrase)
                _statusState.update { state ->
                    state.copy(isBusy = false, hasCachedPassphrase = true, lastBackupError = null)
                }
            },
            onFailure = { err ->
                _statusState.update { state ->
                    state.copy(isBusy = false, lastBackupError = err.message)
                }
            }
        )
        return payloadResult
    }

    fun recordLocalExportSuccess(epochMillis: Long) {
        secureStore.recordBackupSuccess(epochMillis, "Local Encrypted File (.labackup)")
        _statusState.update {
            it.copy(
                lastBackupEpochMillis = epochMillis,
                lastBackupDestination = "Local Encrypted File (.labackup)",
                lastBackupError = null,
                statusMessage = "Encrypted backup exported and verified."
            )
        }
    }

    fun updateStatusMessage(message: String?, error: String? = null) {
        if (error != null) {
            secureStore.recordBackupError(error)
        }
        _statusState.update {
            it.copy(statusMessage = message, lastBackupError = error)
        }
    }

    private suspend fun getValidSessionOrRefresh(): Result<SupabaseSession> {
        val current = secureStore.getSession()
            ?: return Result.failure(IllegalStateException("Sign in to your cloud backup account first."))
        val nowSec = timeProvider.nowEpochMillis() / 1000L
        if (current.expiresAtEpochSeconds > nowSec + 60L) {
            return Result.success(current)
        }
        return remoteClient.refreshSession(current.refreshToken).map { refreshed ->
            secureStore.saveSession(refreshed)
            refreshed
        }
    }

    private fun defaultDeviceLabel(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        val model = Build.MODEL?.trim().orEmpty()
        return listOf(manufacturer, model)
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .ifBlank { "Android Device" }
    }
}
