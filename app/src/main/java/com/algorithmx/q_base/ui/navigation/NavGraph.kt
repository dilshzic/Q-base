package com.algorithmx.q_base.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.algorithmx.q_base.ui.explore.*

sealed class Screen(val route: String) {
    object Categories : Screen("categories")

    // categoryName = Name of the master category
    object ExplorePager : Screen("explore_pager/{categoryName}") {
        fun createRoute(categoryName: String) = "explore_pager/${categoryName.encodeForNav()}"
    }
}

/** URL-encodes slashes in titles so they don't break Navigation routes. */
private fun String.encodeForNav() = this.replace("/", "%2F")

@Composable
fun ExploreNavGraph(
    navController: NavHostController,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    NavHost(navController = navController, startDestination = Screen.Categories.route) {

        // Level 1: Master Categories
        composable(Screen.Categories.route) {
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            CategoryListScreen(
                categories = categories,
                onCategoryClick = { categoryName ->
                    navController.navigate(Screen.ExplorePager.createRoute(categoryName))
                }
            )
        }

        // Level 2: Horizontal Pager for Questions
        composable(
            route = Screen.ExplorePager.route,
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName")
                ?.replace("%2F", "/") ?: ""
            
            val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
            
            LaunchedEffect(categoryName) {
                viewModel.loadQuestionsByMasterCategory(categoryName)
            }

            ExploreQuestionPagerScreen(
                categoryName = categoryName,
                questionStates = questionStates,
                onOptionSelected = { index, option ->
                    viewModel.selectOption(index, option)
                },
                onCheckAnswer = { index ->
                    viewModel.revealAnswer(index)
                },
                onPageChanged = { index ->
                    viewModel.loadQuestionDetails(index)
                    // Optionally pre-load next page
                    viewModel.loadQuestionDetails(index + 1)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
