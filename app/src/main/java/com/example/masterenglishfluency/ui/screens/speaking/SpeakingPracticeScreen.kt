package com.example.masterenglishfluency.ui.screens.speaking

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.masterenglishfluency.AppViewModel
import com.example.masterenglishfluency.RecordingState
import com.example.masterenglishfluency.data.model.PracticeSession
import com.example.masterenglishfluency.data.repository.SampleData
import com.example.masterenglishfluency.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakingPracticeScreen(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val prompt = SampleData.practicePrompts[state.selectedPracticePromptIndex]
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionDenied = !isGranted
        if (isGranted) {
            viewModel.startRecording(context)
        }
    }

    LaunchedEffect(permissionDenied) {
        if (permissionDenied) {
            snackbarHostState.showSnackbar("Microphone permission is required to record practice.")
            permissionDenied = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text(text = "Speaking Practice") })
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
                PracticePromptCard(
                    prompt = prompt,
                    promptIndex = state.selectedPracticePromptIndex,
                    totalPrompts = SampleData.practicePrompts.size,
                    onPrevious = { viewModel.selectPracticePrompt((state.selectedPracticePromptIndex - 1).coerceAtLeast(0)) },
                    onNext = { viewModel.selectPracticePrompt((state.selectedPracticePromptIndex + 1).coerceAtMost(SampleData.practicePrompts.lastIndex)) }
                )
            }
            item {
                RecorderCard(
                    recordingState = recordingState,
                    onRecord = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.startRecording(context)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStop = { viewModel.stopRecording() },
                    onPlay = { viewModel.playRecording(context) },
                    onSave = { viewModel.saveRecording(context) }
                )
            }
            item {
                RecentSessionsCard(sessions = state.recentPracticeSessions)
            }
        }
    }
}

@Composable
private fun PracticePromptCard(
    prompt: String,
    promptIndex: Int,
    totalPrompts: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Prompt ${promptIndex + 1} of $totalPrompts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = prompt, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrevious, enabled = promptIndex > 0) { Text("Previous") }
                Button(onClick = onNext, enabled = promptIndex < totalPrompts - 1) { Text("Next") }
            }
        }
    }
}

@Composable
private fun RecorderCard(
    recordingState: RecordingState,
    onRecord: () -> Unit,
    onStop: () -> Unit,
    onPlay: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (recordingState.isRecording) {
                CircularProgressIndicator()
                Text(
                    text = formatDuration(recordingState.elapsedSeconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Ready to speak",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recordingState.isRecording) {
                    FilledIconButton(
                        onClick = onStop,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Stop recording", modifier = Modifier.size(28.dp))
                    }
                } else {
                    FilledIconButton(
                        onClick = onRecord,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = "Record", modifier = Modifier.size(36.dp))
                    }
                }
                FilledIconButton(
                    onClick = onPlay,
                    enabled = recordingState.currentFilePath != null && !recordingState.isRecording
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play recording")
                }
                FilledIconButton(
                    onClick = onSave,
                    enabled = recordingState.currentFilePath != null && !recordingState.isRecording
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save recording")
                }
                FilledIconButton(
                    onClick = onPlay,
                    enabled = recordingState.currentFilePath != null && !recordingState.isRecording
                ) {
                    Icon(imageVector = Icons.Default.Audiotrack, contentDescription = "Recording file")
                }
            }
            if (recordingState.message.isNotBlank()) {
                Text(text = recordingState.message, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun RecentSessionsCard(sessions: List<PracticeSession>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Saved Practice Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (sessions.isEmpty()) {
                Text("No sessions saved yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                sessions.forEach { session ->
                    SessionItem(session)
                }
            }
        }
    }
}

@Composable
private fun SessionItem(session: PracticeSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = session.prompt, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Duration: ${formatDuration(session.durationSeconds)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
