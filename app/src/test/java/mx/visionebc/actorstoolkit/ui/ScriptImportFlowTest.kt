package mx.visionebc.actorstoolkit.ui

import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import mx.visionebc.actorstoolkit.data.entity.ScriptInfo
import mx.visionebc.actorstoolkit.data.entity.ScriptLine
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository
import mx.visionebc.actorstoolkit.util.ScriptParser
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptImportFlowTest {

    @MockK
    private lateinit var repository: ScriptRepository

    private val testDispatcher = StandardTestDispatcher()

    private fun scriptInfo(id: Long = 1L, title: String = "Test") = ScriptInfo(
        id = id, title = title, fileName = "test.pdf", fileType = "pdf",
        createdAt = 1000L, updatedAt = 1000L, lastPracticedAt = null, practiceCount = 0
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun fullFlow_pdfWithNoRecognizableDialogue_doesNotCrash() = runTest {
        val pdfRawText = """
            Lorem ipsum dolor sit amet, consectetur adipiscing elit.
            Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
        """.trimIndent()

        val parsed = ScriptParser.parse(1L, pdfRawText)
        assertEquals("PDF with no dialogue should produce 0 lines", 0, parsed.lines.size)
        assertEquals("PDF with no dialogue should produce 0 characters", 0, parsed.characters.size)

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo(title = "My PDF Script")
        coEvery { repository.getLinesSync(1L) } returns parsed.lines
        coEvery { repository.getCharactersSync(1L) } returns parsed.characters
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        val viewModel = PracticeViewModel(repository)
        viewModel.loadScript(1L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertEquals("My PDF Script", state.scriptTitle)
        assertEquals(0, state.lines.size)
        assertFalse("Should NOT show role selector with 0 characters", state.showRoleSelector)

        viewModel.nextLine()
        viewModel.previousLine()
        viewModel.goToLine(0)
    }

    @Test
    fun fullFlow_pdfWithValidScreenplay_loadsCorrectly() = runTest {
        val pdfRawText = """
ROMEO: But, soft! what light through yonder window breaks?
JULIET: O Romeo, Romeo! wherefore art thou Romeo?
ROMEO: Shall I hear more, or shall I speak at this?
        """.trimIndent()

        val parsed = ScriptParser.parse(1L, pdfRawText)
        assertTrue("Should find dialogue lines", parsed.lines.isNotEmpty())
        assertTrue("Should find characters", parsed.characters.isNotEmpty())

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo(title = "Romeo and Juliet")
        coEvery { repository.getLinesSync(1L) } returns parsed.lines
        coEvery { repository.getCharactersSync(1L) } returns parsed.characters
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        val viewModel = PracticeViewModel(repository)
        viewModel.loadScript(1L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(3, state.lines.size)
        assertEquals(2, state.characters.size)
        assertTrue("Should show role selector", state.showRoleSelector)

        viewModel.nextLine()
        assertEquals(1, viewModel.state.value.currentLineIndex)
        viewModel.nextLine()
        assertEquals(2, viewModel.state.value.currentLineIndex)
        viewModel.nextLine()
        assertEquals(2, viewModel.state.value.currentLineIndex)
    }

    @Test
    fun fullFlow_pdfWithOnlyWhitespace_doesNotCrash() = runTest {
        val pdfRawText = "   \n\n   \n  \t  \n\n\n   "

        val parsed = ScriptParser.parse(1L, pdfRawText)
        assertEquals(0, parsed.lines.size)

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo(title = "Blank PDF")
        coEvery { repository.getLinesSync(1L) } returns emptyList()
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        val viewModel = PracticeViewModel(repository)
        viewModel.loadScript(1L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.error)
        assertEquals(0, state.lines.size)
    }

    @Test
    fun fullFlow_pdfWithIndentedScreenplay_handledCorrectly() = runTest {
        val pdfRawText = """
                                                               1.


          FADE IN:

          INT. COFFEE SHOP - DAY

          A busy coffee shop. EMMA (30s) sits at a table.

                              EMMA
                    Can I get a latte, please?

                              BARISTA
                    Sure thing. Name?

                              EMMA
                    Emma.
        """.trimIndent()

        val parsed = ScriptParser.parse(1L, pdfRawText)

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo(title = "Coffee Shop Scene")
        coEvery { repository.getLinesSync(1L) } returns parsed.lines
        coEvery { repository.getCharactersSync(1L) } returns parsed.characters
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        val viewModel = PracticeViewModel(repository)
        viewModel.loadScript(1L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull("Should not have error", state.error)
        assertFalse("Should not be loading", state.isLoading)

        if (state.lines.isNotEmpty()) {
            for (i in 0 until minOf(state.lines.size, 10)) {
                viewModel.nextLine()
            }
        }
    }

    @Test
    fun accessPattern_currentLineWithEmptyList_isDefensive() {
        val lines = emptyList<ScriptLine>()
        val currentLineIndex = 0

        assertFalse("Guard should prevent access", currentLineIndex < lines.size - 1)

        val lineCountText = if (lines.isNotEmpty()) {
            "Line ${currentLineIndex + 1} of ${lines.size}"
        } else {
            "No lines"
        }
        assertEquals("No lines", lineCountText)
    }

    @Test
    fun separateViewModels_independentState() = runTest {
        val parsed = ScriptParser.parse(1L, "ROMEO: Hello.")

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns parsed.lines
        coEvery { repository.getCharactersSync(1L) } returns parsed.characters
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        val vm1 = PracticeViewModel(repository)
        vm1.loadScript(1L)
        advanceUntilIdle()

        val vm2 = PracticeViewModel(repository)
        assertTrue("VM2 should start loading", vm2.state.value.isLoading)
        assertEquals(0, vm2.state.value.lines.size)

        vm2.loadScript(1L)
        advanceUntilIdle()

        assertEquals(1, vm1.state.value.lines.size)
        assertEquals(1, vm2.state.value.lines.size)
    }
}
