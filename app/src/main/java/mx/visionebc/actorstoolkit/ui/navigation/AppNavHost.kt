package mx.visionebc.actorstoolkit.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import mx.visionebc.actorstoolkit.ActorsToolkitApp
import mx.visionebc.actorstoolkit.data.repository.AuditionRepository
import mx.visionebc.actorstoolkit.data.repository.ScriptRepository
import mx.visionebc.actorstoolkit.data.repository.SelfTapeRepository
import mx.visionebc.actorstoolkit.ui.*
import mx.visionebc.actorstoolkit.ui.audition.AuditionViewModel
import mx.visionebc.actorstoolkit.ui.blocking.BlockingViewModel
import mx.visionebc.actorstoolkit.ui.selftape.SelfTapeViewModel
import mx.visionebc.actorstoolkit.ui.screen.*

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val db = remember { ActorsToolkitApp.instance.database }
    val repository = remember { ScriptRepository(db) }
    val auditionRepository = remember { AuditionRepository(db) }
    val selfTapeRepository = remember { SelfTapeRepository(db) }

    val crashLog = remember {
        ActorsToolkitApp.instance.getLastCrashLog()
    }

    LaunchedEffect(crashLog) {
        if (crashLog != null) {
            ActorsToolkitApp.instance.clearCrashLog()
        }
    }

    // Shared data for self-tape record screen
    val scripts by repository.getAllScripts().collectAsState(initial = emptyList())
    val scriptLinesMap = remember { mutableStateMapOf<Long, List<mx.visionebc.actorstoolkit.data.entity.ScriptLine>>() }

    // Load script lines when scripts change
    LaunchedEffect(scripts) {
        scripts.forEach { script ->
            if (!scriptLinesMap.containsKey(script.id)) {
                val lines = repository.getLinesSync(script.id)
                scriptLinesMap[script.id] = lines
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.ScriptList.route,
        modifier = modifier
    ) {
        composable(NavRoutes.ScriptList.route) {
            val vm: ScriptListViewModel = viewModel(factory = ScriptListViewModel.Factory(repository))
            ScriptListScreen(
                viewModel = vm,
                onImportClick = { navController.navigate(NavRoutes.ScriptImport.route) },
                onScriptClick = { scriptId ->
                    Log.d("AppNavHost", "Navigating to script detail: $scriptId")
                    navController.navigate(NavRoutes.ScriptDetail.withId(scriptId))
                },
                crashLog = crashLog
            )
        }

        composable(NavRoutes.ScriptImport.route) {
            val vm: ScriptImportViewModel = viewModel(factory = ScriptImportViewModel.Factory(repository))
            ScriptImportScreen(
                viewModel = vm,
                onImportComplete = { scriptId ->
                    Log.d("AppNavHost", "Import complete, navigating to detail: $scriptId")
                    navController.navigate(NavRoutes.ScriptDetail.withId(scriptId)) {
                        popUpTo(NavRoutes.ScriptList.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            NavRoutes.ScriptDetail.route,
            arguments = listOf(navArgument("scriptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getLong("scriptId") ?: 0L
            Log.d("AppNavHost", "ScriptDetail composable entered, scriptId=$scriptId")
            if (scriptId == 0L) {
                ErrorScreen("Invalid script ID", onBack = { navController.popBackStack() })
                return@composable
            }
            val vm: PracticeViewModel = viewModel(factory = PracticeViewModel.Factory(repository))
            ScriptDetailScreen(
                scriptId = scriptId,
                viewModel = vm,
                onPractice = { navController.navigate(NavRoutes.Practice.withId(scriptId)) },
                onBlocking = { navController.navigate(NavRoutes.Blocking.withId(scriptId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            NavRoutes.Practice.route,
            arguments = listOf(navArgument("scriptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getLong("scriptId") ?: 0L
            if (scriptId == 0L) {
                ErrorScreen("Invalid script ID", onBack = { navController.popBackStack() })
                return@composable
            }
            val vm: PracticeViewModel = viewModel(factory = PracticeViewModel.Factory(repository))
            PracticeScreen(
                scriptId = scriptId,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            NavRoutes.Blocking.route,
            arguments = listOf(navArgument("scriptId") { type = NavType.LongType })
        ) { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getLong("scriptId") ?: 0L
            if (scriptId == 0L) {
                ErrorScreen("Invalid script ID", onBack = { navController.popBackStack() })
                return@composable
            }
            val vm: BlockingViewModel = viewModel(factory = BlockingViewModel.Factory(repository))
            BlockingScreen(
                scriptId = scriptId,
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }

        // Audition screens
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
            AuditionEditScreen(
                auditionId = null,
                viewModel = vm,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            NavRoutes.AuditionEdit.route,
            arguments = listOf(navArgument("auditionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val auditionId = backStackEntry.arguments?.getLong("auditionId") ?: 0L
            if (auditionId == 0L) {
                ErrorScreen("Invalid audition ID", onBack = { navController.popBackStack() })
                return@composable
            }
            val vm: AuditionViewModel = viewModel(factory = AuditionViewModel.Factory(auditionRepository))
            AuditionEditScreen(
                auditionId = auditionId,
                viewModel = vm,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // Self-Tape screens
        composable(NavRoutes.SelfTapeList.route) {
            val vm: SelfTapeViewModel = viewModel(factory = SelfTapeViewModel.Factory(selfTapeRepository))
            SelfTapeListScreen(
                viewModel = vm,
                onRecordClick = { navController.navigate(NavRoutes.SelfTapeRecord.route) },
                onTapeClick = { id -> navController.navigate(NavRoutes.SelfTapePlayer.withId(id)) }
            )
        }

        composable(NavRoutes.SelfTapeRecord.route) {
            val vm: SelfTapeViewModel = viewModel(factory = SelfTapeViewModel.Factory(selfTapeRepository))
            SelfTapeRecordScreen(
                viewModel = vm,
                scripts = scripts,
                scriptLines = scriptLinesMap,
                onBack = { navController.popBackStack() },
                onSaved = { tapeId ->
                    navController.navigate(NavRoutes.SelfTapePlayer.withId(tapeId)) {
                        popUpTo(NavRoutes.SelfTapeList.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            NavRoutes.SelfTapePlayer.route,
            arguments = listOf(navArgument("tapeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val tapeId = backStackEntry.arguments?.getLong("tapeId") ?: 0L
            if (tapeId == 0L) {
                ErrorScreen("Invalid tape ID", onBack = { navController.popBackStack() })
                return@composable
            }
            val vm: SelfTapeViewModel = viewModel(factory = SelfTapeViewModel.Factory(selfTapeRepository))
            val auditions by vm.auditions.collectAsState()
            val auditionMap = remember(auditions) { auditions.associateBy { it.id } }
            SelfTapePlayerScreen(
                tapeId = tapeId,
                viewModel = vm,
                auditionMap = auditionMap,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun ErrorScreen(message: String, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onBack) { Text("Go Back") }
        }
    }
}
