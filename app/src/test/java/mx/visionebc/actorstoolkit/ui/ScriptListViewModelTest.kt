package mx.visionebc.actorstoolkit.ui

import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import mx.visionebc.actorstoolkit.data.entity.ScriptInfo
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptListViewModelTest {

    @MockK
    private lateinit var repository: ScriptRepository

    private val testDispatcher = StandardTestDispatcher()

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
    fun scripts_emitsListFromRepository() = runTest {
        val scripts = listOf(
            ScriptInfo(id = 1L, title = "Script A", fileName = "a.txt", fileType = "txt", createdAt = 1000L, updatedAt = 1000L, lastPracticedAt = null, practiceCount = 0),
            ScriptInfo(id = 2L, title = "Script B", fileName = "b.pdf", fileType = "pdf", createdAt = 2000L, updatedAt = 2000L, lastPracticedAt = null, practiceCount = 0)
        )
        every { repository.getAllScriptInfos() } returns flowOf(scripts)

        val viewModel = ScriptListViewModel(repository)

        val collectedScripts = mutableListOf<List<ScriptInfo>>()
        val job = launch(Dispatchers.Unconfined) {
            viewModel.scripts.collect { collectedScripts.add(it) }
        }
        advanceUntilIdle()

        val state = viewModel.scripts.value
        job.cancel()
        assertEquals(2, state.size)
    }

    @Test
    fun scripts_emptyList_handledCorrectly() = runTest {
        every { repository.getAllScriptInfos() } returns flowOf(emptyList())

        val viewModel = ScriptListViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.scripts.value
        assertEquals(0, state.size)
    }

    @Test
    fun deleteScript_callsRepository() = runTest {
        val scriptInfo = ScriptInfo(id = 1L, title = "X", fileName = "x.txt", fileType = "txt", createdAt = 1000L, updatedAt = 1000L, lastPracticedAt = null, practiceCount = 0)
        every { repository.getAllScriptInfos() } returns flowOf(listOf(scriptInfo))
        coEvery { repository.deleteScriptById(1L) } just Runs

        val viewModel = ScriptListViewModel(repository)
        viewModel.deleteScript(scriptInfo)
        advanceUntilIdle()

        coVerify { repository.deleteScriptById(1L) }
    }
}
