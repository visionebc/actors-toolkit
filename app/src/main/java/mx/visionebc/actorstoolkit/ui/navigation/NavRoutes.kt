package mx.visionebc.actorstoolkit.ui.navigation

sealed class NavRoutes(val route: String) {
    // Project
    object ProjectList : NavRoutes("projects")
    object ProjectAdd : NavRoutes("projects/add")
    object ProjectEdit : NavRoutes("projects/{projectId}/edit") {
        fun withId(id: Long) = "projects/$id/edit"
    }
    object ProjectDetail : NavRoutes("projects/{projectId}") {
        fun withId(id: Long) = "projects/$id"
    }

    // Casting
    object CastingAdd : NavRoutes("projects/{projectId}/castings/add") {
        fun withId(projectId: Long) = "projects/$projectId/castings/add"
    }
    object CastingEdit : NavRoutes("castings/{castingId}/edit") {
        fun withId(id: Long) = "castings/$id/edit"
    }
    object CastingDetail : NavRoutes("castings/{castingId}") {
        fun withId(id: Long) = "castings/$id"
    }

    // Character
    object CharacterAdd : NavRoutes("castings/{castingId}/characters/add") {
        fun withId(castingId: Long) = "castings/$castingId/characters/add"
    }
    object CharacterEdit : NavRoutes("characters/{characterId}/edit") {
        fun withId(id: Long) = "characters/$id/edit"
    }
    object CharacterDetail : NavRoutes("characters/{characterId}") {
        fun withId(id: Long) = "characters/$id"
    }

    // Script Picker
    object ScriptPicker : NavRoutes("characters/{characterId}/scripts/pick") {
        fun withId(characterId: Long) = "characters/$characterId/scripts/pick"
    }
    object ScriptImportForCharacter : NavRoutes("scripts/import/{characterId}") {
        fun withId(characterId: Long) = "scripts/import/$characterId"
    }

    // Scripts (existing)
    object ScriptList : NavRoutes("scripts")
    object ScriptImport : NavRoutes("scripts/import")
    object ScriptDetail : NavRoutes("scripts/{scriptId}") {
        fun withId(id: Long) = "scripts/$id"
    }
    object Practice : NavRoutes("practice/{scriptId}") {
        fun withId(id: Long) = "practice/$id"
    }
    object RoleSelect : NavRoutes("roleselect/{scriptId}") {
        fun withId(id: Long) = "roleselect/$id"
    }
    object Blocking : NavRoutes("blocking/{scriptId}") {
        fun withId(id: Long) = "blocking/$id"
    }

    // Script import for project
    object ScriptImportForProject : NavRoutes("projects/{projectId}/scripts/import") {
        fun withId(projectId: Long) = "projects/$projectId/scripts/import"
    }

    // Auditions
    object AuditionList : NavRoutes("auditions")
    object AuditionAdd : NavRoutes("auditions/add")
    object AuditionAddForProject : NavRoutes("projects/{projectId}/auditions/add") {
        fun withId(projectId: Long) = "projects/$projectId/auditions/add"
    }
    object AuditionEdit : NavRoutes("auditions/{auditionId}/edit") {
        fun withId(id: Long) = "auditions/$id/edit"
    }
    // Settings
    object Settings : NavRoutes("settings")
    object Manual : NavRoutes("manual")
}
