package com.hackx.ruraledtech.feature.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hackx.ruraledtech.feature.accessibility.AccessibilitySettingsScreen
import com.hackx.ruraledtech.feature.contentlibrary.ContentLibraryScreen
import com.hackx.ruraledtech.feature.home.HomeScreen
import com.hackx.ruraledtech.feature.learner.LearnerSelectionScreen
import com.hackx.ruraledtech.feature.lessons.LessonListScreen
import com.hackx.ruraledtech.feature.lessons.LessonViewerScreen
import com.hackx.ruraledtech.feature.lessons.SubjectListScreen
import com.hackx.ruraledtech.feature.onboarding.AddLearnerScreen
import com.hackx.ruraledtech.feature.onboarding.LanguageSelectionScreen
import com.hackx.ruraledtech.feature.onboarding.RoleSelectionScreen
import com.hackx.ruraledtech.feature.onboarding.UserRole
import com.hackx.ruraledtech.feature.passport.PassportScreen
import com.hackx.ruraledtech.feature.profile.ProfileScreen
import com.hackx.ruraledtech.feature.progress.ProgressScreen
import com.hackx.ruraledtech.feature.quiz.QuizScreen
import com.hackx.ruraledtech.feature.splash.SplashScreen
import com.hackx.ruraledtech.feature.teacher.TeacherHomeScreen
import com.hackx.ruraledtech.feature.teacher.TeacherLoginScreen

/**
 * The full learner journey (PS section 24/56/57) lives in this single graph. Nothing here
 * ever routes through a network call — every destination is reachable with the device in
 * airplane mode, which is the acceptance test in PS section 46.
 */
@Composable
fun RuralEdTechNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                onDestinationReady = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(
                onNavigate = { destination ->
                    navController.navigate(destination) { popUpTo(Routes.ROLE_SELECTION) { inclusive = true } }
                },
            )
        }

        composable(Routes.TEACHER_LOGIN) {
            TeacherLoginScreen(
                onLoginSuccess = { navController.navigate(Routes.TEACHER_HOME) { popUpTo(Routes.TEACHER_LOGIN) { inclusive = true } } },
            )
        }

        composable(Routes.TEACHER_HOME) {
            TeacherHomeScreen(
                onSwitchToStudent = { navController.navigate(Routes.ROLE_SELECTION) { popUpTo(Routes.TEACHER_HOME) { inclusive = true } } },
                onOpenContentLibrary = { navController.navigate(Routes.CONTENT_LIBRARY) },
                onLoggedOut = { navController.navigate(Routes.ROLE_SELECTION) { popUpTo(Routes.TEACHER_HOME) { inclusive = true } } },
            )
        }

        composable(Routes.LANGUAGE_SELECTION) {
            LanguageSelectionScreen(onLanguageSelected = { navController.navigate(Routes.ADD_LEARNER) })
        }

        composable(Routes.ADD_LEARNER) {
            AddLearnerScreen(
                onLearnerCreated = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LANGUAGE_SELECTION) { inclusive = true } }
                },
            )
        }

        composable(Routes.LEARNER_SELECTION) {
            LearnerSelectionScreen(
                onLearnerSelected = { navController.navigate(Routes.HOME) { popUpTo(Routes.LEARNER_SELECTION) { inclusive = true } } },
                onAddLearner = { navController.navigate(Routes.ADD_LEARNER) },
                onSwitchRole = { navController.navigate(Routes.ROLE_SELECTION) { popUpTo(Routes.LEARNER_SELECTION) { inclusive = true } } },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onOpenSubjects = { navController.navigate(Routes.SUBJECT_LIST) },
                onOpenSubject = { subject -> navController.navigate(Routes.lessonList(subject)) },
                onOpenProgress = { navController.navigate(Routes.PROGRESS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenContentLibrary = { navController.navigate(Routes.CONTENT_LIBRARY) },
                onAddLearner = { navController.navigate(Routes.ADD_LEARNER) },
                onOpenLesson = { lessonId -> navController.navigate(Routes.lessonViewer(lessonId)) },
            )
        }

        composable(Routes.SUBJECT_LIST) {
            SubjectListScreen(onSubjectClick = { subject -> navController.navigate(Routes.lessonList(subject)) })
        }

        composable(
            route = Routes.LESSON_LIST,
            arguments = listOf(navArgument(Routes.ARG_SUBJECT) { type = NavType.StringType }),
        ) {
            LessonListScreen(onLessonClick = { lessonId -> navController.navigate(Routes.lessonViewer(lessonId)) })
        }

        composable(
            route = Routes.LESSON_VIEWER,
            arguments = listOf(navArgument(Routes.ARG_LESSON_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val lessonId = checkNotNull(backStackEntry.arguments?.getString(Routes.ARG_LESSON_ID))
            LessonViewerScreen(onTakeQuiz = { navController.navigate(Routes.quiz(lessonId)) })
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(navArgument(Routes.ARG_LESSON_ID) { type = NavType.StringType }),
        ) {
            QuizScreen(onQuizComplete = { navController.popBackStack(Routes.HOME, inclusive = false) })
        }

        composable(Routes.PROGRESS) { ProgressScreen() }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onSwitchedProfile = { navController.navigate(Routes.LEARNER_SELECTION) { popUpTo(Routes.HOME) { inclusive = true } } },
                onLogoutToRoleSelection = { navController.navigate(Routes.ROLE_SELECTION) { popUpTo(Routes.HOME) { inclusive = true } } },
                onOpenAccessibility = { navController.navigate(Routes.ACCESSIBILITY_SETTINGS) },
                onOpenContentLibrary = { navController.navigate(Routes.CONTENT_LIBRARY) },
                onOpenPassport = { navController.navigate(Routes.PASSPORT) },
            )
        }

        composable(Routes.CONTENT_LIBRARY) {
            ContentLibraryScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PASSPORT) { PassportScreen() }

        composable(Routes.ACCESSIBILITY_SETTINGS) { AccessibilitySettingsScreen() }
    }
}
