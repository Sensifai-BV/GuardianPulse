package com.guardianpulse.prototype.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "event_logs")
data class EventLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val hrValue: Float?,
    val audioLevel: Float?,
    val hrFlag: Boolean = false,
    val audioFlag: Boolean = false,
    val alertLevel: Int = 0,
    val isAcknowledged: Boolean = false
)

@Dao
interface EventLogDao {
    @Insert
    suspend fun insert(log: EventLog)

    @Query("SELECT * FROM event_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<EventLog>>

    @Query("DELETE FROM event_logs WHERE timestamp < :thresholdTime")
    suspend fun deleteOlderThan(thresholdTime: Long)
}

@Database(entities = [EventLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventLogDao(): EventLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "guardian_pulse_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
