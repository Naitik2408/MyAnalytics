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
import org.json.JSONArray
import org.json.JSONObject

/**
 * Deterministic, versioned JSON serializer/deserializer and structural validator for
 * Life Analytics backup archives (.labackup).
 */
object BackupSerializer {

    fun encodeEnvelopeToJson(envelope: EncryptedBackupEnvelope): String {
        val root = JSONObject().apply {
            put("backupId", envelope.backupId)
            put("formatVersion", envelope.formatVersion)
            put("schemaVersion", envelope.schemaVersion)
            put("appVersion", envelope.appVersion)
            put("createdAtEpochMillis", envelope.createdAtEpochMillis)
            put("deviceLabel", envelope.deviceLabel)
            put("recordCounts", encodeRecordCounts(envelope.recordCounts))
            put("kdfAlgorithm", envelope.kdfAlgorithm)
            put("kdfIterations", envelope.kdfIterations)
            put("saltBase64", envelope.saltBase64)
            put("ivBase64", envelope.ivBase64)
            put("ciphertextSha256", envelope.ciphertextSha256)
            put("plaintextSha256", envelope.plaintextSha256)
            put("ciphertextBase64", envelope.ciphertextBase64)
        }
        return root.toString(2)
    }

    fun decodeEnvelopeFromJson(rawJson: String): Result<EncryptedBackupEnvelope> = runCatching {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            throw IllegalArgumentException("Backup archive file is empty.")
        }
        val root = JSONObject(trimmed)
        EncryptedBackupEnvelope(
            backupId = root.getString("backupId"),
            formatVersion = root.getInt("formatVersion"),
            schemaVersion = root.getInt("schemaVersion"),
            appVersion = root.optString("appVersion", "1.0.0"),
            createdAtEpochMillis = root.getLong("createdAtEpochMillis"),
            deviceLabel = root.optString("deviceLabel", "Android Device"),
            recordCounts = decodeRecordCounts(root.getJSONObject("recordCounts")),
            kdfAlgorithm = root.getString("kdfAlgorithm"),
            kdfIterations = root.getInt("kdfIterations"),
            saltBase64 = root.getString("saltBase64"),
            ivBase64 = root.getString("ivBase64"),
            ciphertextSha256 = root.getString("ciphertextSha256"),
            plaintextSha256 = root.getString("plaintextSha256"),
            ciphertextBase64 = root.getString("ciphertextBase64")
        )
    }

    fun encodeRecordCounts(counts: BackupRecordCounts): JSONObject {
        return JSONObject().apply {
            put("tasks", counts.tasks)
            put("focusSessions", counts.focusSessions)
            put("focusPauseIntervals", counts.focusPauseIntervals)
            put("habits", counts.habits)
            put("habitSchedulePeriods", counts.habitSchedulePeriods)
            put("habitCompletions", counts.habitCompletions)
            put("financeTransactions", counts.financeTransactions)
            put("dailyReviews", counts.dailyReviews)
        }
    }

    fun decodeRecordCounts(obj: JSONObject): BackupRecordCounts {
        return BackupRecordCounts(
            tasks = obj.optInt("tasks", 0),
            focusSessions = obj.optInt("focusSessions", 0),
            focusPauseIntervals = obj.optInt("focusPauseIntervals", 0),
            habits = obj.optInt("habits", 0),
            habitSchedulePeriods = obj.optInt("habitSchedulePeriods", 0),
            habitCompletions = obj.optInt("habitCompletions", 0),
            financeTransactions = obj.optInt("financeTransactions", 0),
            dailyReviews = obj.optInt("dailyReviews", 0)
        )
    }

    fun encodePayloadToJson(payload: BackupPayload): String {
        val manifestObj = JSONObject().apply {
            put("backupId", payload.manifest.backupId)
            put("formatVersion", payload.manifest.formatVersion)
            put("schemaVersion", payload.manifest.schemaVersion)
            put("appVersion", payload.manifest.appVersion)
            put("createdAtEpochMillis", payload.manifest.createdAtEpochMillis)
            put("deviceLabel", payload.manifest.deviceLabel)
            put("recordCounts", encodeRecordCounts(payload.manifest.recordCounts))
            put(
                "preferences",
                JSONObject().apply {
                    put("themeOption", payload.manifest.preferences.themeOption.name)
                    put("currencyCode", payload.manifest.preferences.currencyCode)
                    putOptNullableLong("monthlyBudgetMinor", payload.manifest.preferences.monthlyBudgetMinor)
                }
            )
        }

        val tasksArr = JSONArray().apply {
            payload.tasks.forEach { t ->
                put(
                    JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("category", t.category)
                        put("plannedDate", t.plannedDate)
                        putOptNullable("plannedTime", t.plannedTime)
                        putOptNullableInt("estimatedDurationMinutes", t.estimatedDurationMinutes)
                        put("status", t.status)
                        putOptNullableLong("completedAtEpochMillis", t.completedAtEpochMillis)
                        put("sortOrder", t.sortOrder)
                        put("createdAtEpochMillis", t.createdAtEpochMillis)
                        put("updatedAtEpochMillis", t.updatedAtEpochMillis)
                        putOptNullable("lifeArea", t.lifeArea)
                    }
                )
            }
        }

        val focusSessionsArr = JSONArray().apply {
            payload.focusSessions.forEach { s ->
                put(
                    JSONObject().apply {
                        put("id", s.id)
                        putOptNullable("taskId", s.taskId)
                        putOptNullable("taskTitleSnapshot", s.taskTitleSnapshot)
                        put("sessionType", s.sessionType)
                        putOptNullable("description", s.description)
                        put("startedAtEpochMillis", s.startedAtEpochMillis)
                        put("startedLocalDate", s.startedLocalDate)
                        put("timezoneId", s.timezoneId)
                        putOptNullableLong("endedAtEpochMillis", s.endedAtEpochMillis)
                        put("status", s.status)
                        put("focusedDurationSeconds", s.focusedDurationSeconds)
                        put("lastConfirmedAtEpochMillis", s.lastConfirmedAtEpochMillis)
                        putOptNullable("interruptionReason", s.interruptionReason)
                        put("createdAtEpochMillis", s.createdAtEpochMillis)
                        put("updatedAtEpochMillis", s.updatedAtEpochMillis)
                    }
                )
            }
        }

        val pauseIntervalsArr = JSONArray().apply {
            payload.focusPauseIntervals.forEach { p ->
                put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("sessionId", p.sessionId)
                        put("pausedAtEpochMillis", p.pausedAtEpochMillis)
                        putOptNullableLong("resumedAtEpochMillis", p.resumedAtEpochMillis)
                    }
                )
            }
        }

        val habitsArr = JSONArray().apply {
            payload.habits.forEach { h ->
                put(
                    JSONObject().apply {
                        put("id", h.id)
                        put("name", h.name)
                        put("isActive", h.isActive)
                        put("createdAtEpochMillis", h.createdAtEpochMillis)
                        put("updatedAtEpochMillis", h.updatedAtEpochMillis)
                        putOptNullableLong("deactivatedAtEpochMillis", h.deactivatedAtEpochMillis)
                        putOptNullable("deactivatedFromLocalDate", h.deactivatedFromLocalDate)
                        putOptNullable("lifeArea", h.lifeArea)
                    }
                )
            }
        }

        val schedulePeriodsArr = JSONArray().apply {
            payload.habitSchedulePeriods.forEach { sp ->
                put(
                    JSONObject().apply {
                        put("id", sp.id)
                        put("habitId", sp.habitId)
                        put("effectiveFrom", sp.effectiveFrom)
                        putOptNullable("effectiveTo", sp.effectiveTo)
                        put("scheduleType", sp.scheduleType)
                        put("weekdaysCsv", sp.weekdaysCsv)
                        put("createdAtEpochMillis", sp.createdAtEpochMillis)
                    }
                )
            }
        }

        val habitCompletionsArr = JSONArray().apply {
            payload.habitCompletions.forEach { hc ->
                put(
                    JSONObject().apply {
                        put("id", hc.id)
                        put("habitId", hc.habitId)
                        put("completionDate", hc.completionDate)
                        put("isCompleted", hc.isCompleted)
                        putOptNullableLong("completedAtEpochMillis", hc.completedAtEpochMillis)
                        put("updatedAtEpochMillis", hc.updatedAtEpochMillis)
                    }
                )
            }
        }

        val transactionsArr = JSONArray().apply {
            payload.financeTransactions.forEach { tx ->
                put(
                    JSONObject().apply {
                        put("id", tx.id)
                        put("type", tx.type)
                        put("amountMinor", tx.amountMinor)
                        put("currencyCode", tx.currencyCode)
                        put("category", tx.category)
                        put("transactionDate", tx.transactionDate)
                        putOptNullable("note", tx.note)
                        put("createdAtEpochMillis", tx.createdAtEpochMillis)
                        put("updatedAtEpochMillis", tx.updatedAtEpochMillis)
                    }
                )
            }
        }

        val reviewsArr = JSONArray().apply {
            payload.dailyReviews.forEach { r ->
                put(
                    JSONObject().apply {
                        put("id", r.id)
                        put("reviewDate", r.reviewDate)
                        putOptNullable("wentWell", r.wentWell)
                        putOptNullable("improveTomorrow", r.improveTomorrow)
                        putOptNullable("remember", r.remember)
                        put("status", r.status)
                        put("createdAtEpochMillis", r.createdAtEpochMillis)
                        put("updatedAtEpochMillis", r.updatedAtEpochMillis)
                    }
                )
            }
        }

        return JSONObject().apply {
            put("manifest", manifestObj)
            put("tasks", tasksArr)
            put("focusSessions", focusSessionsArr)
            put("focusPauseIntervals", pauseIntervalsArr)
            put("habits", habitsArr)
            put("habitSchedulePeriods", schedulePeriodsArr)
            put("habitCompletions", habitCompletionsArr)
            put("financeTransactions", transactionsArr)
            put("dailyReviews", reviewsArr)
        }.toString()
    }

    fun decodeAndValidatePayload(rawJson: String): Result<BackupPayload> = runCatching {
        val root = JSONObject(rawJson)
        val manifestObj = root.getJSONObject("manifest")
        val prefsObj = manifestObj.optJSONObject("preferences")
        val themeRaw = prefsObj?.optString("themeOption")
        val theme = AppThemeOption.entries.firstOrNull { it.name == themeRaw } ?: AppThemeOption.LIGHT
        val currency = SupportedCurrency.fromCode(prefsObj?.optString("currencyCode")).code
        val monthlyBudget = prefsObj?.optNullableLong("monthlyBudgetMinor")?.takeIf { it > 0L }

        val manifest = BackupManifest(
            backupId = manifestObj.getString("backupId"),
            formatVersion = manifestObj.getInt("formatVersion"),
            schemaVersion = manifestObj.getInt("schemaVersion"),
            appVersion = manifestObj.optString("appVersion", "1.0.0"),
            createdAtEpochMillis = manifestObj.getLong("createdAtEpochMillis"),
            deviceLabel = manifestObj.optString("deviceLabel", "Android Device"),
            recordCounts = decodeRecordCounts(manifestObj.getJSONObject("recordCounts")),
            preferences = AppPreferences(
                themeOption = theme,
                currencyCode = currency,
                monthlyBudgetMinor = monthlyBudget
            )
        )

        val tasksArr = root.getJSONArray("tasks")
        val tasks = (0 until tasksArr.length()).map { i ->
            val o = tasksArr.getJSONObject(i)
            TaskEntity(
                id = o.getString("id"),
                title = o.getString("title"),
                category = o.getString("category"),
                plannedDate = o.getString("plannedDate"),
                plannedTime = o.optNullableString("plannedTime"),
                estimatedDurationMinutes = o.optNullableInt("estimatedDurationMinutes"),
                status = o.getString("status"),
                completedAtEpochMillis = o.optNullableLong("completedAtEpochMillis"),
                sortOrder = o.optInt("sortOrder", 0),
                createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                updatedAtEpochMillis = o.getLong("updatedAtEpochMillis"),
                lifeArea = o.optNullableString("lifeArea")
            )
        }

        val sessionsArr = root.getJSONArray("focusSessions")
        val focusSessions = (0 until sessionsArr.length()).map { i ->
            val o = sessionsArr.getJSONObject(i)
            FocusSessionEntity(
                id = o.getString("id"),
                taskId = o.optNullableString("taskId"),
                taskTitleSnapshot = o.optNullableString("taskTitleSnapshot"),
                sessionType = o.getString("sessionType"),
                description = o.optNullableString("description"),
                startedAtEpochMillis = o.getLong("startedAtEpochMillis"),
                startedLocalDate = o.getString("startedLocalDate"),
                timezoneId = o.optString("timezoneId", "UTC"),
                endedAtEpochMillis = o.optNullableLong("endedAtEpochMillis"),
                status = o.getString("status"),
                focusedDurationSeconds = o.getLong("focusedDurationSeconds"),
                lastConfirmedAtEpochMillis = o.getLong("lastConfirmedAtEpochMillis"),
                interruptionReason = o.optNullableString("interruptionReason"),
                createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                updatedAtEpochMillis = o.getLong("updatedAtEpochMillis")
            )
        }

        val pausesArr = root.getJSONArray("focusPauseIntervals")
        val focusPauseIntervals = (0 until pausesArr.length()).map { i ->
            val o = pausesArr.getJSONObject(i)
            FocusPauseIntervalEntity(
                id = o.getString("id"),
                sessionId = o.getString("sessionId"),
                pausedAtEpochMillis = o.getLong("pausedAtEpochMillis"),
                resumedAtEpochMillis = o.optNullableLong("resumedAtEpochMillis")
            )
        }

        val habitsArr = root.getJSONArray("habits")
        val habits = (0 until habitsArr.length()).map { i ->
            val o = habitsArr.getJSONObject(i)
            HabitEntity(
                id = o.getString("id"),
                name = o.getString("name"),
                isActive = o.getBoolean("isActive"),
                createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                updatedAtEpochMillis = o.getLong("updatedAtEpochMillis"),
                deactivatedAtEpochMillis = o.optNullableLong("deactivatedAtEpochMillis"),
                deactivatedFromLocalDate = o.optNullableString("deactivatedFromLocalDate"),
                lifeArea = o.optNullableString("lifeArea")
            )
        }

        val schedulesArr = root.getJSONArray("habitSchedulePeriods")
        val habitSchedulePeriods = (0 until schedulesArr.length()).map { i ->
            val o = schedulesArr.getJSONObject(i)
            HabitSchedulePeriodEntity(
                id = o.getString("id"),
                habitId = o.getString("habitId"),
                effectiveFrom = o.getString("effectiveFrom"),
                effectiveTo = o.optNullableString("effectiveTo"),
                scheduleType = o.getString("scheduleType"),
                weekdaysCsv = o.getString("weekdaysCsv"),
                createdAtEpochMillis = o.getLong("createdAtEpochMillis")
            )
        }

        val completionsArr = root.getJSONArray("habitCompletions")
        val habitCompletions = (0 until completionsArr.length()).map { i ->
            val o = completionsArr.getJSONObject(i)
            HabitCompletionEntity(
                id = o.getString("id"),
                habitId = o.getString("habitId"),
                completionDate = o.getString("completionDate"),
                isCompleted = o.getBoolean("isCompleted"),
                completedAtEpochMillis = o.optNullableLong("completedAtEpochMillis"),
                updatedAtEpochMillis = o.getLong("updatedAtEpochMillis")
            )
        }

        val transactionsArr = root.getJSONArray("financeTransactions")
        val financeTransactions = (0 until transactionsArr.length()).map { i ->
            val o = transactionsArr.getJSONObject(i)
            FinanceTransactionEntity(
                id = o.getString("id"),
                type = o.getString("type"),
                amountMinor = o.getLong("amountMinor"),
                currencyCode = o.getString("currencyCode"),
                category = o.getString("category"),
                transactionDate = o.getString("transactionDate"),
                note = o.optNullableString("note"),
                createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                updatedAtEpochMillis = o.getLong("updatedAtEpochMillis")
            )
        }

        val reviewsArr = root.getJSONArray("dailyReviews")
        val dailyReviews = (0 until reviewsArr.length()).map { i ->
            val o = reviewsArr.getJSONObject(i)
            DailyReviewEntity(
                id = o.getString("id"),
                reviewDate = o.getString("reviewDate"),
                wentWell = o.optNullableString("wentWell"),
                improveTomorrow = o.optNullableString("improveTomorrow"),
                remember = o.optNullableString("remember"),
                status = o.getString("status"),
                createdAtEpochMillis = o.getLong("createdAtEpochMillis"),
                updatedAtEpochMillis = o.getLong("updatedAtEpochMillis")
            )
        }

        val payload = BackupPayload(
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
        validatePayloadIntegrity(payload)
        payload
    }

    fun validatePayloadIntegrity(payload: BackupPayload) {
        val counts = payload.manifest.recordCounts
        require(payload.tasks.size == counts.tasks) {
            "Task count mismatch: manifest=${counts.tasks}, payload=${payload.tasks.size}"
        }
        require(payload.focusSessions.size == counts.focusSessions) {
            "Focus session count mismatch: manifest=${counts.focusSessions}, payload=${payload.focusSessions.size}"
        }
        require(payload.focusPauseIntervals.size == counts.focusPauseIntervals) {
            "Pause interval count mismatch: manifest=${counts.focusPauseIntervals}, payload=${payload.focusPauseIntervals.size}"
        }
        require(payload.habits.size == counts.habits) {
            "Habit count mismatch: manifest=${counts.habits}, payload=${payload.habits.size}"
        }
        require(payload.habitSchedulePeriods.size == counts.habitSchedulePeriods) {
            "Habit schedule count mismatch: manifest=${counts.habitSchedulePeriods}, payload=${payload.habitSchedulePeriods.size}"
        }
        require(payload.habitCompletions.size == counts.habitCompletions) {
            "Habit completion count mismatch: manifest=${counts.habitCompletions}, payload=${payload.habitCompletions.size}"
        }
        require(payload.financeTransactions.size == counts.financeTransactions) {
            "Transaction count mismatch: manifest=${counts.financeTransactions}, payload=${payload.financeTransactions.size}"
        }
        require(payload.dailyReviews.size == counts.dailyReviews) {
            "Daily review count mismatch: manifest=${counts.dailyReviews}, payload=${payload.dailyReviews.size}"
        }

        val sessionIds = payload.focusSessions.map { it.id }.toSet()
        for (pause in payload.focusPauseIntervals) {
            require(pause.sessionId in sessionIds) {
                "Focus pause interval ${pause.id} references missing session ${pause.sessionId}."
            }
        }

        val habitIds = payload.habits.map { it.id }.toSet()
        for (period in payload.habitSchedulePeriods) {
            require(period.habitId in habitIds) {
                "Habit schedule period ${period.id} references missing habit ${period.habitId}."
            }
        }
        for (completion in payload.habitCompletions) {
            require(completion.habitId in habitIds) {
                "Habit completion ${completion.id} references missing habit ${completion.habitId}."
            }
        }
    }

    private fun JSONObject.putOptNullable(key: String, value: String?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.putOptNullableInt(key: String, value: Int?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.putOptNullableLong(key: String, value: Long?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (isNull(key) || !has(key)) return null
        return optString(key).takeIf { it != "null" }
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (isNull(key) || !has(key)) return null
        return optInt(key)
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (isNull(key) || !has(key)) return null
        return optLong(key)
    }
}
