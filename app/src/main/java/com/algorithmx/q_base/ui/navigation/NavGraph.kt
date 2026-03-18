package com.algorithmx.q_base.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.algorithmx.q_base.ui.explore.*

sealed class Screen(val route: String) {
    object Categories : Screen("categories")
    object Collections : Screen("collections/{masterCategory}") {
        fun createRoute(masterCategory: String) = "collections/$masterCategory"
    }
    object Questions : Screen("questions/{category}") {
        fun createRoute(category: String) = "questions/$category"
    }
    object QuestionDetail : Screen("question_detail/{questionId}") {
        fun createRoute(questionId: String) = "question_detail/$questionId"
    }
}

@Composable
fun ExploreNavGraph(
    navController: NavHostController,
    viewModel: ExploreViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Categories.route
    ) {
        composable(Screen.Categories.route) {
            val categories by viewModel.categories.collectAsState()
            CategoryListScreen(
                categories = categories,
                onCategoryClick = { masterCategory ->
                    viewModel.selectCategory(masterCategory)
                    navController.navigate(Screen.Collections.createRoute(masterCategory))
                }
            )
        }
        
        composable(
            route = Screen.Collections.route,
            arguments = listOf(navArgument("masterCategory") { type = NavType.StringType })
        ) {
            val collections by viewModel.collections.collectAsState()
            CollectionListScreen(
                collections = collections,
                onCollectionClick = { category ->
                    viewModel.selectCollection(category)
                    navController.navigate(Screen.Questions.createRoute(category))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.Questions.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) {
            val questions by viewModel.questions.collectAsState()
            QuestionListScreen(
                questions = questions,
                onQuestionClick = { questionId ->
                    viewModel.selectQuestion(questionId)
                    navController.navigate(Screen.QuestionDetail.createRoute(questionId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.QuestionDetail.route,
            arguments = listOf(navArgument("questionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val questionId = backStackEntry.arguments?.getString("questionId") ?: ""
            val questions by viewModel.questions.collectAsState()
            val question = questions.find { it.questionId == questionId }
            val options by viewModel.currentOptions.collectAsState()
            val answer by viewModel.currentAnswer.collectAsState()
            
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
