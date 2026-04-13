package mx.visionebc.actorstoolkit.ui

import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import mx.visionebc.actorstoolkit.data.entity.Character
import mx.visionebc.actorstoolkit.data.entity.ScriptInfo
import mx.visionebc.actorstoolkit.data.entity.ScriptLine
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {

    @MockK
    private lateinit var repository: ScriptRepository

    private lateinit var viewModel: PracticeViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = PracticeViewModel(repository)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun scriptInfo(id: Long = 1L, title: String = "Test") = ScriptInfo(
        id = id, title = title, fileName = "test.txt", fileType = "txt",
        createdAt = 1000L, updatedAt = 1000L, lastPracticedAt = null, practiceCount = 0
    )

    @Test
    fun loadScript_withLines_setsStateCorrectly() = runTest {
        val lines = listOf(
            ScriptLine(id = 1, scriptId = 1L, lineNumber = 1, character = "HAMLET", dialogue = "To be or not to be"),
            ScriptLine(id = 2, scriptId = 1L, lineNumber = 2, character = "OPHELIA", dialogue = "My lord!")
        )
        val characters = listOf(
            Character(id = 1, scriptId = 1L, name = "HAMLET", lineCount = 1),
            Character(id = 2, scriptId = 1L, name = "OPHELIA", lineCount = 1)
        )

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo(title = "Hamlet")
        coEvery { repository.getLinesSync(1L) } returns lines
        coEvery { repository.getCharactersSync(1L) } returns characters
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertEquals("Hamlet", state.scriptTitle)
        assertEquals(2, state.lines.size)
        assertEquals(2, state.characters.size)
        assertTrue("Should show role selector when no role set", state.showRoleSelector)
    }

    @Test
    fun loadScript_withZeroLines_doesNotCrash() = runTest {
        coEvery { repository.getScriptInfo(1L) } returns scriptInfo(title = "Bad PDF")
        coEvery { repository.getLinesSync(1L) } returns emptyList()
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse("Should not be loading", state.isLoading)
        assertNull("Should have no error", state.error)
        assertEquals(0, state.lines.size)
        assertEquals(0, state.characters.size)
        assertFalse("No role selector when no characters", state.showRoleSelector)
    }

    @Test
    fun loadScript_scriptNotFound_setsError() = runTest {
        coEvery { repository.getScriptInfo(999L) } returns null

        viewModel.loadScript(999L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull("Should have error when script not found", state.error)
        assertTrue(state.error!!.contains("not found"))
    }

    @Test
    fun loadScript_repositoryThrows_setsError() = runTest {
        coEvery { repository.getScriptInfo(1L) } throws RuntimeException("DB error")

        viewModel.loadScript(1L)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNotNull("Should have error", state.error)
    }

    @Test
    fun nextLine_withEmptyLines_doesNotCrash() = runTest {
        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns emptyList()
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        viewModel.nextLine()
        assertEquals(0, viewModel.state.value.currentLineIndex)
    }

    @Test
    fun previousLine_withEmptyLines_doesNotCrash() = runTest {
        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns emptyList()
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        viewModel.previousLine()
        assertEquals(0, viewModel.state.value.currentLineIndex)
    }

    @Test
    fun nextLine_advancesIndex() = runTest {
        val lines = listOf(
            ScriptLine(id = 1, scriptId = 1L, lineNumber = 1, character = "A", dialogue = "One"),
            ScriptLine(id = 2, scriptId = 1L, lineNumber = 2, character = "B", dialogue = "Two"),
            ScriptLine(id = 3, scriptId = 1L, lineNumber = 3, character = "A", dialogue = "Three")
        )

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns lines
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.currentLineIndex)
        viewModel.nextLine()
        assertEquals(1, viewModel.state.value.currentLineIndex)
        viewModel.nextLine()
        assertEquals(2, viewModel.state.value.currentLineIndex)
        viewModel.nextLine()
        assertEquals(2, viewModel.state.value.currentLineIndex)
    }

    @Test
    fun goToLine_outOfBounds_doesNotCrash() = runTest {
        val lines = listOf(
            ScriptLine(id = 1, scriptId = 1L, lineNumber = 1, character = "A", dialogue = "One")
        )

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns lines
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        viewModel.goToLine(-1)
        assertEquals(0, viewModel.state.value.currentLineIndex)

        viewModel.goToLine(100)
        assertEquals(0, viewModel.state.value.currentLineIndex)
    }

    @Test
    fun progress_calculatedCorrectly() = runTest {
        val lines = listOf(
            ScriptLine(id = 1, scriptId = 1L, lineNumber = 1, character = "A", dialogue = "1"),
            ScriptLine(id = 2, scriptId = 1L, lineNumber = 2, character = "B", dialogue = "2"),
            ScriptLine(id = 3, scriptId = 1L, lineNumber = 3, character = "A", dialogue = "3"),
            ScriptLine(id = 4, scriptId = 1L, lineNumber = 4, character = "B", dialogue = "4")
        )

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns lines
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        assertEquals(0f, viewModel.state.value.progress, 0.01f)
        viewModel.nextLine()
        assertEquals(0.25f, viewModel.state.value.progress, 0.01f)
        viewModel.nextLine()
        assertEquals(0.5f, viewModel.state.value.progress, 0.01f)
    }

    @Test
    fun progress_withEmptyLines_doesNotDivideByZero() = runTest {
        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns emptyList()
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        assertEquals(0f, viewModel.state.value.progress, 0.01f)
    }

    @Test
    fun setMode_resetsIndexAndRevealed() = runTest {
        val lines = listOf(
            ScriptLine(id = 1, scriptId = 1L, lineNumber = 1, character = "A", dialogue = "1"),
            ScriptLine(id = 2, scriptId = 1L, lineNumber = 2, character = "B", dialogue = "2")
        )

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns lines
        coEvery { repository.getCharactersSync(1L) } returns emptyList()
        coEvery { repository.getUserRole(1L) } returns null
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        viewModel.nextLine()
        viewModel.revealLine(1L)
        viewModel.setMode(PracticeMode.MEMORIZATION)

        assertEquals(0, viewModel.state.value.currentLineIndex)
        assertTrue(viewModel.state.value.revealedLines.isEmpty())
        assertEquals(PracticeMode.MEMORIZATION, viewModel.state.value.mode)
    }

    @Test
    fun revealLine_addsToSet() {
        viewModel.revealLine(42L)
        assertTrue(viewModel.state.value.revealedLines.contains(42L))
        viewModel.revealLine(43L)
        assertTrue(viewModel.state.value.revealedLines.contains(42L))
        assertTrue(viewModel.state.value.revealedLines.contains(43L))
    }

    @Test
    fun selectRole_updatesUserRole() = runTest {
        val character = Character(id = 5L, scriptId = 1L, name = "HAMLET", lineCount = 50)
        coEvery { repository.setUserRole(1L, 5L) } just Runs

        viewModel.selectRole(character)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNotNull(state.userRole)
        assertEquals("HAMLET", state.userRole!!.name)
        assertFalse(state.showRoleSelector)
    }

    @Test
    fun nextLine_withUserRole_doesNotCrashWithNullTts() = runTest {
        val userChar = Character(id = 1, scriptId = 1L, name = "HAMLET", lineCount = 2, isUserRole = true)
        val lines = listOf(
            ScriptLine(id = 1, scriptId = 1L, lineNumber = 1, character = "HAMLET", dialogue = "My line"),
            ScriptLine(id = 2, scriptId = 1L, lineNumber = 2, character = "OPHELIA", dialogue = "Her line")
        )

        coEvery { repository.getScriptInfo(1L) } returns scriptInfo()
        coEvery { repository.getLinesSync(1L) } returns lines
        coEvery { repository.getCharactersSync(1L) } returns listOf(userChar)
        coEvery { repository.getUserRole(1L) } returns userChar
        coEvery { repository.updatePracticeStats(1L) } just Runs

        viewModel.loadScript(1L)
        advanceUntilIdle()

        viewModel.nextLine()
        assertEquals(1, viewModel.state.value.currentLineIndex)
    }
}
