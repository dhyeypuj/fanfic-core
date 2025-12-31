package com.dhyey.fanfic.ui.reader

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.dhyey.fanfic.data.ReaderSettings
import com.dhyey.fanfic.data.ReaderTheme

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState()

    val backgroundColor = Color(readerSettings.theme.bgColor)
    val textColor = Color(readerSettings.theme.textColor)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState is ReaderUiState.Success) {
                        val state = uiState as ReaderUiState.Success
                        Text(
                            text = state.chapterTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor.copy(alpha = 0.95f),
                    titleContentColor = textColor
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    if (uiState is ReaderUiState.Success) {
                        val state = uiState as ReaderUiState.Success
                        IconButton(
                            onClick = { viewModel.previousChapter() },
                            enabled = state.hasPrevious
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Chapter",
                                tint = textColor.copy(alpha = if (state.hasPrevious) 1f else 0.3f)
                            )
                        }
                        Text(
                            text = "${state.currentChapter}/${state.totalChapters}",
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor
                        )
                        IconButton(
                            onClick = { viewModel.nextChapter() },
                            enabled = state.hasNext
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Chapter",
                                tint = textColor.copy(alpha = if (state.hasNext) 1f else 0.3f)
                            )
                        }
                        IconButton(onClick = { viewModel.toggleSettings() }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Reader Settings",
                                tint = textColor
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ReaderUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = textColor)
                }
            }
            is ReaderUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is ReaderUiState.Success -> {
                val screenWidth = LocalConfiguration.current.screenWidthDp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(backgroundColor)
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val thirdWidth = screenWidth / 3
                                when {
                                    offset.x < thirdWidth && state.hasPrevious -> {
                                        viewModel.previousChapter()
                                    }
                                    offset.x > thirdWidth * 2 && state.hasNext -> {
                                        viewModel.nextChapter()
                                    }
                                }
                            }
                        }
                ) {
                    val bgHex = String.format("#%06X", 0xFFFFFF and backgroundColor.toArgb())
                    val textHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = WebViewClient()
                                settings.javaScriptEnabled = false
                                setBackgroundColor(backgroundColor.toArgb())
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
                            webView.loadDataWithBaseURL(
                                null,
                                styledHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Settings Bottom Sheet
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.toggleSettings() },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReaderSettingsContent(
                    settings = readerSettings,
                    onThemeSelected = { viewModel.setTheme(it) },
                    onFontSizeChanged = { viewModel.setFontSize(it) },
                    onLineHeightChanged = { viewModel.setLineHeight(it) }
                )
            }
        }
    }
}

@Composable
private fun ReaderSettingsContent(
    settings: ReaderSettings,
    onThemeSelected: (ReaderTheme) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    onLineHeightChanged: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Reader Settings",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Theme Selection
        Text(
            text = "Theme",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ReaderTheme.entries) { theme ->
                ThemeOption(
                    theme = theme,
                    isSelected = settings.theme == theme,
                    onClick = { onThemeSelected(theme) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Font Size
        Text(
            text = "Font Size: ${settings.fontSize.toInt()}px",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = settings.fontSize,
            onValueChange = onFontSizeChanged,
            valueRange = 12f..32f,
            steps = 9,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Line Height
        Text(
            text = "Line Height: ${String.format("%.1f", settings.lineHeight)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Slider(
            value = settings.lineHeight,
            onValueChange = onLineHeightChanged,
            valueRange = 1.2f..2.5f,
            steps = 12,
            modifier = Modifier.fillMaxWidth()
        )
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
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
                style = MaterialTheme.typography.labelLarge
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
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
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {
                    background-color: $backgroundColor;
                    color: $textColor;
                    font-family: -apple-system, system-ui, sans-serif;
                    font-size: ${fontSize}px;
                    line-height: $lineHeight;
                    padding: 16px;
                    margin: 0;
                    word-wrap: break-word;
                }
                p { margin: 1em 0; }
                a { color: #7C9EFF; }
            </style>
        </head>
        <body>
            $html
        </body>
        </html>
    """.trimIndent()
}
