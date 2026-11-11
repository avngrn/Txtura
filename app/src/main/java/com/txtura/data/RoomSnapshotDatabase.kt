package com.txtura.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File
import com.txtura.security.SecurityCryptoManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Entity(tableName = "repository_snapshot")
class RepositorySnapshotEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val payload: ByteArray,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(tableName = "thread_summary_snapshot")
class ThreadSummarySnapshotEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val payload: ByteArray,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(tableName = "conversation_history")
class ConversationHistoryEntity(
    @PrimaryKey val threadId: Long,
    val payload: ByteArray,
    val messageCount: Int,
    val isComplete: Boolean,
)

@Dao
interface RepositorySnapshotDao {
    @Query("SELECT * FROM repository_snapshot WHERE id = :id LIMIT 1")
    fun getSnapshot(id: Int = RepositorySnapshotEntity.SINGLETON_ID): RepositorySnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveSnapshot(snapshot: RepositorySnapshotEntity)

    @Query("DELETE FROM repository_snapshot WHERE id = :id")
    fun deleteSnapshot(id: Int = RepositorySnapshotEntity.SINGLETON_ID)

    @Query("SELECT * FROM thread_summary_snapshot WHERE id = :id LIMIT 1")
    fun getThreadSummarySnapshot(id: Int = ThreadSummarySnapshotEntity.SINGLETON_ID): ThreadSummarySnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveThreadSummarySnapshot(snapshot: ThreadSummarySnapshotEntity)

    @Query("SELECT * FROM conversation_history WHERE threadId = :threadId LIMIT 1")
    fun getConversationHistory(threadId: Long): ConversationHistoryEntity?

    @Query("SELECT threadId FROM conversation_history")
    fun getConversationHistoryIds(): List<Long>

    @Query("SELECT * FROM conversation_history")
    fun getAllConversationHistories(): List<ConversationHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveConversationHistories(histories: List<ConversationHistoryEntity>)

    @Query("DELETE FROM conversation_history WHERE threadId = :threadId")
    fun deleteConversationHistory(threadId: Long)

}

@Database(
    entities = [
        RepositorySnapshotEntity::class,
        ThreadSummarySnapshotEntity::class,
        ConversationHistoryEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class TxturaSecureDatabase : RoomDatabase() {
    abstract fun repositorySnapshotDao(): RepositorySnapshotDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `thread_summary_snapshot` " +
                        "(`id` INTEGER NOT NULL, `payload` BLOB NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `conversation_history` " +
                        "(`threadId` INTEGER NOT NULL, `payload` BLOB NOT NULL, " +
                        "`messageCount` INTEGER NOT NULL, `isComplete` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`threadId`))"
                )
            }
        }
        private const val DATABASE_NAME = "txtura_secure.db"

        @Volatile
        private var instance: TxturaSecureDatabase? = null

        fun get(context: Context): TxturaSecureDatabase {
            instance?.let { return it }
            return synchronized(this) {
                instance?.let { return it }

                val appContext = context.applicationContext
                System.loadLibrary("sqlcipher")
                val databaseFile = appContext.getDatabasePath(DATABASE_NAME)
                val databaseArtifacts = listOf(
                    databaseFile,
                    File("${databaseFile.path}-wal"),
                    File("${databaseFile.path}-shm"),
                    File("${databaseFile.path}-journal"),
                )
                val passphrase = SecurityCryptoManager.getOrCreateSqlCipherPassphrase(
                    context = appContext,
                    databaseFileExists = databaseArtifacts.any(File::exists),
                )
                val openHelperFactory = SupportOpenHelperFactory(passphrase)

                Room.databaseBuilder(
                    appContext,
                    TxturaSecureDatabase::class.java,
                    DATABASE_NAME,
                )
                    .openHelperFactory(openHelperFactory)
                    .addMigrations(MIGRATION_1_2)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
