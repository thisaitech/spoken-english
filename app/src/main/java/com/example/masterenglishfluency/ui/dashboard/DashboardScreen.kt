package com.example.masterenglishfluency.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.masterenglishfluency.ui.chat.ChatScreen
import com.example.masterenglishfluency.ui.chat.ChatViewModel
import com.example.masterenglishfluency.ui.components.AppIcons

@Composable
fun DashboardScreen(
    onNavigateToSpeakingPractice: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var activeTab by remember { mutableStateOf(0) }
    val chatViewModel: ChatViewModel = viewModel()

    Scaffold(
        bottomBar = {
            CustomBottomBar(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> HomeTab(
                    viewModel = viewModel,
                    onNavigateToSpeaking = onNavigateToSpeakingPractice,
                    onNavigateToVocab = { activeTab = 1 },
                    onNavigateToPerformance = { activeTab = 2 },
                    onLogout = onLogout
                )
                1 -> VocabularyTab(viewModel = viewModel)
                2 -> PerformanceTab(viewModel = viewModel, chatViewModel = chatViewModel)
                3 -> ChatScreen(onBack = { activeTab = 0 }, viewModel = chatViewModel, speechRecognizer = null, tts = null)
            }
        }
    }
}

@Composable
fun CustomBottomBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                icon = AppIcons.INSTANCE.HomeTab,
                label = "Home",
                isSelected = activeTab == 0,
                onClick = { onTabSelected(0) }
            )
            BottomNavItem(
                icon = AppIcons.INSTANCE.BookTab,
                label = "Vocabulary",
                isSelected = activeTab == 1,
                onClick = { onTabSelected(1) }
            )
            BottomNavItem(
                icon = AppIcons.INSTANCE.ChartTab,
                label = "Performance",
                isSelected = activeTab == 2,
                onClick = { onTabSelected(2) }
            )
            BottomNavItem(
                icon = AppIcons.INSTANCE.Chat,
                label = "Chat",
                isSelected = activeTab == 3,
                onClick = { onTabSelected(3) }
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Color(0xFF2F80ED)
    val inactiveColor = Color.Gray

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(22.dp),
            tint = if (isSelected) activeColor else inactiveColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
