package mx.visionebc.actorstoolkit.ui.flow

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mx.visionebc.actorstoolkit.data.AppDatabase
import mx.visionebc.actorstoolkit.data.entity.Script
import mx.visionebc.actorstoolkit.data.entity.ScriptLine
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository
import mx.visionebc.actorstoolkit.util.ScriptParser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric UI flow tests — simulates real user actions
 * without requiring an emulator or physical device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], manifest = Config.NONE)
class UserFlowTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ScriptRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ScriptRepository(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    /** Helper: import a script (parse + save to DB), return scriptId */
    private suspend fun importScript(title: String, rawText: String, fileType: String = "txt"): Long {
        val script = Script(
            title = title,
            fileName = "${title.lowercase().replace(" ", "_")}.$fileType",
            fileType = fileType,
            rawContent = rawText
        )
        val scriptId = repository.insertScript(script)

        val parsed = if (fileType == "fdx") {
            ScriptParser.parseFdx(scriptId, rawText)
        } else {
            ScriptParser.parse(scriptId, rawText)
        }

        repository.insertLines(parsed.lines)
        repository.insertCharacters(parsed.characters)
        return scriptId
    }

    // ── FLOW 1: Import a script and verify it appears in list ───

    @Test
    fun `flow - import plain text script and list it`() = runBlocking {
        val rawText = """
            ROMEO
            But, soft! What light through yonder window breaks?
            It is the east, and Juliet is the sun.

            JULIET
            O Romeo, Romeo! Wherefore art thou Romeo?

            ROMEO
            I take thee at thy word.
        """.trimIndent()

        val scriptId = importScript("Romeo and Juliet - Balcony Scene", rawText)
        assertTrue("Script ID should be positive", scriptId > 0)

        // Verify script appears in list (like user sees the home screen)
        val allScripts = repository.getAllScriptInfos().first()
        assertEquals("Should have 1 script", 1, allScripts.size)
        assertEquals("Romeo and Juliet - Balcony Scene", allScripts[0].title)
    }

    // ── FLOW 2: Tap script → see detail with characters ────────

    @Test
    fun `flow - open script and see characters and lines`() = runBlocking {
        val rawText = """
            HAMLET
            To be, or not to be, that is the question.

            HORATIO
            My lord, I came to see your father's funeral.

            HAMLET
            Do not mock me, fellow student.
        """.trimIndent()

        val scriptId = importScript("Hamlet Act 3", rawText)

        // Simulate user tapping on script
        val info = repository.getScriptInfo(scriptId)
        assertNotNull("Script info should exist", info)
        assertEquals("Hamlet Act 3", info!!.title)

        // Load lines (as the detail screen does)
        val lines = repository.getLinesSync(scriptId)
        assertTrue("Should have lines", lines.isNotEmpty())

        // Verify characters extracted
        val characters = repository.getCharactersSync(scriptId)
        assertTrue("Should find HAMLET", characters.any { it.name.contains("HAMLET", ignoreCase = true) })
        assertTrue("Should find HORATIO", characters.any { it.name.contains("HORATIO", ignoreCase = true) })
    }

    // ── FLOW 3: Practice mode — hide selected role's lines ─────

    @Test
    fun `flow - practice mode hides selected character lines`() = runBlocking {
        val rawText = """
            MACBETH
            Is this a dagger which I see before me?

            LADY MACBETH
            Infirm of purpose! Give me the daggers.

            MACBETH
            Will all great Neptune's ocean wash this blood clean from my hand?
        """.trimIndent()

        val scriptId = importScript("Macbeth", rawText)
        val lines = repository.getLinesSync(scriptId)
        val selectedRole = "MACBETH"

        // Simulate practice mode: hide selected character's lines
        val visibleLines = lines.filter { it.character != selectedRole }
        val hiddenLines = lines.filter { it.character == selectedRole }

        assertTrue("Some lines should be hidden", hiddenLines.isNotEmpty())
        assertTrue("Some lines should be visible", visibleLines.isNotEmpty())
        assertTrue("All visible lines should be other characters",
            visibleLines.all { it.character != selectedRole })
    }

    // ── FLOW 4: Import multiple scripts ─────────────────────────

    @Test
    fun `flow - import multiple scripts and list all`() = runBlocking {
        val scripts = listOf(
            "Scene 1" to "ALICE\nHello there!\n\nBOB\nHi there!",
            "Scene 2" to "CHARLIE\nGood morning.\n\nDAVE\nGood night.",
            "Scene 3" to "EVE\nWhat time is it?\n\nFRANK\nIt is noon."
        )

        scripts.forEach { (title, content) ->
            importScript(title, content)
        }

        val allScripts = repository.getAllScriptInfos().first()
        assertEquals("Should have 3 scripts", 3, allScripts.size)
    }

    // ── FLOW 5: Delete a script ─────────────────────────────────

    @Test
    fun `flow - delete script removes it and its lines`() = runBlocking {
        val scriptId = importScript("Temp Script", "ACTOR\nSome line here.")

        // Verify it exists
        assertEquals(1, repository.getAllScriptInfos().first().size)

        // Delete (as user would from the detail screen)
        repository.deleteScriptById(scriptId)

        // Verify gone (cascade should remove lines too)
        assertEquals(0, repository.getAllScriptInfos().first().size)
        assertEquals(0, repository.getLinesSync(scriptId).size)
    }

    // ── FLOW 6: FDX screenplay format ───────────────────────────

    @Test
    fun `flow - import FDX screenplay format`() = runBlocking {
        val fdxContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <FinalDraft DocumentType="Script" Template="No" Version="1">
            <Content>
                <Paragraph Type="Scene Heading">
                    <Text>INT. COFFEE SHOP - DAY</Text>
                </Paragraph>
                <Paragraph Type="Character">
                    <Text>SARAH</Text>
                </Paragraph>
                <Paragraph Type="Dialogue">
                    <Text>I can't believe you said that.</Text>
                </Paragraph>
                <Paragraph Type="Character">
                    <Text>MIKE</Text>
                </Paragraph>
                <Paragraph Type="Dialogue">
                    <Text>What did you expect me to say?</Text>
                </Paragraph>
            </Content>
            </FinalDraft>
        """.trimIndent()

        val scriptId = importScript("Coffee Shop Scene", fdxContent, "fdx")
        assertTrue("Script should be saved", scriptId > 0)

        val lines = repository.getLinesSync(scriptId)
        assertTrue("FDX should parse dialogue lines", lines.isNotEmpty())

        val characters = repository.getCharactersSync(scriptId)
        assertTrue("Should find SARAH", characters.any { it.name == "SARAH" })
        assertTrue("Should find MIKE", characters.any { it.name == "MIKE" })
    }

    // ── FLOW 7: Practice stats tracking ─────────────────────────

    @Test
    fun `flow - practice session updates stats`() = runBlocking {
        val scriptId = importScript("Stats Test", "ACTOR\nLine one here.")

        // Initial state
        val before = repository.getScriptInfo(scriptId)
        assertEquals(0, before!!.practiceCount)

        // Simulate completing a practice session
        repository.updatePracticeStats(scriptId)
        val after = repository.getScriptInfo(scriptId)
        assertEquals(1, after!!.practiceCount)

        // Another session
        repository.updatePracticeStats(scriptId)
        val after2 = repository.getScriptInfo(scriptId)
        assertEquals(2, after2!!.practiceCount)
    }

    // ── FLOW 8: Large script handling ───────────────────────────

    @Test
    fun `flow - large script with many lines doesnt crash`() = runBlocking {
        val textLines = (1..200).map { i ->
            val char = if (i % 2 == 0) "ALICE" else "BOB"
            "$char\nThis is line number $i of the script."
        }
        val rawText = textLines.joinToString("\n\n")

        val scriptId = importScript("Large Script", rawText)
        val lines = repository.getLinesSync(scriptId)
        assertTrue("Should parse lines (got ${lines.size})", lines.size > 0)

        val characters = repository.getCharactersSync(scriptId)
        assertTrue("Should have characters", characters.size >= 2)
    }

    // ── FLOW 9: Select user role ────────────────────────────────

    @Test
    fun `flow - user selects their role for practice`() = runBlocking {
        val rawText = """
            ANNA
            Where are you going?

            BEN
            To the store.

            ANNA
            Can I come with you?
        """.trimIndent()

        val scriptId = importScript("Role Selection Test", rawText)
        val characters = repository.getCharactersSync(scriptId)
        assertTrue(characters.size >= 2)

        // User selects ANNA as their role
        val anna = characters.first { it.name == "ANNA" }
        repository.setUserRole(scriptId, anna.id)

        // Verify role is set
        val userRole = repository.getUserRole(scriptId)
        assertNotNull("User role should be set", userRole)
        assertEquals("ANNA", userRole!!.name)
        assertTrue(userRole.isUserRole)
    }

    // ── FLOW 10: Memorization tracking ──────────────────────────

    @Test
    fun `flow - mark lines as memorized during practice`() = runBlocking {
        val rawText = """
            STUDENT
            To be, or not to be, that is the question.

            TEACHER
            Very good! Continue.

            STUDENT
            Whether 'tis nobler in the mind to suffer.
        """.trimIndent()

        val scriptId = importScript("Memorization Test", rawText)
        val lines = repository.getLinesSync(scriptId)
        val studentLines = lines.filter { it.character == "STUDENT" }
        assertTrue(studentLines.isNotEmpty())

        // Mark first line as memorized
        repository.setMemorized(studentLines[0].id, true)

        // Verify
        val updatedLines = repository.getLinesSync(scriptId)
        val updatedFirst = updatedLines.first { it.id == studentLines[0].id }
        assertTrue("Line should be marked memorized", updatedFirst.isMemorized)
    }
}
