package com.example.masterenglishfluency.ui.dashboard

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.masterenglishfluency.ui.components.AppIcons

@Composable
fun VocabularyTab(
    viewModel: DashboardViewModel
) {
    val context = LocalContext.current
    val words by viewModel.vocabularyWords.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val wordOfDay by viewModel.wordOfDay.collectAsState()

    var showOnlyBookmarks by remember { mutableStateOf(false) }
    var isPaywallVisible by remember { mutableStateOf(false) }

    // Setup TextToSpeech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.US
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    val filteredWords = if (showOnlyBookmarks) {
        words.filter { it.isBookmarked }
    } else {
        words
    }

    val bookmarkedCount = words.count { it.isBookmarked }
    val progressFraction = if (words.isEmpty()) 0f else bookmarkedCount.toFloat() / words.size.toFloat()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F8FA))
            .padding(24.dp)
    ) {
        // Title & Filter Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vocabulary",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Box(
                modifier = Modifier
                    .background(
                        color = if (showOnlyBookmarks) Color(0x222F80ED) else Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (showOnlyBookmarks) Color(0xFF2F80ED) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { showOnlyBookmarks = !showOnlyBookmarks }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Bookmarks Only",
                    color = if (showOnlyBookmarks) Color(0xFF2F80ED) else Color(0xFF2C3E50),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Daily Progress card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Daily Progress", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        color = Color(0xFF2F80ED),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF2F80ED),
                    trackColor = Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You've bookmarked $bookmarkedCount of ${words.size} words",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Word of the day card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2F80ED)),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WORD OF THE DAY",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = wordOfDay?.word ?: "Luminous",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = wordOfDay?.let { "/'${it.word.take(2)}mɪnəs/" } ?: "/'lu:mɪnəs/",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable {
                            wordOfDay?.let { tts?.speak(it.word, TextToSpeech.QUEUE_FLUSH, null, null) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.INSTANCE.Volume,
                        contentDescription = "Speak word",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Word list
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredWords) { word ->
                VocabItemRow(
                    word = word,
                    onVolumeClick = {
                        tts?.speak(word.word, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    onBookmarkClick = {
                        if (word.id > 5 && !isPremiumUser) {
                            isPaywallVisible = true
                        } else {
                            viewModel.toggleBookmark(word.id)
                        }
                    }
                )
            }
        }
    }

    if (isPaywallVisible) {
        PremiumPaywallSheet(
            onDismiss = { isPaywallVisible = false },
            onPurchaseSuccess = {
                viewModel.upgradeToPremium()
                isPaywallVisible = false
                Toast.makeText(context, "Successfully upgraded to Premium!", Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
fun VocabItemRow(
    word: VocabWord,
    onVolumeClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${word.type})",
                        fontSize = 12.sp,
                        color = Color(0xFF7F8C8D),
                        fontStyle = FontStyle.Italic
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.definition,
                    fontSize = 13.sp,
                    color = Color(0xFF7F8C8D)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolumeClick) {
                    Icon(
                        imageVector = AppIcons.INSTANCE.Volume,
                        contentDescription = "Speak",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        imageVector = if (word.isBookmarked) AppIcons.INSTANCE.Bookmark else AppIcons.INSTANCE.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (word.isBookmarked) Color(0xFF2D9CDB) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumPaywallSheet(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Unlock Vocabulary Pro",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Upgrade to unlock 100+ advanced words and expressions built for job interviews, academic papers, and public speeches.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            PaywallFeatureItem(text = "Full advanced vocabulary lists")
            Spacer(modifier = Modifier.height(12.dp))
            PaywallFeatureItem(text = "Detailed grammatical insights and definitions")
            Spacer(modifier = Modifier.height(12.dp))
            PaywallFeatureItem(text = "Custom bookmarks and bookmarks backup")
            Spacer(modifier = Modifier.height(12.dp))
            PaywallFeatureItem(text = "Priority live audio pronunciation tools")

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onPurchaseSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F80ED))
            ) {
                Text(
                    text = "Upgrade for $4.99/mo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Maybe Later", color = Color.Gray)
            }
        }
    }
}

@Composable
fun PaywallFeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = AppIcons.INSTANCE.Star,
            contentDescription = null,
            tint = Color(0xFFF2C94C),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 14.sp, color = Color(0xFF2C3E50))
    }
}
