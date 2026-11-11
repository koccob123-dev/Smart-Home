package com.example.automation.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "automation_logs")
data class AutomationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val sourceType: String, // "SCHEDULE", "RULE", "TEST"
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true
) {
    fun formattedTime(): String {
        val sdf = SimpleDateFormat("MMM d, hh:mm:ss a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
