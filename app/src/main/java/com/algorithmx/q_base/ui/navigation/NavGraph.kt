package com.algorithmx.q_base.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.algorithmx.q_base.ui.explore.*

sealed class Screen(val route: String) {
    object Categories : Screen("categories")

    // masterCategoryId = UUID of the master category
    object Collections : Screen("collections/{masterCategoryId}") {
        fun createRoute(masterCategoryId: String) = "collections/$masterCategoryId"
    }

    // collectionId = UUID, collectionTitle = raw title string (for subject queries)
    object Subjects : Screen("subjects/{collectionId}/{collectionTitle}") {
        fun createRoute(collectionId: String, collectionTitle: String) =
            "subjects/$collectionId/${collectionTitle.encodeForNav()}"
    }

    object Questions : Screen("questions/{collectionTitle}/{subject}") {
        fun createRoute(collectionTitle: String, subject: String) =
            "questions/${collectionTitle.encodeForNav()}/${subject.encodeForNav()}"
    }

    object QuestionDetail : Screen("question_detail/{questionId}") {
        fun createRoute(questionId: String) = "question_detail/$questionId"
    }
}

/** URL-encodes slashes in titles so they don't break Navigation routes. */
private fun String.encodeForNav() = this.replace("/", "%2F")

@Composable
fun ExploreNavGraph(
    navController: NavHostController,
    viewModel: ExploreViewModel
) {
    NavHost(navController = navController, startDestination = Screen.Categories.route) {

        // Level 1: Master Categories
        composable(Screen.Categories.route) {
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            CategoryListScreen(
                categories = categories,
                onCategoryClick = { masterCategoryId ->
                    viewModel.selectCategory(masterCategoryId)
                    navController.navigate(Screen.Collections.createRoute(masterCategoryId))
                }
            )
        }

        // Level 2: Collections in a Master Category (by UUID)
        composable(
            route = Screen.Collections.route,
            arguments = listOf(navArgument("masterCategoryId") { type = NavType.StringType })
        ) {
            val collections by viewModel.collections.collectAsStateWithLifecycle()
            CollectionListScreen(
                collections = collections,
                onCollectionClick = { collection ->
                    viewModel.selectCollection(collection.collectionId)
                    navController.navigate(
                        Screen.Subjects.createRoute(collection.collectionId, collection.title)
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Level 3: Subjects within a Collection
        composable(
            route = Screen.Subjects.route,
            arguments = listOf(
                navArgument("collectionId") { type = NavType.StringType },
                navArgument("collectionTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val collectionTitle = backStackEntry.arguments?.getString("collectionTitle")
                ?.replace("%2F", "/") ?: ""
            val subjects by viewModel.subjects.collectAsStateWithLifecycle()
            SubjectListScreen(
                category = collectionTitle,
                subjects = subjects,
                onSubjectClick = { subject ->
                    viewModel.selectSubject(collectionTitle, subject)
                    navController.navigate(Screen.Questions.createRoute(collectionTitle, subject))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Level 4: Questions in a Subject
        composable(
            route = Screen.Questions.route,
            arguments = listOf(
                navArgument("collectionTitle") { type = NavType.StringType },
                navArgument("subject") { type = NavType.StringType }
            )
        ) {
            val questions by viewModel.questions.collectAsStateWithLifecycle()
            QuestionListScreen(
                questions = questions,
                onQuestionClick = { questionId ->
                    viewModel.selectQuestion(questionId)
                    navController.navigate(Screen.QuestionDetail.createRoute(questionId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Level 5: Question Detail
        composable(
            route = Screen.QuestionDetail.route,
            arguments = listOf(navArgument("questionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
            val questions by viewModel.questions.collectAsStateWithLifecycle()
            val question = questions.find { it.questionId == questionId }
            val options by viewModel.currentOptions.collectAsStateWithLifecycle()
            val answer by viewModel.currentAnswer.collectAsStateWithLifecycle()

            if (question != null) {
                QuestionDetailScreen(
                    question = question,
                    options = options,
                    explanation = answer?.generalExplanation,
                    correctAnswer = answer?.correctAnswerString,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

