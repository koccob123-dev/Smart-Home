package com.example.automation.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM device_schedules ORDER BY startHour ASC, startMinute ASC")
    fun getAllSchedules(): Flow<List<DeviceScheduleEntity>>

    @Query("SELECT * FROM device_schedules WHERE isEnabled = 1")
    suspend fun getActiveSchedules(): List<DeviceScheduleEntity>

    @Query("SELECT * FROM device_schedules WHERE id = :id LIMIT 1")
    suspend fun getScheduleById(id: Long): DeviceScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: DeviceScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: DeviceScheduleEntity)

    @Query("UPDATE device_schedules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setScheduleEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE device_schedules SET nextRunEpochMs = :nextRun, lastRunEpochMs = :lastRun WHERE id = :id")
    suspend fun updateRunTimestamps(id: Long, nextRun: Long, lastRun: Long)

    @Delete
    suspend fun deleteSchedule(schedule: DeviceScheduleEntity)

    @Query("DELETE FROM device_schedules WHERE id = :id")
    suspend fun deleteScheduleById(id: Long)
}
