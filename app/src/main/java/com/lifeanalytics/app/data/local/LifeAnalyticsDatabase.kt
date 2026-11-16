package com.lifeanalytics.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lifeanalytics.app.data.local.dao.DailyReviewDao
import com.lifeanalytics.app.data.local.dao.FinanceDao
import com.lifeanalytics.app.data.local.dao.FocusDao
import com.lifeanalytics.app.data.local.dao.HabitDao
import com.lifeanalytics.app.data.local.dao.TaskDao
import com.lifeanalytics.app.data.local.entity.DailyReviewEntity
import com.lifeanalytics.app.data.local.entity.FinanceTransactionEntity
import com.lifeanalytics.app.data.local.entity.FocusPauseIntervalEntity
import com.lifeanalytics.app.data.local.entity.FocusSessionEntity
import com.lifeanalytics.app.data.local.entity.HabitCompletionEntity
import com.lifeanalytics.app.data.local.entity.HabitEntity
import com.lifeanalytics.app.data.local.entity.HabitSchedulePeriodEntity
import com.lifeanalytics.app.data.local.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        FocusSessionEntity::class,
        FocusPauseIntervalEntity::class,
        HabitEntity::class,
        HabitSchedulePeriodEntity::class,
        HabitCompletionEntity::class,
        FinanceTransactionEntity::class,
        DailyReviewEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LifeAnalyticsDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusDao(): FocusDao
    abstract fun habitDao(): HabitDao
    abstract fun financeDao(): FinanceDao
    abstract fun dailyReviewDao(): DailyReviewDao

    companion object {
        private const val DATABASE_NAME = "life_analytics.db"

        @Volatile
        private var INSTANCE: LifeAnalyticsDatabase? = null

        fun getInstance(context: Context): LifeAnalyticsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeAnalyticsDatabase::class.java,
                    DATABASE_NAME
                )
                    // Never use fallbackToDestructiveMigration in production (docs/11_DATA_ARCHITECTURE.md Section 16)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
