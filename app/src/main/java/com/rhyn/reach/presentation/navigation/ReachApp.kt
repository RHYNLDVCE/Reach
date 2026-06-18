package com.rhyn.reach.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rhyn.reach.presentation.feature.auth.AuthScreen
import com.rhyn.reach.presentation.feature.auth.AuthViewModel
import com.rhyn.reach.presentation.feature.account.AccountViewModel
import com.rhyn.reach.presentation.feature.chat.ChatScreen
import com.rhyn.reach.presentation.feature.chat.ChatViewModel
import com.rhyn.reach.presentation.feature.chat.CreateGroupScreen
import com.rhyn.reach.core.utils.SettingsManager
import com.rhyn.reach.presentation.feature.chat.SearchScreen
import com.rhyn.reach.presentation.feature.home.MainScreen
import com.rhyn.reach.presentation.feature.onboarding.OnboardingScreen
import com.rhyn.reach.presentation.feature.settings.AboutScreen
import com.rhyn.reach.presentation.feature.settings.SettingsScreen

@Composable
fun ReachApp(
    navController: NavHostController = rememberNavController(),
    // Inject the separated ViewModels here
    authViewModel: AuthViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    accountViewModel: AccountViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    val startDestination = remember {
        if (!SettingsManager.hasSeenOnboarding(context)) {
            "onboarding"
        } else if (authViewModel.isUserLoggedIn()) {
            "main"
        } else {
            "auth"
        }
    }

    LaunchedEffect(Unit) {
        chatViewModel.initializeApp(context)
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        chatViewModel.initializeApp(context)
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
        }
    ) {

        composable("onboarding") {
            OnboardingScreen(
                onFinish = {
                    SettingsManager.setHasSeenOnboarding(context, true)
                    val nextScreen = if (authViewModel.isUserLoggedIn()) "main" else "auth"
                    navController.navigate(nextScreen) {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }

        composable("auth") {
            AuthScreen(
                viewModel = authViewModel, // 2. Pass AuthViewModel here
                onAuthSuccess = {
                    // Initialize the mesh service immediately after successful login
                    chatViewModel.initializeApp(context)

                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                viewModel = chatViewModel, // ChatViewModel stays for the main inbox
                onNavigateToChat = { targetUserId ->
                    navController.navigate("chat/$targetUserId")
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                },
                onNavigateToCreateGroup = {
                    navController.navigate("create_group")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = chatViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { targetUserId ->
                    navController.popBackStack()
                    navController.navigate("chat/$targetUserId")
                }
            )
        }

        composable("create_group") {
            CreateGroupScreen(
                viewModel = chatViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGroupCreated = {
                    navController.popBackStack()
                }
            )
        }

        composable("chat/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable

            ChatScreen(
                viewModel = chatViewModel,
                targetUserId = userId,
                onNavigateBack = {
                    chatViewModel.clearChatPartner()
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAbout = {
                    navController.navigate("about")
                },
                onLogoutClick = {
                    accountViewModel.logout(
                        context = context,
                        onComplete = {
                            navController.navigate("auth") {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }
            )
        }

        composable("about") {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}