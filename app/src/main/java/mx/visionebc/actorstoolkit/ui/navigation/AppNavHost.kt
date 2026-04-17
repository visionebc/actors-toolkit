package mx.visionebc.actorstoolkit.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import mx.visionebc.actorstoolkit.ActorsToolkitApp
import mx.visionebc.actorstoolkit.data.preferences.SettingsDataStore
import mx.visionebc.actorstoolkit.data.repository.AuditionRepository
import mx.visionebc.actorstoolkit.data.repository.CastingRepository
import mx.visionebc.actorstoolkit.data.repository.ProjectRepository
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository
import mx.visionebc.actorstoolkit.ui.*
import mx.visionebc.actorstoolkit.ui.audition.AuditionViewModel
import mx.visionebc.actorstoolkit.ui.blocking.BlockingViewModel
import mx.visionebc.actorstoolkit.ui.casting.CastingViewModel
import mx.visionebc.actorstoolkit.ui.character.CharacterViewModel
import mx.visionebc.actorstoolkit.ui.project.ProjectViewModel
import mx.visionebc.actorstoolkit.ui.settings.SettingsViewModel
import mx.visionebc.actorstoolkit.ui.screen.*
import mx.visionebc.actorstoolkit.ui.screen.ProjectTab

@Composable
fun AppNavHost(
    navController: NavHostController,
    settingsDataStore: SettingsDataStore,
    onRestartApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val db = remember { ActorsToolkitApp.instance.database }
    val repository = remember { ScriptRepository(db) }
    val auditionRepository = remember { AuditionRepository(db) }
    val projectRepository = remember { ProjectRepository(db) }
    val castingRepository = remember { CastingRepository(db) }

    val crashLog = remember { ActorsToolkitApp.instance.getLastCrashLog() }

    LaunchedEffect(crashLog) {
        if (crashLog != null) ActorsToolkitApp.instance.clearCrashLog()
    }

    // Track current project context for nav bar on all screens
    var currentProjectId by remember { mutableStateOf(0L) }
    var currentProjectName by remember { mutableStateOf("") }
    var currentProjectTab by remember { mutableStateOf(ProjectTab.PROJECT) }

    fun navigateToProjectTab(tab: ProjectTab) {
        currentProjectTab = tab
        if (currentProjectId > 0) {
            navController.navigate(NavRoutes.ProjectDetail.withId(currentProjectId)) {
                popUpTo(NavRoutes.ProjectDetail.withId(currentProjectId)) { inclusive = true }
            }
        }
    }

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = NavRoutes.ProjectList.route,
            modifier = Modifier.fillMaxSize()
        ) {
        // ── Project screens ──
        composable(NavRoutes.ProjectList.route) {
            val vm: ProjectViewModel = viewModel(factory = ProjectViewModel.Factory(projectRepository))
            ProjectListScreen(
                viewModel = vm,
                onAddClick = { navController.navigate(NavRoutes.ProjectAdd.route) },
                onProjectClick = { id -> navController.navigate(NavRoutes.ProjectDetail.withId(id)) },
                crashLog = crashLog
            )
        }

        composable(NavRoutes.ProjectAdd.route) {
            val vm: ProjectViewModel = viewModel(factory = ProjectViewModel.Factory(projectRepository))
            ProjectAddEditScreen(projectId = null, viewModel = vm, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.ProjectEdit.route, arguments = listOf(navArgument("projectId") { type = NavType.LongType })) { entry ->
            val pId = entry.arguments?.getLong("projectId") ?: 0L
            if (pId == 0L) { ErrorScreen("Invalid project ID") { navController.popBackStack() }; return@composable }
            val vm: ProjectViewModel = viewModel(factory = ProjectViewModel.Factory(projectRepository))
            ProjectAddEditScreen(projectId = pId, viewModel = vm, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.ProjectDetail.route, arguments = listOf(navArgument("projectId") { type = NavType.LongType })) { entry ->
            val pId = entry.arguments?.getLong("projectId") ?: 0L
            if (pId == 0L) { ErrorScreen("Invalid project ID") { navController.popBackStack() }; return@composable }
            LaunchedEffect(pId) { currentProjectId = pId }
            val pvm: ProjectViewModel = viewModel(factory = ProjectViewModel.Factory(projectRepository))
            val proj by pvm.selectedProject.collectAsState()
            LaunchedEffect(proj) { if (proj != null) currentProjectName = proj!!.name }
            val cvm: CastingViewModel = viewModel(factory = CastingViewModel.Factory(castingRepository))
            val auditions by auditionRepository.getAuditionsByProject(pId).collectAsState(initial = emptyList())
            val linkedScripts by db.characterScriptDao().getScriptInfosForProject(pId).collectAsState(initial = emptyList())
            val directScripts by db.scriptDao().getScriptInfosByProject(pId).collectAsState(initial = emptyList())
            val projectScripts = remember(linkedScripts, directScripts) {
                (directScripts + linkedScripts).distinctBy { it.id }
            }
            val projectContacts by db.projectContactDao().getByProjectId(pId).collectAsState(initial = emptyList())
            val contactScope = rememberCoroutineScope()
            ProjectDetailScreen(
                projectId = pId,
                projectViewModel = pvm,
                castingViewModel = cvm,
                onBack = { navController.popBackStack() },
                onEdit = { },
                settingsContent = { mod ->
                    val svm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(settingsDataStore))
                    val allProjects by projectRepository.getAllProjects().collectAsState(initial = emptyList())
                    SettingsScreen(viewModel = svm, onBack = { }, onRestartApp = onRestartApp, projects = allProjects, onOpenManual = { navController.navigate(NavRoutes.Manual.route) })
                },
                onAddCasting = { navController.navigate(NavRoutes.CastingAdd.withId(pId)) },
                onCastingClick = { id -> navController.navigate(NavRoutes.CastingDetail.withId(id)) },
                onAddAudition = { navController.navigate(NavRoutes.AuditionAddForProject.withId(pId)) },
                onAuditionClick = { id -> navController.navigate(NavRoutes.AuditionEdit.withId(id)) },
                onScriptClick = { scriptId -> navController.navigate(NavRoutes.ScriptDetail.withId(scriptId)) },
                onImportScript = { navController.navigate(NavRoutes.ScriptImportForProject.withId(pId)) },
                auditions = auditions,
                projectScripts = projectScripts,
                contacts = projectContacts,
                onSaveContact = { contact -> contactScope.launch { if (contact.id == 0L) db.projectContactDao().insert(contact) else db.projectContactDao().update(contact) } },
                onDeleteContact = { contact -> contactScope.launch { db.projectContactDao().delete(contact) } },
                initialTab = currentProjectTab
            )
        }

        // ── Casting screens ──
        composable(NavRoutes.CastingAdd.route, arguments = listOf(navArgument("projectId") { type = NavType.LongType })) { entry ->
            val pId = entry.arguments?.getLong("projectId") ?: 0L
            val vm: CastingViewModel = viewModel(factory = CastingViewModel.Factory(castingRepository))
            CastingAddEditScreen(castingId = null, projectId = pId, viewModel = vm, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CastingEdit.route, arguments = listOf(navArgument("castingId") { type = NavType.LongType })) { entry ->
            val cId = entry.arguments?.getLong("castingId") ?: 0L
            if (cId == 0L) { ErrorScreen("Invalid casting ID") { navController.popBackStack() }; return@composable }
            val vm: CastingViewModel = viewModel(factory = CastingViewModel.Factory(castingRepository))
            CastingAddEditScreen(castingId = cId, projectId = 0, viewModel = vm, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CastingDetail.route, arguments = listOf(navArgument("castingId") { type = NavType.LongType })) { entry ->
            val cId = entry.arguments?.getLong("castingId") ?: 0L
            if (cId == 0L) { ErrorScreen("Invalid casting ID") { navController.popBackStack() }; return@composable }
            val cvm: CastingViewModel = viewModel(factory = CastingViewModel.Factory(castingRepository))
            val chvm: CharacterViewModel = viewModel(factory = CharacterViewModel.Factory(db))
            CastingDetailScreen(
                castingId = cId,
                castingViewModel = cvm,
                characterViewModel = chvm,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(NavRoutes.CastingEdit.withId(cId)) },
                onAddCharacter = { navController.navigate(NavRoutes.CharacterAdd.withId(cId)) },
                onCharacterClick = { id -> navController.navigate(NavRoutes.CharacterDetail.withId(id)) }
            )
        }

        // ── Character screens ──
        composable(NavRoutes.CharacterAdd.route, arguments = listOf(navArgument("castingId") { type = NavType.LongType })) { entry ->
            val cId = entry.arguments?.getLong("castingId") ?: 0L
            val vm: CharacterViewModel = viewModel(factory = CharacterViewModel.Factory(db))
            CharacterAddEditScreen(characterId = null, castingId = cId, viewModel = vm, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CharacterEdit.route, arguments = listOf(navArgument("characterId") { type = NavType.LongType })) { entry ->
            val chId = entry.arguments?.getLong("characterId") ?: 0L
            if (chId == 0L) { ErrorScreen("Invalid character ID") { navController.popBackStack() }; return@composable }
            val vm: CharacterViewModel = viewModel(factory = CharacterViewModel.Factory(db))
            CharacterAddEditScreen(characterId = chId, castingId = 0, viewModel = vm, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.CharacterDetail.route, arguments = listOf(navArgument("characterId") { type = NavType.LongType })) { entry ->
            val chId = entry.arguments?.getLong("characterId") ?: 0L
            if (chId == 0L) { ErrorScreen("Invalid character ID") { navController.popBackStack() }; return@composable }
            val vm: CharacterViewModel = viewModel(factory = CharacterViewModel.Factory(db))
            CharacterDetailScreen(
                characterId = chId,
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(NavRoutes.CharacterEdit.withId(chId)) },
                onScriptClick = { scriptId -> navController.navigate(NavRoutes.ScriptDetail.withId(scriptId)) },
                onAddScript = { navController.navigate(NavRoutes.ScriptImportForCharacter.withId(chId)) },
                onPickScript = { navController.navigate(NavRoutes.ScriptPicker.withId(chId)) }
            )
        }

        // ── Script Picker ──
        composable(NavRoutes.ScriptPicker.route, arguments = listOf(navArgument("characterId") { type = NavType.LongType })) { entry ->
            val chId = entry.arguments?.getLong("characterId") ?: 0L
            val vm: CharacterViewModel = viewModel(factory = CharacterViewModel.Factory(db))
            ScriptPickerScreen(characterId = chId, viewModel = vm, onBack = { navController.popBackStack() })
        }

        // ── Script Import for Character (auto-links after import) ──
        composable(NavRoutes.ScriptImportForCharacter.route, arguments = listOf(navArgument("characterId") { type = NavType.LongType })) { entry ->
            val chId = entry.arguments?.getLong("characterId") ?: 0L
            val vm: ScriptImportViewModel = viewModel(factory = ScriptImportViewModel.Factory(repository))
            val scope = rememberCoroutineScope()
            ScriptImportScreen(
                viewModel = vm,
                onImportComplete = { scriptId ->
                    // Auto-link to character
                    scope.launch {
                        db.characterScriptDao().link(mx.visionebc.actorstoolkit.data.entity.CharacterScript(chId, scriptId))
                    }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Script screens (existing) ──
        composable(NavRoutes.ScriptList.route) {
            val vm: ScriptListViewModel = viewModel(factory = ScriptListViewModel.Factory(repository))
            ScriptListScreen(
                viewModel = vm,
                onImportClick = { navController.navigate(NavRoutes.ScriptImport.route) },
                onScriptClick = { scriptId -> navController.navigate(NavRoutes.ScriptDetail.withId(scriptId)) }
            )
        }

        composable(NavRoutes.ScriptImport.route) {
            val vm: ScriptImportViewModel = viewModel(factory = ScriptImportViewModel.Factory(repository))
            ScriptImportScreen(
                viewModel = vm,
                onImportComplete = { scriptId ->
                    navController.navigate(NavRoutes.ScriptDetail.withId(scriptId)) {
                        popUpTo(NavRoutes.ScriptList.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Script import for project (auto-links to project)
        composable(NavRoutes.ScriptImportForProject.route, arguments = listOf(navArgument("projectId") { type = NavType.LongType })) { entry ->
            val pId = entry.arguments?.getLong("projectId") ?: 0L
            val vm: ScriptImportViewModel = viewModel(factory = ScriptImportViewModel.Factory(repository))
            vm.projectId = pId
            ScriptImportScreen(
                viewModel = vm,
                onImportComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ScriptDetail.route, arguments = listOf(navArgument("scriptId") { type = NavType.LongType })) { entry ->
            val scriptId = entry.arguments?.getLong("scriptId") ?: 0L
            if (scriptId == 0L) { ErrorScreen("Invalid script ID") { navController.popBackStack() }; return@composable }
            val vm: PracticeViewModel = viewModel(factory = PracticeViewModel.Factory(repository))
            ProjectNavScaffold(title = currentProjectName, selectedTab = ProjectTab.SCRIPTS, onTabSelected = { navigateToProjectTab(it) }, onBack = { navController.popBackStack() }) { padding ->
                Box(Modifier.padding(padding)) {
                    ScriptDetailScreen(
                        scriptId = scriptId, viewModel = vm,
                        onPractice = { navController.navigate(NavRoutes.Practice.withId(scriptId)) },
                        onBlocking = { navController.navigate(NavRoutes.Blocking.withId(scriptId)) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composable(NavRoutes.Practice.route, arguments = listOf(navArgument("scriptId") { type = NavType.LongType })) { entry ->
            val scriptId = entry.arguments?.getLong("scriptId") ?: 0L
            if (scriptId == 0L) { ErrorScreen("Invalid script ID") { navController.popBackStack() }; return@composable }
            val vm: PracticeViewModel = viewModel(factory = PracticeViewModel.Factory(repository))
            ProjectNavScaffold(title = currentProjectName, selectedTab = ProjectTab.SCRIPTS, onTabSelected = { navigateToProjectTab(it) }, onBack = { navController.popBackStack() }) { padding ->
                Box(Modifier.padding(padding)) {
                    PracticeScreen(scriptId = scriptId, viewModel = vm, onBack = { navController.popBackStack() })
                }
            }
        }

        composable(NavRoutes.Blocking.route, arguments = listOf(navArgument("scriptId") { type = NavType.LongType })) { entry ->
            val scriptId = entry.arguments?.getLong("scriptId") ?: 0L
            if (scriptId == 0L) { ErrorScreen("Invalid script ID") { navController.popBackStack() }; return@composable }
            val vm: BlockingViewModel = viewModel(factory = BlockingViewModel.Factory(repository))
            ProjectNavScaffold(title = currentProjectName, selectedTab = ProjectTab.SCRIPTS, onTabSelected = { navigateToProjectTab(it) }, onBack = { navController.popBackStack() }) { padding ->
                Box(Modifier.padding(padding)) {
                    BlockingScreen(scriptId = scriptId, viewModel = vm, onBack = { navController.popBackStack() })
                }
            }
        }

        // ── Audition screens ──
        composable(NavRoutes.AuditionList.route) {
            val vm: AuditionViewModel = viewModel(factory = AuditionViewModel.Factory(auditionRepository))
            AuditionListScreen(
                viewModel = vm,
                onAddClick = { navController.navigate(NavRoutes.AuditionAdd.route) },
                onAuditionClick = { id -> navController.navigate(NavRoutes.AuditionEdit.withId(id)) }
            )
        }

        composable(NavRoutes.AuditionAdd.route) {
            val vm: AuditionViewModel = viewModel(factory = AuditionViewModel.Factory(auditionRepository))
            AuditionEditScreen(auditionId = null, viewModel = vm, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.AuditionAddForProject.route, arguments = listOf(navArgument("projectId") { type = NavType.LongType })) { entry ->
            val pId = entry.arguments?.getLong("projectId") ?: 0L
            val vm: AuditionViewModel = viewModel(factory = AuditionViewModel.Factory(auditionRepository))
            val pvm: ProjectViewModel = viewModel(factory = ProjectViewModel.Factory(projectRepository))
            val proj by pvm.selectedProject.collectAsState()
            LaunchedEffect(pId) { pvm.loadProject(pId) }
            AuditionEditScreen(
                auditionId = null, viewModel = vm, projectId = pId, projectName = proj?.name ?: "",
                onSaved = {
                    currentProjectId = pId
                    navigateToProjectTab(ProjectTab.AUDITIONS)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.AuditionEdit.route, arguments = listOf(navArgument("auditionId") { type = NavType.LongType })) { entry ->
            val auditionId = entry.arguments?.getLong("auditionId") ?: 0L
            if (auditionId == 0L) { ErrorScreen("Invalid audition ID") { navController.popBackStack() }; return@composable }
            val vm: AuditionViewModel = viewModel(factory = AuditionViewModel.Factory(auditionRepository))
            AuditionEditScreen(auditionId = auditionId, viewModel = vm, onSaved = { navController.popBackStack() }, onBack = { navController.popBackStack() })
        }

        // ── Settings ──
        composable(NavRoutes.Settings.route) {
            val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(settingsDataStore))
            val allProjects2 by projectRepository.getAllProjects().collectAsState(initial = emptyList())
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onRestartApp = onRestartApp,
                projects = allProjects2,
                onOpenManual = { navController.navigate(NavRoutes.Manual.route) }
            )
        }

        composable(NavRoutes.Manual.route) {
            ManualScreen(onBack = { navController.popBackStack() })
        }
        }

        if (currentRoute != null && currentRoute != NavRoutes.Settings.route && currentRoute != NavRoutes.Manual.route) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(end = 8.dp, top = 4.dp)
                    .size(40.dp)
            ) {
                IconButton(onClick = { navController.navigate(NavRoutes.Settings.route) }) {
                    Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Go Back") }
        }
    }
}
