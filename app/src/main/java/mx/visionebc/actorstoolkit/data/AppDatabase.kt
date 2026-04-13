package mx.visionebc.actorstoolkit.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import mx.visionebc.actorstoolkit.data.dao.*
import mx.visionebc.actorstoolkit.data.entity.*

@Database(
    entities = [
        Script::class,
        ScriptLine::class,
        Character::class,
        AudioRecording::class,
        BlockingMark::class,
        StageItem::class,
        MovementPath::class,
        Audition::class,
        SelfTape::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scriptDao(): ScriptDao
    abstract fun scriptLineDao(): ScriptLineDao
    abstract fun characterDao(): CharacterDao
    abstract fun audioRecordingDao(): AudioRecordingDao
    abstract fun blockingMarkDao(): BlockingMarkDao
    abstract fun stageItemDao(): StageItemDao
    abstract fun movementPathDao(): MovementPathDao
    abstract fun auditionDao(): AuditionDao
    abstract fun selfTapeDao(): SelfTapeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE script_lines ADD COLUMN isSkipped INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE script_lines ADD COLUMN editedDialogue TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE script_lines ADD COLUMN ignoredWords TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS stage_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scriptId INTEGER NOT NULL,
                        itemType TEXT NOT NULL,
                        label TEXT NOT NULL DEFAULT '',
                        posX REAL NOT NULL,
                        posY REAL NOT NULL,
                        rotation REAL NOT NULL DEFAULT 0.0,
                        scaleX REAL NOT NULL DEFAULT 1.0,
                        scaleY REAL NOT NULL DEFAULT 1.0,
                        FOREIGN KEY(scriptId) REFERENCES scripts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_stage_items_scriptId ON stage_items(scriptId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS movement_paths (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scriptId INTEGER NOT NULL,
                        lineNumber INTEGER NOT NULL,
                        characterName TEXT NOT NULL,
                        pathPoints TEXT NOT NULL,
                        FOREIGN KEY(scriptId) REFERENCES scripts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_movement_paths_scriptId ON movement_paths(scriptId)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS auditions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectName TEXT NOT NULL,
                        roleName TEXT NOT NULL DEFAULT '',
                        castingDirector TEXT NOT NULL DEFAULT '',
                        auditionDate INTEGER,
                        status TEXT NOT NULL DEFAULT 'SUBMITTED',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS self_tapes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        videoPath TEXT NOT NULL,
                        thumbnailPath TEXT NOT NULL DEFAULT '',
                        durationMs INTEGER NOT NULL DEFAULT 0,
                        auditionId INTEGER,
                        scriptId INTEGER,
                        trimStartMs INTEGER NOT NULL DEFAULT 0,
                        trimEndMs INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(auditionId) REFERENCES auditions(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_self_tapes_auditionId ON self_tapes(auditionId)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "actors_toolkit.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
