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
        Project::class,
        Casting::class,
        CharacterScript::class,
        ProjectContact::class
    ],
    version = 24,
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
    abstract fun projectDao(): ProjectDao
    abstract fun castingDao(): CastingDao
    abstract fun characterScriptDao(): CharacterScriptDao
    abstract fun projectContactDao(): ProjectContactDao

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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS projects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        castingDirector TEXT NOT NULL DEFAULT '',
                        notes TEXT NOT NULL DEFAULT '',
                        direction TEXT NOT NULL DEFAULT '',
                        auditionDatesJson TEXT NOT NULL DEFAULT '[]',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        // Phase 2: Castings
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS castings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_castings_projectId ON castings(projectId)")
            }
        }

        // Phase 3: Character restructure (add castingId, make scriptId nullable, add notes/timestamps)
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS characters_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        castingId INTEGER,
                        scriptId INTEGER,
                        name TEXT NOT NULL,
                        lineCount INTEGER NOT NULL DEFAULT 0,
                        isUserRole INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(castingId) REFERENCES castings(id) ON DELETE CASCADE,
                        FOREIGN KEY(scriptId) REFERENCES scripts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO characters_new (id, castingId, scriptId, name, lineCount, isUserRole, notes, createdAt, updatedAt)
                    SELECT id, NULL, scriptId, name, lineCount, isUserRole, '', 0, 0 FROM characters
                """.trimIndent())
                db.execSQL("DROP TABLE characters")
                db.execSQL("ALTER TABLE characters_new RENAME TO characters")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_characters_castingId ON characters(castingId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_characters_scriptId ON characters(scriptId)")
            }
        }

        // Phase 4: Character-Script junction table
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS character_scripts (
                        characterId INTEGER NOT NULL,
                        scriptId INTEGER NOT NULL,
                        assignedAt INTEGER NOT NULL,
                        PRIMARY KEY(characterId, scriptId),
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE CASCADE,
                        FOREIGN KEY(scriptId) REFERENCES scripts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_character_scripts_characterId ON character_scripts(characterId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_character_scripts_scriptId ON character_scripts(scriptId)")
            }
        }

        // Phase 5: SelfTape add characterId
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS self_tapes_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        videoPath TEXT NOT NULL,
                        thumbnailPath TEXT NOT NULL DEFAULT '',
                        durationMs INTEGER NOT NULL DEFAULT 0,
                        auditionId INTEGER,
                        scriptId INTEGER,
                        characterId INTEGER,
                        trimStartMs INTEGER NOT NULL DEFAULT 0,
                        trimEndMs INTEGER NOT NULL DEFAULT 0,
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(auditionId) REFERENCES auditions(id) ON DELETE SET NULL,
                        FOREIGN KEY(characterId) REFERENCES characters(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO self_tapes_new (id, title, videoPath, thumbnailPath, durationMs, auditionId, scriptId, characterId, trimStartMs, trimEndMs, notes, createdAt, updatedAt)
                    SELECT id, title, videoPath, thumbnailPath, durationMs, auditionId, scriptId, NULL, trimStartMs, trimEndMs, notes, createdAt, updatedAt FROM self_tapes
                """.trimIndent())
                db.execSQL("DROP TABLE self_tapes")
                db.execSQL("ALTER TABLE self_tapes_new RENAME TO self_tapes")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_self_tapes_auditionId ON self_tapes(auditionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_self_tapes_characterId ON self_tapes(characterId)")
            }
        }

        // Phase 6: Audition add projectId
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS auditions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER,
                        projectName TEXT NOT NULL,
                        roleName TEXT NOT NULL DEFAULT '',
                        castingDirector TEXT NOT NULL DEFAULT '',
                        auditionDate INTEGER,
                        status TEXT NOT NULL DEFAULT 'SUBMITTED',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO auditions_new (id, projectId, projectName, roleName, castingDirector, auditionDate, status, notes, createdAt, updatedAt)
                    SELECT id, NULL, projectName, roleName, castingDirector, auditionDate, status, notes, createdAt, updatedAt FROM auditions
                """.trimIndent())
                db.execSQL("DROP TABLE auditions")
                db.execSQL("ALTER TABLE auditions_new RENAME TO auditions")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_auditions_projectId ON auditions(projectId)")
            }
        }

        // Expand Project fields
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN director TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN startDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE projects ADD COLUMN endDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE projects ADD COLUMN presentationDatesJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE projects ADD COLUMN linksJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE projects ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE projects ADD COLUMN teamJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        // Add locations to projects
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN locationsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        // Add links, attachments, images to auditions
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE auditions ADD COLUMN linksJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE auditions ADD COLUMN attachmentsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE auditions ADD COLUMN imagesJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        // Add project contacts
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS project_contacts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        phone TEXT NOT NULL DEFAULT '',
                        email TEXT NOT NULL DEFAULT '',
                        rolesJson TEXT NOT NULL DEFAULT '[]',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_project_contacts_projectId ON project_contacts(projectId)")
            }
        }

        // Add user notes to script lines
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE script_lines ADD COLUMN userNotes TEXT DEFAULT NULL")
            }
        }

        // Remove self_tapes table
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS self_tapes")
            }
        }

        // Add projectId to scripts
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scripts ADD COLUMN projectId INTEGER DEFAULT NULL")
            }
        }

        // Add sortOrder to projects (backfilled using current updatedAt DESC order)
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                val cursor = db.query("SELECT id FROM projects ORDER BY updatedAt DESC")
                var idx = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    db.execSQL("UPDATE projects SET sortOrder = ? WHERE id = ?", arrayOf<Any>(idx, id))
                    idx++
                }
                cursor.close()
            }
        }

        // Add shareId (stable UUID) to projects for merge-on-import across devices
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN shareId TEXT NOT NULL DEFAULT ''")
                val cursor = db.query("SELECT id FROM projects")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val uuid = java.util.UUID.randomUUID().toString()
                    db.execSQL("UPDATE projects SET shareId = ? WHERE id = ?", arrayOf<Any>(uuid, id))
                }
                cursor.close()
            }
        }

        // Per-character TTS voice mapping per script (JSON blob)
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE scripts ADD COLUMN characterVoicesJson TEXT NOT NULL DEFAULT '{}'")
            }
        }

        // Add card color and image to projects
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN cardColor TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN cardImageUri TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "actors_toolkit.db"
                )
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17,
                        MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20,
                        MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
