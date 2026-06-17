package com.example.masterenglishfluency.ui.dashboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.masterenglishfluency.ui.components.AppIcons
import java.io.File
import java.io.FileOutputStream


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    viewModel: DashboardViewModel,
    onNavigateToSpeaking: () -> Unit,
    onNavigateToVocab: () -> Unit,
    onNavigateToPerformance: () -> Unit,
    onLogout: () -> Unit
) {
    val streakDays by viewModel.streakDays.collectAsState()
    val practiceSeconds by viewModel.practiceSeconds.collectAsState()
    val selectedGoal by viewModel.selectedGoal.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()

    val userName by viewModel.userName.collectAsState()
    val profilePicPath by viewModel.profilePicPath.collectAsState()

    val profileBitmap = remember(profilePicPath) {
        if (profilePicPath.isNotEmpty() && File(profilePicPath).exists()) {
            try {
                BitmapFactory.decodeFile(profilePicPath)
            } catch (e: Exception) {
                Log.e("HomeTab", "Failed to decode profile pic file", e)
                null
            }
        } else {
            null
        }
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showCoreModulesSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val context = LocalContext.current
    var cropImageUri by remember { mutableStateOf<Uri?>(null) }
    var cropImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            cropImageUri = uri
            val bitmap = getBitmapFromUri(context, uri)
            if (bitmap != null) {
                cropImageBitmap = bitmap
                showCropDialog = true
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            cropImageBitmap = bitmap
            cropImageUri = null
            showCropDialog = true
        }
    }

    val practiceMinutes = practiceSeconds / 60
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hi, ${userName.split(" ").firstOrNull() ?: userName}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = "Ready to sharpen your fluency?",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPremiumUser) {
                    Box(
                        modifier = Modifier
                            .background(Color(0x222F80ED), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF2F80ED), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PRO",
                            color = Color(0xFF2F80ED),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2F80ED))
                        .clickable { showProfileDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initial = userName.firstOrNull()?.toString()?.uppercase() ?: "A"
                        Text(
                            text = initial,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Streak",
                value = "$streakDays days",
                icon = AppIcons.INSTANCE.Flame,
                iconTint = Color(0xFFE28743)
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Practice Time",
                value = "$practiceMinutes min",
                icon = AppIcons.INSTANCE.Clock,
                iconTint = Color(0xFF2F80ED)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Learning Goal selection
        Text(
            text = "Learning Goal",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val goals = listOf("Casual Chat", "Job Interview", "Public Speaking", "Academic")
            goals.take(2).forEach { goal ->
                GoalChip(
                    label = goal,
                    isSelected = selectedGoal == goal,
                    onClick = {
                        viewModel.selectGoal(goal)
                        viewModel.selectSpeakingTopic(goal, 0)
                        onNavigateToSpeaking()
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val goals = listOf("Casual Chat", "Job Interview", "Public Speaking", "Academic")
            goals.drop(2).forEach { goal ->
                GoalChip(
                    label = goal,
                    isSelected = selectedGoal == goal,
                    onClick = {
                        viewModel.selectGoal(goal)
                        viewModel.selectSpeakingTopic(goal, 0)
                        onNavigateToSpeaking()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Speaking Challenges
        Text(
            text = "Speaking Challenges",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C3E50)
        )
        Spacer(modifier = Modifier.height(12.dp))

        ModuleRowCard(
            title = "Start Speaking Challenge",
            subtitle = "Practice real-time speech and get graded instantly",
            icon = AppIcons.INSTANCE.Mic,
            iconBg = Color(0xFF2F80ED),
            onClick = onNavigateToSpeaking
        )

        Spacer(modifier = Modifier.height(16.dp))

        ModuleRowCard(
            title = "Core Modules",
            subtitle = "Explore structured conversations and lessons",
            icon = AppIcons.INSTANCE.BookTab,
            iconBg = Color(0xFF27AE60),
            onClick = { showCoreModulesSheet = true }
        )
    }

    // Modal bottom sheet for core modules
    if (showCoreModulesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCoreModulesSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Core Modules",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val coreModules = listOf(
                    "Introductory Conversation" to "Learn basic greetings and introductions",
                    "Ordering Food" to "Practice conversations in restaurants and cafes",
                    "Asking for Directions" to "Understand how to navigate cities and places",
                    "Sharing Opinions" to "Discuss general topics and express viewpoints",
                    "Job Interview Preparation" to "Formal greetings and talking about strengths/weaknesses",
                    "Professional Networking" to "Conversations with colleagues and updating meetings",
                    "Shopping & Bargaining" to "Asking for prices and checking for discounts",
                    "Socializing & Making Friends" to "Casual small talk at an event and making plans",
                    "Emergency Situations" to "Asking for help and explaining issues to authorities"
                )
                coreModules.forEachIndexed { index, (title, desc) ->
                    ModuleDetailsItem(
                        title = title,
                        desc = desc,
                        onClick = {
                            viewModel.selectSpeakingTopic("Core Modules", index)
                            showCoreModulesSheet = false
                            onNavigateToSpeaking()
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showCoreModulesSheet = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                ) {
                    Text("Close")
                }
            }
        }
    }

    // Profile Dialog
    if (showProfileDialog) {
        var tempName by remember(userName) { mutableStateOf(userName) }
        
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = "My Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Profile Image with Camera badge
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2F80ED))
                            .clickable { showPhotoOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap.asImageBitmap(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val initial = userName.firstOrNull()?.toString()?.uppercase() ?: "A"
                            Text(
                                text = initial,
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Edit icon overlay
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                .align(Alignment.BottomEnd)
                                .clickable { showPhotoOptions = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.INSTANCE.Camera,
                                contentDescription = "Edit photo",
                                tint = Color(0xFF2F80ED),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    TextButton(
                        onClick = { showPhotoOptions = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Change Picture", fontSize = 12.sp, color = Color(0xFF2F80ED))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Name Edit Option
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.INSTANCE.Edit,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Stats Summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Learning Goal:", fontSize = 12.sp, color = Color.Gray)
                                Text(selectedGoal, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Weekly Streak:", fontSize = 12.sp, color = Color.Gray)
                                Text("$streakDays Days", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE28743))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Practice:", fontSize = 12.sp, color = Color.Gray)
                                Text("$practiceMinutes Mins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2F80ED))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Membership:", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    text = if (isPremiumUser) "Premium Pro" else "Free Tier",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPremiumUser) Color(0xFF27AE60) else Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.updateUserName(tempName)
                        }
                        showProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showProfileDialog = false
                        onLogout()
                    }) {
                        Text("Log Out", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { showProfileDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        )
    }

    // Photo Source Picker Options Dialog
    if (showPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoOptions = false },
            title = {
                Text(
                    text = "Update Profile Photo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoOptions = false
                                cameraLauncher.launch(null)
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.INSTANCE.Camera,
                            contentDescription = "Camera",
                            tint = Color(0xFF2F80ED),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Take Photo (Camera)")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showPhotoOptions = false
                                galleryLauncher.launch("image/*")
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = AppIcons.INSTANCE.Photo,
                            contentDescription = "Gallery",
                            tint = Color(0xFF27AE60),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Choose from Gallery")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptions = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Crop Dialog Overlay
    if (showCropDialog && cropImageBitmap != null) {
        ProfileCropDialog(
            bitmap = cropImageBitmap!!,
            onCropComplete = { cropped ->
                val path = saveBitmapToFile(context, cropped)
                if (path != null) {
                    viewModel.updateProfilePic(path)
                }
                showCropDialog = false
            },
            onCancel = {
                showCropDialog = false
            }
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = title, fontSize = 12.sp, color = Color.Gray)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        }
    }
}

@Composable
fun GoalChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) Color(0xFF2F80ED) else Color.White,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF2F80ED) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFF2C3E50),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ModuleRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = AppIcons.INSTANCE.ArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ModuleDetailsItem(title: String, desc: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2C3E50))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = desc, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ProfileCropDialog(
    bitmap: Bitmap,
    onCropComplete: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = "Crop Profile Picture",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Drag to reposition. Use slider to zoom.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Square Crop Container (260.dp x 260.dp)
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .background(Color.LightGray, RoundedCornerShape(8.dp))
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Original Image
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(scale)
                            .offset { IntOffset(offsetX.toInt(), offsetY.toInt()) }
                    )

                    // Semi-transparent overlay with circular transparent hole in the middle
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    ) {
                        drawRect(color = Color.Black.copy(alpha = 0.5f))
                        drawCircle(
                            color = Color.Transparent,
                            radius = size.minDimension * 0.4f,
                            blendMode = BlendMode.Clear
                        )
                    }

                    // Thin border for the circular crop area
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color.White,
                            radius = size.minDimension * 0.4f,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Zoom Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = AppIcons.INSTANCE.BookmarkBorder,
                        contentDescription = "Zoom out",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..3f,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )
                    Icon(
                        imageVector = AppIcons.INSTANCE.Bookmark,
                        contentDescription = "Zoom in",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val containerSizePx = 260f * 3f
                    val cropped = cropBitmap(bitmap, scale, offsetX, offsetY, containerSizePx)
                    onCropComplete(cropped)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text("Crop & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

// Helpers for loading, saving, and cropping profile pictures
fun getBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true)
        }
    } catch (e: Exception) {
        Log.e("HomeTab", "Failed to load bitmap from uri: $uri", e)
        null
    }
}

fun saveBitmapToFile(context: android.content.Context, bitmap: Bitmap): String? {
    return try {
        val file = File(context.filesDir, "cropped_profile_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        Log.e("HomeTab", "Failed to save cropped bitmap", e)
        null
    }
}

fun cropBitmap(bitmap: Bitmap, scale: Float, offsetX: Float, offsetY: Float, containerSizePx: Float): Bitmap {
    val bmpWidth = bitmap.width.toFloat()
    val bmpHeight = bitmap.height.toFloat()
    
    val containerRatio = 1.0f
    val bmpRatio = bmpWidth / bmpHeight
    
    val baseScale = if (bmpRatio > containerRatio) {
        containerSizePx / bmpHeight
    } else {
        containerSizePx / bmpWidth
    }
    
    val cropWindowSize = containerSizePx * 0.8f
    val finalScale = baseScale * scale
    
    val origCropSize = (cropWindowSize / finalScale).coerceAtMost(Math.min(bmpWidth, bmpHeight))
    
    val screenCenterXInImage = (bmpWidth / 2) - (offsetX / finalScale)
    val screenCenterYInImage = (bmpHeight / 2) - (offsetY / finalScale)
    
    val left = (screenCenterXInImage - origCropSize / 2).toInt().coerceIn(0, (bmpWidth - origCropSize).toInt())
    val top = (screenCenterYInImage - origCropSize / 2).toInt().coerceIn(0, (bmpHeight - origCropSize).toInt())
    val size = origCropSize.toInt().coerceIn(100, Math.min(bmpWidth, bmpHeight).toInt())
    
    return Bitmap.createBitmap(bitmap, left, top, size, size)
}

