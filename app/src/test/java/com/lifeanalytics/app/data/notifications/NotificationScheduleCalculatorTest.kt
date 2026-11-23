package com.lifeanalytics.app.data.notifications

import com.lifeanalytics.app.domain.model.Habit
import com.lifeanalytics.app.domain.model.HabitSchedulePeriod
import com.lifeanalytics.app.domain.model.HabitScheduleType
import com.lifeanalytics.app.domain.model.LifeArea
import com.lifeanalytics.app.domain.model.Task
import com.lifeanalytics.app.domain.model.TaskCategory
import com.lifeanalytics.app.domain.model.TaskStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationScheduleCalculatorTest {

    private val istZone = ZoneId.of("Asia/Kolkata")
    private val nyZone = ZoneId.of("America/New_York")

    private fun makeTask(
        id: String = "task-1",
        plannedDate: LocalDate = LocalDate.of(2026, 11, 23),
        plannedTime: LocalTime? = LocalTime.of(14, 0),
        status: TaskStatus = TaskStatus.PLANNED,
        lifeArea: LifeArea? = null
    ) = Task(
        id = id,
        title = "Sample task",
        category = TaskCategory.WORK,
        plannedDate = plannedDate,
        plannedTime = plannedTime,
        estimatedDurationMinutes = 30,
        status = status,
        completedAtEpochMillis = null,
        sortOrder = 0,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
        lifeArea = lifeArea
    )

    private fun makeHabit(
        id: String = "habit-1",
        weekdays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        reminderTime: LocalTime? = LocalTime.of(8, 30),
        isActive: Boolean = true,
        deactivatedFrom: LocalDate? = null
    ) = Habit(
        id = id,
        name = "Morning routine",
        isActive = isActive,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
        deactivatedAtEpochMillis = null,
        deactivatedFromLocalDate = deactivatedFrom,
        schedulePeriods = listOf(
            HabitSchedulePeriod(
                id = "sp-$id",
                habitId = id,
                effectiveFrom = LocalDate.of(2026, 1, 1),
                effectiveTo = deactivatedFrom,
                scheduleType = HabitScheduleType.fromWeekdays(weekdays),
                weekdays = weekdays,
                createdAtEpochMillis = 1L
            )
        ),
        lifeArea = LifeArea.HEALTH_LIFESTYLE,
        reminderTime = reminderTime
    )

    @Test
    fun taskWithFutureDateAndTime_schedulesAtOffsetBeforePlannedTime() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 23), LocalTime.of(10, 0), istZone)
        val task = makeTask(plannedDate = LocalDate.of(2026, 11, 23), plannedTime = LocalTime.of(14, 0))

        val trigger = NotificationScheduleCalculator.computeTaskTriggerTime(task, offsetMinutes = 15, now = now)
        assertNotNull(trigger)
        assertEquals(ZonedDateTime.of(LocalDate.of(2026, 11, 23), LocalTime.of(13, 45), istZone), trigger)
    }

    @Test
    fun taskWithoutTimeOrCompletedOrPast_returnsNull() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 23), LocalTime.of(14, 0), istZone)

        val unscheduled = makeTask(plannedTime = null)
        assertNull(NotificationScheduleCalculator.computeTaskTriggerTime(unscheduled, 15, now))

        val completed = makeTask(plannedTime = LocalTime.of(18, 0), status = TaskStatus.COMPLETED)
        assertNull(NotificationScheduleCalculator.computeTaskTriggerTime(completed, 15, now))

        val past = makeTask(plannedDate = LocalDate.of(2026, 11, 22), plannedTime = LocalTime.of(18, 0))
        assertNull(NotificationScheduleCalculator.computeTaskTriggerTime(past, 15, now))

        // Offset pushes trigger into the past (14:10 - 15m = 13:55 <= 14:00)
        val tooSoon = makeTask(plannedDate = LocalDate.of(2026, 11, 23), plannedTime = LocalTime.of(14, 10))
        assertNull(NotificationScheduleCalculator.computeTaskTriggerTime(tooSoon, 15, now))
    }

    @Test
    fun weekdayHabit_skipsWeekendAndSchedulesNextMonday() {
        // 2026-11-21 is Saturday
        val saturdayMorning = ZonedDateTime.of(LocalDate.of(2026, 11, 21), LocalTime.of(7, 0), istZone)
        val weekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        val habit = makeHabit(weekdays = weekdays, reminderTime = LocalTime.of(8, 30))

        val trigger = NotificationScheduleCalculator.computeNextHabitTriggerTime(habit, emptySet(), saturdayMorning)
        assertNotNull(trigger)
        assertEquals(LocalDate.of(2026, 11, 23), trigger!!.toLocalDate()) // Monday
        assertEquals(LocalTime.of(8, 30), trigger.toLocalTime())
    }

    @Test
    fun habitCompletedToday_skipsTodayAndSchedulesTomorrow() {
        val mondayEarly = ZonedDateTime.of(LocalDate.of(2026, 11, 23), LocalTime.of(7, 0), istZone)
        val habit = makeHabit(reminderTime = LocalTime.of(8, 30))

        val trigger = NotificationScheduleCalculator.computeNextHabitTriggerTime(
            habit = habit,
            completedDates = setOf(LocalDate.of(2026, 11, 23)),
            now = mondayEarly
        )
        assertNotNull(trigger)
        assertEquals(LocalDate.of(2026, 11, 24), trigger!!.toLocalDate())
    }

    @Test
    fun inactiveOrNoReminderHabit_returnsNull() {
        val now = ZonedDateTime.of(LocalDate.of(2026, 11, 23), LocalTime.of(7, 0), istZone)

        val inactive = makeHabit(isActive = false)
        assertNull(NotificationScheduleCalculator.computeNextHabitTriggerTime(inactive, emptySet(), now))

        val noReminder = makeHabit(reminderTime = null)
        assertNull(NotificationScheduleCalculator.computeNextHabitTriggerTime(noReminder, emptySet(), now))
    }

    @Test
    fun timezoneChange_recalculatesInstantForNewZone() {
        val task = makeTask(plannedDate = LocalDate.of(2026, 11, 23), plannedTime = LocalTime.of(15, 0))
        val nowIst = ZonedDateTime.of(LocalDate.of(2026, 11, 23), LocalTime.of(10, 0), istZone)
        val nowNy = ZonedDateTime.of(LocalDate.of(2026, 11, 23), LocalTime.of(10, 0), nyZone)

        val triggerIst = NotificationScheduleCalculator.computeTaskTriggerTime(task, 0, nowIst)!!
        val triggerNy = NotificationScheduleCalculator.computeTaskTriggerTime(task, 0, nowNy)!!

        assertEquals(istZone, triggerIst.zone)
        assertEquals(nyZone, triggerNy.zone)
        assertNotEquals(triggerIst.toInstant(), triggerNy.toInstant())
    }

    @Test
    fun requestCodes_areStableAndDisjointBetweenTasksAndHabits() {
        val taskCode1 = NotificationScheduleCalculator.taskRequestCode("same-id")
        val taskCode2 = NotificationScheduleCalculator.taskRequestCode("same-id")
        val habitCode = NotificationScheduleCalculator.habitRequestCode("same-id")

        assertEquals(taskCode1, taskCode2)
        assertTrue(taskCode1 > 0)
        assertTrue(habitCode > 0)
        assertNotEquals(taskCode1, habitCode)
    }
}
