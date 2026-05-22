package ai.kraftshala.attendance.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import ai.kraftshala.attendance.data.RolePref
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ai.kraftshala.attendance.auth.AuthViewModel
import ai.kraftshala.attendance.ui.common.BluetoothOffScreen
import ai.kraftshala.attendance.ui.common.ProfileScreen
import ai.kraftshala.attendance.ui.instructor.FacultyCockpitScreen
import ai.kraftshala.attendance.ui.instructor.FacultyHomeScreen
import ai.kraftshala.attendance.ui.instructor.FacultySessionDetailScreen
import ai.kraftshala.attendance.ui.instructor.FacultySessionsScreen
import ai.kraftshala.attendance.ui.onboarding.LoginScreen
import ai.kraftshala.attendance.ui.onboarding.PermissionsScreen
import ai.kraftshala.attendance.ui.onboarding.RoleSelectionScreen
import ai.kraftshala.attendance.ui.onboarding.TermsScreen
import ai.kraftshala.attendance.ui.student.LectureDetailScreen
import ai.kraftshala.attendance.ui.student.MarkedConfirmationScreen
import ai.kraftshala.attendance.ui.student.StudentHomeScreen
import ai.kraftshala.attendance.ui.student.StudentSessionDetailScreen
import ai.kraftshala.attendance.ui.student.StudentSessionsScreen

object Routes {
    const val ROLE = "role_selection"
    const val LOGIN = "login"
    const val PERMISSIONS = "permissions"
    const val TERMS = "terms"
    const val BLUETOOTH_OFF = "bluetooth_off"

    const val STUDENT_HOME = "student/home"
    const val STUDENT_LECTURE = "student/lecture/{id}"
    const val STUDENT_MARKED = "student/marked/{id}"
    const val STUDENT_SESSIONS = "student/sessions"
    const val STUDENT_SESSION_DETAIL = "student/session/{id}"
    const val STUDENT_PROFILE = "student/profile"

    const val FACULTY_HOME = "instructor/home"
    const val FACULTY_COCKPIT = "instructor/cockpit/{sessionId}"
    const val FACULTY_SESSIONS = "instructor/sessions"
    const val FACULTY_SESSION_DETAIL = "instructor/session/{id}"
    const val FACULTY_PROFILE = "instructor/profile"
}

@Composable
fun KraftshalaNavGraph() {
    val nav: NavHostController = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val auth by authVm.state.collectAsState()
    val context = LocalContext.current

    val start = when {
        auth.user == null -> Routes.ROLE
        auth.user?.role == "instructor" -> Routes.FACULTY_HOME
        else -> Routes.STUDENT_HOME
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.ROLE) {
            RoleSelectionScreen(onContinue = { role ->
                RolePref.set(context, role)
                authVm.setSelectedRole(role)
                nav.navigate(Routes.LOGIN)
            })
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                vm = authVm,
                onLoggedIn = { nav.navigate(Routes.PERMISSIONS) { popUpTo(Routes.LOGIN) { inclusive = true } } }
            )
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(onContinue = { nav.navigate(Routes.TERMS) })
        }
        composable(Routes.TERMS) {
            TermsScreen(onAgree = {
                val dest = if (auth.user?.role == "instructor") Routes.FACULTY_HOME else Routes.STUDENT_HOME
                nav.navigate(dest) { popUpTo(Routes.ROLE) { inclusive = true } }
            })
        }
        composable(Routes.BLUETOOTH_OFF) {
            BluetoothOffScreen(onAuto = { nav.popBackStack() }, onLater = { nav.popBackStack() })
        }

        // Student
        composable(Routes.STUDENT_HOME) {
            StudentHomeScreen(
                onLecture = { s -> nav.navigate("student/lecture/${s.id}") },
                onSessions = { nav.navigate(Routes.STUDENT_SESSIONS) },
                onProfile = { nav.navigate(Routes.STUDENT_PROFILE) }
            )
        }
        composable(Routes.STUDENT_LECTURE) { backStack ->
            val id = backStack.arguments?.getString("id") ?: return@composable
            LectureDetailScreen(
                sessionId = id,
                onBack = { nav.popBackStack() },
                onMarked = { sid -> nav.navigate("student/marked/$sid") { popUpTo(Routes.STUDENT_HOME) } },
                onBluetoothOff = { nav.navigate(Routes.BLUETOOTH_OFF) }
            )
        }
        composable(Routes.STUDENT_MARKED) { backStack ->
            val id = backStack.arguments?.getString("id") ?: return@composable
            MarkedConfirmationScreen(sessionId = id, onBackHome = {
                nav.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.STUDENT_HOME) { inclusive = true } }
            })
        }
        composable(Routes.STUDENT_SESSIONS) {
            StudentSessionsScreen(
                onSessionTap = { sid -> nav.navigate("student/session/$sid") },
                onHome = { nav.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.STUDENT_HOME) { inclusive = true } } },
                onProfile = { nav.navigate(Routes.STUDENT_PROFILE) }
            )
        }
        composable(Routes.STUDENT_SESSION_DETAIL) { backStack ->
            val id = backStack.arguments?.getString("id") ?: return@composable
            StudentSessionDetailScreen(sessionId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.STUDENT_PROFILE) {
            ProfileScreen(
                role = "student",
                onHome = { nav.navigate(Routes.STUDENT_HOME) { popUpTo(Routes.STUDENT_HOME) { inclusive = true } } },
                onSessions = { nav.navigate(Routes.STUDENT_SESSIONS) },
                onSignedOut = { nav.navigate(Routes.ROLE) { popUpTo(0) } }
            )
        }

        // Faculty
        composable(Routes.FACULTY_HOME) {
            FacultyHomeScreen(
                onStartSession = { s -> nav.navigate("instructor/cockpit/${s.id}") },
                onOpenCockpit = { s -> nav.navigate("instructor/cockpit/${s.id}") },
                onPrepare = { s -> nav.navigate("instructor/session/${s.id}") },
                onSessions = { nav.navigate(Routes.FACULTY_SESSIONS) },
                onProfile = { nav.navigate(Routes.FACULTY_PROFILE) },
                onBluetoothOff = { nav.navigate(Routes.BLUETOOTH_OFF) }
            )
        }
        composable(Routes.FACULTY_COCKPIT) { backStack ->
            val sid = backStack.arguments?.getString("sessionId") ?: return@composable
            FacultyCockpitScreen(
                sessionId = sid,
                onBack = { nav.popBackStack() },
                onEnded = { nav.navigate(Routes.FACULTY_HOME) { popUpTo(Routes.FACULTY_HOME) { inclusive = true } } },
                onBluetoothOff = { nav.navigate(Routes.BLUETOOTH_OFF) }
            )
        }
        composable(Routes.FACULTY_SESSIONS) {
            FacultySessionsScreen(
                onSessionTap = { sid -> nav.navigate("instructor/session/$sid") },
                onHome = { nav.navigate(Routes.FACULTY_HOME) { popUpTo(Routes.FACULTY_HOME) { inclusive = true } } },
                onProfile = { nav.navigate(Routes.FACULTY_PROFILE) }
            )
        }
        composable(Routes.FACULTY_SESSION_DETAIL) { backStack ->
            val id = backStack.arguments?.getString("id") ?: return@composable
            FacultySessionDetailScreen(sessionId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.FACULTY_PROFILE) {
            ProfileScreen(
                role = "instructor",
                onHome = { nav.navigate(Routes.FACULTY_HOME) { popUpTo(Routes.FACULTY_HOME) { inclusive = true } } },
                onSessions = { nav.navigate(Routes.FACULTY_SESSIONS) },
                onSignedOut = { nav.navigate(Routes.ROLE) { popUpTo(0) } }
            )
        }
    }
}
