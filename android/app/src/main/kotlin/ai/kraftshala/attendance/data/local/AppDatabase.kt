package ai.kraftshala.attendance.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Entity(tableName = "queued_marks")
data class QueuedMark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val userId: String,
    val timestamp: String,
    val nonce: String,
    val signatureB64: String,
    val signalStrengthsCsv: String,    // comma-separated RSSIs
    val syncedAt: String? = null
)

@Entity(tableName = "queued_ble_events")
data class QueuedBleEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val userId: String,
    val signalStrength: Int,
    val detectedAt: String,
    val eventType: String,             // 'detected' | 'lost'
    val syncedAt: String? = null
)

@Dao
interface QueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMark(m: QueuedMark): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBle(e: QueuedBleEvent): Long

    @Query("SELECT * FROM queued_marks WHERE syncedAt IS NULL")
    suspend fun pendingMarks(): List<QueuedMark>

    @Query("SELECT * FROM queued_ble_events WHERE syncedAt IS NULL")
    suspend fun pendingBleEvents(): List<QueuedBleEvent>

    @Query("UPDATE queued_marks SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markMarkSynced(id: Long, syncedAt: String)

    @Query("UPDATE queued_ble_events SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markBleSynced(id: Long, syncedAt: String)
}

@Database(entities = [QueuedMark::class, QueuedBleEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "kraftshala.db")
                    .build().also { INSTANCE = it }
            }
        }
    }
}
