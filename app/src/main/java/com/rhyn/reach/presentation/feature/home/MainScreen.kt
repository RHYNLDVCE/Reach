package com.rhyn.reach.presentation.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.rhyn.reach.presentation.feature.account.AccountScreen
import com.rhyn.reach.presentation.feature.chat.ChatViewModel
import com.rhyn.reach.presentation.feature.chat.InboxScreen
import com.rhyn.reach.presentation.feature.nearby.NearbyScreen

enum class HomeTab(val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    NEARBY("Nearby", Icons.Default.Explore, Icons.Outlined.Explore),
    INBOX("Inbox", Icons.Default.ChatBubble, Icons.Default.ChatBubbleOutline),
    ACCOUNT("Account", Icons.Default.Person, Icons.Default.PersonOutline)
}

@OptIn(ExperimentalMaterial3Api::class) // Required for BadgedBox in some Compose versions
@Composable
fun MainScreen(
    viewModel: ChatViewModel,
    onNavigateToChat: (String) -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(HomeTab.INBOX) }

    // ---> NEW: Collect the unread count from the ViewModel <---
    val unreadCount by viewModel.unreadCount.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        // ---> NEW: Check for unread messages and show a badge <---
                        icon = {
                            val currentIcon = if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon
                            if (tab == HomeTab.INBOX && unreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(text = unreadCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(currentIcon, contentDescription = tab.title)
                                }
                            } else {
                                Icon(currentIcon, contentDescription = tab.title)
                            }
                        },
                        label = { Text(tab.title) },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->

        val tabModifier = Modifier
            .fillMaxSize()
            .padding(bottom = innerPadding.calculateBottomPadding())

        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = 300),
            label = "tab_crossfade"
        ) { currentTab ->
            when (currentTab) {
                HomeTab.NEARBY -> Box(modifier = tabModifier) {
                    NearbyScreen(onNavigateToChat = onNavigateToChat)
                }

                HomeTab.INBOX -> InboxScreen(
                    viewModel = viewModel,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToSearch = onNavigateToSearch,
                    onNavigateToCreateGroup = onNavigateToCreateGroup,
                    modifier = tabModifier
                )
                HomeTab.ACCOUNT -> Box(modifier = tabModifier) {
                    AccountScreen(
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        }
    }
}