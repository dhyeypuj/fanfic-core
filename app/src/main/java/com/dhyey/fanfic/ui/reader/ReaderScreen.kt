package com.dhyey.fanfic.ui.reader

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.dhyey.fanfic.data.ReaderSettings
import com.dhyey.fanfic.data.ReaderTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun ReaderScreen(
    ficId: String,
    initialChapter: Int,
    onNavigateBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val readerSettings by viewModel.settings.collectAsState(initial = ReaderSettings())
    val showSettings by viewModel.showSettings.collectAsState()

    var showControls by remember { mutableStateOf(true) } // Start visible
    var showQuickSettings by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(getCurrentTime()) }

    // Update time every minute
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000)
            currentTime = getCurrentTime()
        }
    }

    val backgroundColor = Color(readerSettings.theme.bgColor)
    val textColor = Color(readerSettings.theme.textColor)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = textColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading chapter...", color = textColor.copy(alpha = 0.7f))
                    }
                }
            }

            is ReaderUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            color = textColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            is ReaderUiState.Success -> {
                val progress = state.currentChapter.toFloat() / state.totalChapters.toFloat()
                var chapterSliderValue by remember(state.currentChapter) { 
                    mutableFloatStateOf(state.currentChapter.toFloat()) 
                }

                // WebView content with tap interception
                Box(modifier = Modifier.fillMaxSize()) {
                    val bgHex = String.format("#%06X", 0xFFFFFF and backgroundColor.toArgb())
                    val textHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())
                    val context = LocalContext.current

                    androidx.compose.runtime.key(state.currentChapter) {
                        AndroidView(
                            factory = {
                                WebView(context).apply {
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            // Restore scroll position after page loads
                                            if (state.initialScrollPosition > 0) {
                                                view?.postDelayed({
                                                    view.scrollTo(0, state.initialScrollPosition)
                                                }, 100)
                                            }
                                        }
                                    }
                                    settings.javaScriptEnabled = false
                                    setBackgroundColor(backgroundColor.toArgb())
                                    isVerticalScrollBarEnabled = false
                                    isHorizontalScrollBarEnabled = false
                                    
                                    // Track scroll position and save when changed
                                    setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                                        if (scrollY != oldScrollY) {
                                            viewModel.saveScrollPosition(scrollY)
                                        }
                                    }
                                    
                                    // Track touch to distinguish tap from scroll
                                    var touchDownX = 0f
                                    var touchDownY = 0f
                                    
                                    setOnTouchListener { view, event ->
                                        val density = view.context.resources.displayMetrics.density
                                        val tapThreshold = 20 * density // 20dp movement threshold
                                        
                                        when (event.action) {
                                            MotionEvent.ACTION_DOWN -> {
                                                touchDownX = event.x
                                                touchDownY = event.y
                                            }
                                            MotionEvent.ACTION_UP -> {
                                                val deltaX = kotlin.math.abs(event.x - touchDownX)
                                                val deltaY = kotlin.math.abs(event.y - touchDownY)
                                                
                                                // Only handle as tap if finger didn't move much
                                                if (deltaX < tapThreshold && deltaY < tapThreshold) {
                                                    val edgeZone = 60 * density
                                                    val width = view.width
                                                    val x = event.x
                                                    
                                                    when {
                                                        // Left edge (60dp) - previous
                                                        x < edgeZone -> {
                                                            if (state.hasPrevious) {
                                                                viewModel.previousChapter()
                                                            }
                                                        }
                                                        // Right edge (60dp) - next
                                                        x > width - edgeZone -> {
                                                            if (state.hasNext) {
                                                                viewModel.nextChapter()
                                                            }
                                                        }
                                                        // Center - toggle controls
                                                        else -> {
                                                            showControls = !showControls
                                                            if (!showControls) showQuickSettings = false
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        false // Allow scrolling
                                    }
                                }
                            },
                            update = { webView ->
                                webView.setBackgroundColor(backgroundColor.toArgb())
                                val styledHtml = wrapHtmlWithStyle(
                                    state.htmlContent,
                                    bgHex,
                                    textHex,
                                    readerSettings.fontSize,
                                    readerSettings.lineHeight
                                )
                                webView.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }


                    // Bottom status bar (always visible)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(backgroundColor.copy(alpha = 0.95f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ch ${state.currentChapter}/${state.totalChapters}",
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                                .height(2.dp),
                            color = textColor.copy(alpha = 0.4f),
                            trackColor = textColor.copy(alpha = 0.1f)
                        )
                        Text(
                            text = currentTime,
                            color = textColor.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Top toolbar
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = backgroundColor.copy(alpha = 0.97f),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = textColor
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = state.chapterTitle,
                                    color = textColor,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Chapter ${state.currentChapter} of ${state.totalChapters}",
                                    color = textColor.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { showQuickSettings = !showQuickSettings }) {
                                Icon(
                                    Icons.Default.TextFields,
                                    contentDescription = "Font Settings",
                                    tint = textColor
                                )
                            }
                            IconButton(onClick = { viewModel.toggleSettings() }) {
                                Icon(
                                    Icons.Default.ColorLens,
                                    contentDescription = "Theme",
                                    tint = textColor
                                )
                            }
                        }
                    }
                }

                // Bottom navigation bar
                AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = backgroundColor.copy(alpha = 0.97f),
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(16.dp)
                        ) {
                            // Chapter slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("1", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                                Slider(
                                    value = chapterSliderValue,
                                    onValueChange = { chapterSliderValue = it },
                                    onValueChangeFinished = {
                                        val target = chapterSliderValue.toInt()
                                        if (target != state.currentChapter) {
                                            if (target > state.currentChapter) {
                                                repeat(target - state.currentChapter) {
                                                    viewModel.nextChapter()
                                                }
                                            } else {
                                                repeat(state.currentChapter - target) {
                                                    viewModel.previousChapter()
                                                }
                                            }
                                        }
                                    },
                                    valueRange = 1f..state.totalChapters.toFloat(),
                                    steps = (state.totalChapters - 2).coerceAtLeast(0),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = textColor,
                                        activeTrackColor = textColor.copy(alpha = 0.6f),
                                        inactiveTrackColor = textColor.copy(alpha = 0.2f)
                                    )
                                )
                                Text("${state.totalChapters}", color = textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                            }

                            // Navigation buttons
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = state.hasPrevious) { viewModel.previousChapter() },
                                    color = if (state.hasPrevious) textColor.copy(alpha = 0.1f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = null,
                                            tint = if (state.hasPrevious) textColor else textColor.copy(alpha = 0.3f)
                                        )
                                        Text(
                                            "Previous",
                                            color = if (state.hasPrevious) textColor else textColor.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable(enabled = state.hasNext) { viewModel.nextChapter() },
                                    color = if (state.hasNext) textColor.copy(alpha = 0.1f) else Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Next",
                                            color = if (state.hasNext) textColor else textColor.copy(alpha = 0.3f)
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = if (state.hasNext) textColor else textColor.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick settings panel
                AnimatedVisibility(
                    visible = showControls && showQuickSettings,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                        .statusBarsPadding()
                ) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 32.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = backgroundColor,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Font Size", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text("A", color = textColor, fontSize = 14.sp)
                                Slider(
                                    value = readerSettings.fontSize,
                                    onValueChange = { viewModel.setFontSize(it) },
                                    valueRange = 14f..28f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = textColor,
                                        activeTrackColor = textColor.copy(alpha = 0.6f),
                                        inactiveTrackColor = textColor.copy(alpha = 0.2f)
                                    )
                                )
                                Text("A", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Line Spacing", color = textColor.copy(alpha = 0.7f), fontSize = 12.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text("≡", color = textColor, fontSize = 16.sp)
                                Slider(
                                    value = readerSettings.lineHeight,
                                    onValueChange = { viewModel.setLineHeight(it) },
                                    valueRange = 1.3f..2.2f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = textColor,
                                        activeTrackColor = textColor.copy(alpha = 0.6f),
                                        inactiveTrackColor = textColor.copy(alpha = 0.2f)
                                    )
                                )
                                Text("≡", color = textColor, fontSize = 24.sp)
                            }
                        }
                    }
                }

                // Theme picker overlay
                if (showSettings) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { viewModel.toggleSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .padding(32.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { /* Block clicks */ },
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 16.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Reading Theme",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(bottom = 20.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    items(ReaderTheme.entries) { theme ->
                                        ThemeOption(
                                            theme = theme,
                                            isSelected = readerSettings.theme == theme,
                                            onClick = { viewModel.setTheme(theme) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOption(
    theme: ReaderTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(theme.bgColor))
                .then(
                    if (isSelected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aa",
                color = Color(theme.textColor),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = theme.displayName,
            fontSize = 12.sp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getCurrentTime(): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
}

private fun wrapHtmlWithStyle(
    html: String,
    backgroundColor: String,
    textColor: String,
    fontSize: Float,
    lineHeight: Float
): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
            <style>
                * {
                    -webkit-tap-highlight-color: transparent;
                }
                html, body {
                    margin: 0;
                    padding: 0;
                    background-color: $backgroundColor;
                    overflow-x: hidden;
                }
                body {
                    color: $textColor;
                    font-family: Georgia, 'Times New Roman', serif;
                    font-size: ${fontSize}px;
                    line-height: $lineHeight;
                    padding: 20px 24px 100px 24px;
                    text-align: justify;
                    word-wrap: break-word;
                }
                p {
                    margin: 0;
                    text-indent: 1.5em;
                    margin-bottom: 0.8em;
                }
                p:first-child { text-indent: 0; }
                a { color: #7C9EFF; text-decoration: none; }
                hr { border: none; border-top: 1px solid ${textColor}33; margin: 2em 0; }
            </style>
        </head>
        <body>$html</body>
        </html>
    """.trimIndent()
}
