package com.example.masterenglishfluency.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.masterenglishfluency.AppViewModel
import com.example.masterenglishfluency.ui.components.AppCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    var profileName by remember { mutableStateOf(state.settings.profileName) }

    LaunchedEffect(state.settings.profileName) {
        if (profileName != state.settings.profileName) {
            profileName = state.settings.profileName
        }
    }

    LaunchedEffect(profileName) {
        delay(300)
        viewModel.setProfileName(profileName)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Settings") })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ProfileSettingsCard(
                    profileName = profileName,
                    onProfileNameChange = { profileName = it }
                )
            }
            item {
                ToggleCard(
                    title = "Dark Mode",
                    description = "Use a darker interface for low-light study sessions.",
                    checked = state.settings.darkModeEnabled,
                    onCheckedChange = viewModel::setDarkMode
                )
            }
            item {
                ToggleCard(
                    title = "Notifications",
                    description = "Receive reminders to complete your daily English challenge.",
                    checked = state.settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotifications
                )
            }
            item {
                AppCard(title = "App Information") {
                    Text("Master English Fluency")
                    Text("Version 1.0")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { profileName = "Learner" }, modifier = Modifier.fillMaxWidth()) {
                        Text("Reset Profile Name")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSettingsCard(
    profileName: String,
    onProfileNameChange: (String) -> Unit
) {
    AppCard(title = "Profile Settings") {
        OutlinedTextField(
            value = profileName,
            onValueChange = onProfileNameChange,
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
