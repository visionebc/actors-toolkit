package mx.visionebc.actorstoolkit.ui.navigation

sealed class NavRoutes(val route: String) {
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
    object AuditionList : NavRoutes("auditions")
    object AuditionAdd : NavRoutes("auditions/add")
    object AuditionEdit : NavRoutes("auditions/{auditionId}/edit") {
        fun withId(id: Long) = "auditions/$id/edit"
    }
    object SelfTapeList : NavRoutes("selftapes")
    object SelfTapeRecord : NavRoutes("selftapes/record")
    object SelfTapePlayer : NavRoutes("selftapes/{tapeId}") {
        fun withId(id: Long) = "selftapes/$id"
    }
}
