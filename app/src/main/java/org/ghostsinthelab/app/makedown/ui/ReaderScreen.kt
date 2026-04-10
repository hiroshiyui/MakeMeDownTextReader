package org.ghostsinthelab.app.makedown.ui

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.ghostsinthelab.app.makedown.data.DocumentType
import org.ghostsinthelab.app.makedown.io.DocumentLoader
import org.ghostsinthelab.app.makedown.io.LoadedDocument
import org.ghostsinthelab.app.makedown.reader.epub.EpubReader
import org.ghostsinthelab.app.makedown.reader.markdown.MarkdownReader
import org.ghostsinthelab.app.makedown.reader.text.PlainTextReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    target: Screen.Reader,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loaded by remember(target.uri) { mutableStateOf<LoadedDocument?>(null) }
    var error by remember(target.uri) { mutableStateOf<String?>(null) }
    var showRaw by rememberSaveable(target.uri) { mutableStateOf(false) }

    LaunchedEffect(target.uri) {
        loaded = null
        error = null
        scope.launch {
            runCatching {
                DocumentLoader.load(
                    context = context,
                    uri = Uri.parse(target.uri),
                    type = target.type,
                    displayName = target.displayName,
                )
            }.onSuccess { loaded = it }
                .onFailure { error = it.message ?: it::class.simpleName ?: "Failed to open" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = loaded?.displayName ?: target.displayName,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (target.type == DocumentType.MARKDOWN) {
                        androidx.compose.material3.TextButton(onClick = { showRaw = !showRaw }) {
                            Text(if (showRaw) "Rendered" else "Raw")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when {
                error != null -> Text(
                    text = "Could not open: $error",
                    color = MaterialTheme.colorScheme.error,
                )
                loaded == null -> CircularProgressIndicator()
                else -> when (val doc = loaded!!) {
                    is LoadedDocument.PlainText -> PlainTextReader(text = doc.text)
                    is LoadedDocument.Markdown -> MarkdownReader(source = doc.source, showRaw = showRaw)
                    is LoadedDocument.Epub -> EpubReader(book = doc.book)
                }
            }
        }
    }
}
