package com.lifeanalytics.app.data.backup

import com.lifeanalytics.app.core.common.SupportedCurrency
import com.lifeanalytics.app.data.local.entity.DailyReviewEntity
import com.lifeanalytics.app.data.local.entity.FinanceTransactionEntity
import com.lifeanalytics.app.data.local.entity.FocusPauseIntervalEntity
import com.lifeanalytics.app.data.local.entity.FocusSessionEntity
import com.lifeanalytics.app.data.local.entity.HabitCompletionEntity
import com.lifeanalytics.app.data.local.entity.HabitEntity
import com.lifeanalytics.app.data.local.entity.HabitSchedulePeriodEntity
import com.lifeanalytics.app.data.local.entity.TaskEntity
import com.lifeanalytics.app.domain.model.AppPreferences
import com.lifeanalytics.app.domain.model.AppThemeOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupIntegrationTest {

    private val samplePreferences = AppPreferences(
        themeOption = AppThemeOption.LIGHT,
        currencyCode = "INR"
    )

    private val sampleTask = TaskEntity(
        id = UUID.randomUUID().toString(),
        title = "Study System Design",
        category = "Study",
        plannedDate = "2026-11-16",
        plannedTime = "14:30",
        estimatedDurationMinutes = 60,
        status = "COMPLETED",
        completedAtEpochMillis = 1794825392000L,
        sortOrder = 0,
        createdAtEpochMillis = 1794820000000L,
        updatedAtEpochMillis = 1794825392000L
    )

    private val sampleSession = FocusSessionEntity(
        id = UUID.randomUUID().toString(),
        taskId = sampleTask.id,
        taskTitleSnapshot = "Study System Design",
        sessionType = "TASK_LINKED",
        description = "Deep work on distributed systems",
        startedAtEpochMillis = 1794821000000L,
        startedLocalDate = "2026-11-16",
        timezoneId = "Asia/Kolkata",
        endedAtEpochMillis = 1794824600000L,
        status = "COMPLETED",
        focusedDurationSeconds = 3000L,
        lastConfirmedAtEpochMillis = 1794824600000L,
        interruptionReason = null,
        createdAtEpochMillis = 1794821000000L,
        updatedAtEpochMillis = 1794824600000L
    )

    private val samplePause = FocusPauseIntervalEntity(
        id = UUID.randomUUID().toString(),
        sessionId = sampleSession.id,
        pausedAtEpochMillis = 1794822500000L,
        resumedAtEpochMillis = 1794823100000L
    )

    private val sampleHabit = HabitEntity(
        id = UUID.randomUUID().toString(),
        name = "Drink Water",
        isActive = true,
        createdAtEpochMillis = 1794800000000L,
        updatedAtEpochMillis = 1794800000000L,
        deactivatedAtEpochMillis = null,
        deactivatedFromLocalDate = null
    )

    private val sampleSchedule = HabitSchedulePeriodEntity(
        id = UUID.randomUUID().toString(),
        habitId = sampleHabit.id,
        effectiveFrom = "2026-11-01",
        effectiveTo = null,
        scheduleType = "EVERY_DAY",
        weekdaysCsv = "1,2,3,4,5,6,7",
        createdAtEpochMillis = 1794800000000L
    )

    private val sampleCompletion = HabitCompletionEntity(
        id = UUID.randomUUID().toString(),
        habitId = sampleHabit.id,
        completionDate = "2026-11-16",
        isCompleted = true,
        completedAtEpochMillis = 1794825000000L,
        updatedAtEpochMillis = 1794825000000L
    )

    private val sampleTransaction = FinanceTransactionEntity(
        id = UUID.randomUUID().toString(),
        type = "EXPENSE",
        amountMinor = 24900L, // ₹249.00
        currencyCode = "INR",
        category = "Food",
        transactionDate = "2026-11-16",
        note = "Lunch with team",
        createdAtEpochMillis = 1794820000000L,
        updatedAtEpochMillis = 1794820000000L
    )

    private val sampleReview = DailyReviewEntity(
        id = UUID.randomUUID().toString(),
        reviewDate = "2026-11-16",
        wentWell = "Completed full study milestone.",
        improveTomorrow = "Take breaks more consistently.",
        remember = "Key insight on distributed locking.",
        status = "COMPLETED",
        createdAtEpochMillis = 1794826000000L,
        updatedAtEpochMillis = 1794826000000L
    )

    private fun createTestPayload(): BackupPayload {
        val counts = BackupRecordCounts(
            tasks = 1,
            focusSessions = 1,
            focusPauseIntervals = 1,
            habits = 1,
            habitSchedulePeriods = 1,
            habitCompletions = 1,
            financeTransactions = 1,
            dailyReviews = 1
        )
        val manifest = BackupManifest(
            backupId = UUID.randomUUID().toString(),
            formatVersion = 1,
            schemaVersion = 1,
            appVersion = "1.1.0",
            createdAtEpochMillis = System.currentTimeMillis(),
            deviceLabel = "Pixel 8 Pro",
            recordCounts = counts,
            preferences = samplePreferences
        )
        return BackupPayload(
            manifest = manifest,
            tasks = listOf(sampleTask),
            focusSessions = listOf(sampleSession),
            focusPauseIntervals = listOf(samplePause),
            habits = listOf(sampleHabit),
            habitSchedulePeriods = listOf(sampleSchedule),
            habitCompletions = listOf(sampleCompletion),
            financeTransactions = listOf(sampleTransaction),
            dailyReviews = listOf(sampleReview)
        )
    }

    @Test
    fun testSerializationAndDeserializationIntegrity() {
        val payload = createTestPayload()
        val json = BackupSerializer.encodePayloadToJson(payload)
        val decodedResult = BackupSerializer.decodeAndValidatePayload(json)

        assertTrue(decodedResult.isSuccess)
        val decoded = decodedResult.getOrThrow()

        assertEquals(payload.manifest.backupId, decoded.manifest.backupId)
        assertEquals(payload.manifest.recordCounts, decoded.manifest.recordCounts)
        assertEquals(1, decoded.tasks.size)
        assertEquals("Study System Design", decoded.tasks.first().title)
        assertEquals(1, decoded.focusSessions.size)
        assertEquals(1, decoded.focusPauseIntervals.size)
        assertEquals(1, decoded.habits.size)
        assertEquals(1, decoded.habitSchedulePeriods.size)
        assertEquals(1, decoded.habitCompletions.size)
        assertEquals(1, decoded.financeTransactions.size)
        assertEquals(24900L, decoded.financeTransactions.first().amountMinor)
        assertEquals(1, decoded.dailyReviews.size)
        assertEquals("COMPLETED", decoded.dailyReviews.first().status)
    }

    @Test
    fun testEncryptionAndDecryptionRoundtrip() {
        val payload = createTestPayload()
        val payloadJson = BackupSerializer.encodePayloadToJson(payload)
        val passphrase = "CorrectHorseBatteryStaple123"

        val encryptResult = BackupCrypto.encryptPayload(
            plaintextJson = payloadJson,
            passphrase = passphrase,
            manifest = payload.manifest,
            kdfIterations = 5000 // Fast iterations for unit test
        )
        assertTrue(encryptResult.isSuccess)
        val envelope = encryptResult.getOrThrow()

        val envelopeJson = BackupSerializer.encodeEnvelopeToJson(envelope)
        val parsedEnvelope = BackupSerializer.decodeEnvelopeFromJson(envelopeJson).getOrThrow()

        // 1. Decrypt with correct passphrase
        val decryptResult = BackupCrypto.decryptPayload(parsedEnvelope, passphrase)
        assertTrue(decryptResult.isSuccess)
        val restoredPayload = BackupSerializer.decodeAndValidatePayload(decryptResult.getOrThrow()).getOrThrow()
        assertEquals(payload.manifest.recordCounts, restoredPayload.manifest.recordCounts)

        // 2. Decrypt with wrong passphrase must fail with authenticated decryption error
        val wrongResult = BackupCrypto.decryptPayload(parsedEnvelope, "WrongPassword999")
        assertFalse(wrongResult.isSuccess)
    }

    @Test
    fun testTamperedCiphertextFailsIntegrityCheck() {
        val payload = createTestPayload()
        val payloadJson = BackupSerializer.encodePayloadToJson(payload)
        val passphrase = "SafePassphrase888"

        val envelope = BackupCrypto.encryptPayload(
            plaintextJson = payloadJson,
            passphrase = passphrase,
            manifest = payload.manifest,
            kdfIterations = 5000
        ).getOrThrow()

        // Tamper with ciphertext
        val tamperedEnvelope = envelope.copy(
            ciphertextSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
        )
        val result = BackupCrypto.decryptPayload(tamperedEnvelope, passphrase)
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("integrity check failed") == true)
    }

    @Test
    fun testPassphraseValidationRequirements() {
        val shortResult = BackupCrypto.validatePassphrase("short")
        assertFalse(shortResult.isSuccess)

        val validResult = BackupCrypto.validatePassphrase("atLeast8Chars")
        assertTrue(validResult.isSuccess)
        assertEquals("atLeast8Chars", validResult.getOrThrow())
    }
}
