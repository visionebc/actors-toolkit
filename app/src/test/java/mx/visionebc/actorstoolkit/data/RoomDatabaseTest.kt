package mx.visionebc.actorstoolkit.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mx.visionebc.actorstoolkit.data.dao.*
import mx.visionebc.actorstoolkit.data.entity.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var scriptDao: ScriptDao
    private lateinit var scriptLineDao: ScriptLineDao
    private lateinit var characterDao: CharacterDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scriptDao = db.scriptDao()
        scriptLineDao = db.scriptLineDao()
        characterDao = db.characterDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun insertScript_andQueryById_returnsCorrectScript() = runTest {
        val script = Script(
            title = "Hamlet",
            fileName = "hamlet.pdf",
            fileType = "pdf",
            rawContent = "To be or not to be..."
        )
        val id = scriptDao.insert(script)

        val loaded = scriptDao.getScriptById(id)
        assertNotNull("Script should be found", loaded)
        assertEquals("Hamlet", loaded!!.title)
        assertEquals("hamlet.pdf", loaded.fileName)
        assertEquals("pdf", loaded.fileType)
        assertEquals("To be or not to be...", loaded.rawContent)
    }

    @Test
    fun insertScript_getAllScripts_returnsScriptsOrderedByUpdatedAt() = runTest {
        val script1 = Script(
            title = "Script A",
            fileName = "a.txt",
            fileType = "txt",
            rawContent = "content A",
            updatedAt = 1000L
        )
        val script2 = Script(
            title = "Script B",
            fileName = "b.txt",
            fileType = "txt",
            rawContent = "content B",
            updatedAt = 2000L
        )
        scriptDao.insert(script1)
        scriptDao.insert(script2)

        val scripts = scriptDao.getAllScripts().first()
        assertEquals(2, scripts.size)
        assertEquals("Script B", scripts[0].title)
        assertEquals("Script A", scripts[1].title)
    }

    @Test
    fun queryNonExistentScript_returnsNull() = runTest {
        val result = scriptDao.getScriptById(999L)
        assertNull("Non-existent script should return null", result)
    }

    @Test
    fun updatePracticeStats_incrementsCountAndSetsTime() = runTest {
        val id = scriptDao.insert(Script(
            title = "Test",
            fileName = "test.txt",
            fileType = "txt",
            rawContent = "content",
            practiceCount = 0
        ))

        scriptDao.updatePracticeStats(id, System.currentTimeMillis())

        val updated = scriptDao.getScriptById(id)
        assertNotNull(updated)
        assertEquals("Practice count should be 1", 1, updated!!.practiceCount)
        assertNotNull("lastPracticedAt should be set", updated.lastPracticedAt)
    }

    @Test
    fun updatePracticeStats_multipleIncrements() = runTest {
        val id = scriptDao.insert(Script(
            title = "Test",
            fileName = "test.txt",
            fileType = "txt",
            rawContent = "content",
            practiceCount = 0
        ))

        scriptDao.updatePracticeStats(id, System.currentTimeMillis())
        scriptDao.updatePracticeStats(id, System.currentTimeMillis())
        scriptDao.updatePracticeStats(id, System.currentTimeMillis())

        val updated = scriptDao.getScriptById(id)
        assertEquals("Practice count should be 3", 3, updated!!.practiceCount)
    }

    @Test
    fun deleteScript_removesScript() = runTest {
        val script = Script(
            title = "Delete Me",
            fileName = "delete.txt",
            fileType = "txt",
            rawContent = "content"
        )
        val id = scriptDao.insert(script)
        val loaded = scriptDao.getScriptById(id)
        assertNotNull(loaded)

        scriptDao.delete(loaded!!)

        val afterDelete = scriptDao.getScriptById(id)
        assertNull("Script should be deleted", afterDelete)
    }

    @Test
    fun getScriptInfoById_returnsLightweightData() = runTest {
        val id = scriptDao.insert(Script(
            title = "Info Test",
            fileName = "info.txt",
            fileType = "txt",
            rawContent = "long content here"
        ))

        val info = scriptDao.getScriptInfoById(id)
        assertNotNull(info)
        assertEquals("Info Test", info!!.title)
        assertEquals("info.txt", info.fileName)
    }

    @Test
    fun getAllScriptInfos_returnsOrderedList() = runTest {
        scriptDao.insert(Script(title = "A", fileName = "a.txt", fileType = "txt", rawContent = "a", updatedAt = 1000L))
        scriptDao.insert(Script(title = "B", fileName = "b.txt", fileType = "txt", rawContent = "b", updatedAt = 2000L))

        val infos = scriptDao.getAllScriptInfos().first()
        assertEquals(2, infos.size)
        assertEquals("B", infos[0].title)
        assertEquals("A", infos[1].title)
    }

    @Test
    fun deleteById_removesScript() = runTest {
        val id = scriptDao.insert(Script(title = "X", fileName = "x.txt", fileType = "txt", rawContent = "x"))
        assertNotNull(scriptDao.getScriptById(id))

        scriptDao.deleteById(id)

        assertNull(scriptDao.getScriptById(id))
    }

    @Test
    fun insertLines_andQueryByScriptId_returnsLinesInOrder() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Test", fileName = "test.txt", fileType = "txt", rawContent = "content"))

        val lines = listOf(
            ScriptLine(scriptId = scriptId, lineNumber = 1, character = "HAMLET", dialogue = "Line 1"),
            ScriptLine(scriptId = scriptId, lineNumber = 2, character = "OPHELIA", dialogue = "Line 2"),
            ScriptLine(scriptId = scriptId, lineNumber = 3, character = "HAMLET", dialogue = "Line 3")
        )
        scriptLineDao.insertAll(lines)

        val loaded = scriptLineDao.getLinesForScriptSync(scriptId)
        assertEquals(3, loaded.size)
        assertEquals(1, loaded[0].lineNumber)
        assertEquals(2, loaded[1].lineNumber)
        assertEquals(3, loaded[2].lineNumber)
    }

    @Test
    fun getLinesForCharacter_filtersCorrectly() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Test", fileName = "test.txt", fileType = "txt", rawContent = "content"))

        scriptLineDao.insertAll(listOf(
            ScriptLine(scriptId = scriptId, lineNumber = 1, character = "HAMLET", dialogue = "H1"),
            ScriptLine(scriptId = scriptId, lineNumber = 2, character = "OPHELIA", dialogue = "O1"),
            ScriptLine(scriptId = scriptId, lineNumber = 3, character = "HAMLET", dialogue = "H2")
        ))

        val hamletLines = scriptLineDao.getLinesForCharacter(scriptId, "HAMLET").first()
        assertEquals("Should only get Hamlet's lines", 2, hamletLines.size)
        assertTrue(hamletLines.all { it.character == "HAMLET" })
    }

    @Test
    fun setMemorized_updatesFlag() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Test", fileName = "test.txt", fileType = "txt", rawContent = "content"))

        scriptLineDao.insertAll(listOf(
            ScriptLine(scriptId = scriptId, lineNumber = 1, character = "A", dialogue = "test")
        ))

        val lines = scriptLineDao.getLinesForScriptSync(scriptId)
        assertEquals(false, lines[0].isMemorized)

        scriptLineDao.setMemorized(lines[0].id, true)

        val updated = scriptLineDao.getLinesForScriptSync(scriptId)
        assertEquals("Line should now be memorized", true, updated[0].isMemorized)
    }

    @Test
    fun deleteForScript_removesAllLines() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Test", fileName = "test.txt", fileType = "txt", rawContent = "content"))

        scriptLineDao.insertAll(listOf(
            ScriptLine(scriptId = scriptId, lineNumber = 1, character = "A", dialogue = "1"),
            ScriptLine(scriptId = scriptId, lineNumber = 2, character = "B", dialogue = "2")
        ))

        scriptLineDao.deleteForScript(scriptId)

        val remaining = scriptLineDao.getLinesForScriptSync(scriptId)
        assertEquals("All lines should be deleted", 0, remaining.size)
    }

    @Test
    fun insertCharacters_andQuery_returnsOrderedByLineCount() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Test", fileName = "test.txt", fileType = "txt", rawContent = "content"))

        characterDao.insertAll(listOf(
            Character(scriptId = scriptId, name = "MINOR", lineCount = 5),
            Character(scriptId = scriptId, name = "LEAD", lineCount = 50),
            Character(scriptId = scriptId, name = "SUPPORT", lineCount = 20)
        ))

        val chars = characterDao.getCharactersForScriptSync(scriptId)
        assertEquals(3, chars.size)
        assertEquals("LEAD", chars[0].name)
        assertEquals("SUPPORT", chars[1].name)
        assertEquals("MINOR", chars[2].name)
    }

    @Test
    fun setUserRole_andGetUserRole_works() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Test", fileName = "test.txt", fileType = "txt", rawContent = "content"))

        characterDao.insertAll(listOf(
            Character(scriptId = scriptId, name = "HAMLET", lineCount = 50),
            Character(scriptId = scriptId, name = "OPHELIA", lineCount = 20)
        ))

        val chars = characterDao.getCharactersForScriptSync(scriptId)
        characterDao.setUserRole(chars[0].id)

        val userRole = characterDao.getUserRole(scriptId)
        assertNotNull("User role should be set", userRole)
        assertEquals("HAMLET", userRole!!.name)
    }

    @Test
    fun clearUserRoles_resetsAllRoles() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Test", fileName = "test.txt", fileType = "txt", rawContent = "content"))

        characterDao.insertAll(listOf(
            Character(scriptId = scriptId, name = "A", lineCount = 10),
            Character(scriptId = scriptId, name = "B", lineCount = 5)
        ))

        val chars = characterDao.getCharactersForScriptSync(scriptId)
        characterDao.setUserRole(chars[0].id)

        characterDao.clearUserRoles(scriptId)

        val userRole = characterDao.getUserRole(scriptId)
        assertNull("User role should be cleared", userRole)
    }

    @Test
    fun deleteScript_cascadeDeletesLinesAndCharacters() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Test", fileName = "test.txt", fileType = "txt", rawContent = "content"))

        scriptLineDao.insertAll(listOf(
            ScriptLine(scriptId = scriptId, lineNumber = 1, character = "A", dialogue = "test")
        ))
        characterDao.insertAll(listOf(
            Character(scriptId = scriptId, name = "A", lineCount = 1)
        ))

        val script = scriptDao.getScriptById(scriptId)
        scriptDao.delete(script!!)

        val lines = scriptLineDao.getLinesForScriptSync(scriptId)
        val chars = characterDao.getCharactersForScriptSync(scriptId)
        assertEquals("Lines should be cascade-deleted", 0, lines.size)
        assertEquals("Characters should be cascade-deleted", 0, chars.size)
    }

    @Test
    fun insertScript_withLargeRawContent_succeeds() = runTest {
        val largeContent = "A".repeat(500_000)
        val scriptId = scriptDao.insert(Script(title = "Large Script", fileName = "large.pdf", fileType = "pdf", rawContent = largeContent))

        val loaded = scriptDao.getScriptById(scriptId)
        assertNotNull(loaded)
        assertEquals(500_000, loaded!!.rawContent.length)
    }

    @Test
    fun insertScript_withEmptyRawContent_succeeds() = runTest {
        val scriptId = scriptDao.insert(Script(title = "Empty Script", fileName = "empty.pdf", fileType = "pdf", rawContent = ""))

        val loaded = scriptDao.getScriptById(scriptId)
        assertNotNull(loaded)
        assertEquals("", loaded!!.rawContent)
    }

    @Test
    fun getLinesForScript_noLinesExist_returnsEmptyList() = runTest {
        val scriptId = scriptDao.insert(Script(title = "No Lines", fileName = "nolines.pdf", fileType = "pdf", rawContent = "just raw text"))

        val lines = scriptLineDao.getLinesForScriptSync(scriptId)
        assertEquals("Should return empty list, not crash", 0, lines.size)
    }

    @Test
    fun getCharactersForScript_noCharactersExist_returnsEmptyList() = runTest {
        val scriptId = scriptDao.insert(Script(title = "No Chars", fileName = "nochars.pdf", fileType = "pdf", rawContent = "text"))

        val chars = characterDao.getCharactersForScriptSync(scriptId)
        assertEquals("Should return empty list, not crash", 0, chars.size)
    }
}
