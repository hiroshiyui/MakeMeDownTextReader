package org.ghostsinthelab.app.makedown.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import org.ghostsinthelab.app.makedown.data.DocumentType
import org.ghostsinthelab.app.makedown.data.ReaderFont
import org.ghostsinthelab.app.makedown.data.SettingsRepository
import org.ghostsinthelab.app.makedown.io.DocumentLoader
import org.ghostsinthelab.app.makedown.io.DocumentSaver
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
    val snackbar = remember { SnackbarHostState() }
    val settingsRepo = remember { SettingsRepository.get(context) }
    val settings by settingsRepo.state.collectAsState()

    var loaded by remember(target.uri) { mutableStateOf<LoadedDocument?>(null) }
    var error by remember(target.uri) { mutableStateOf<String?>(null) }
    var showRaw by rememberSaveable(target.uri) { mutableStateOf(false) }

    // Edit mode state.
    var editing by rememberSaveable(target.uri) { mutableStateOf(false) }
    // Tracks whether we have already honored target.initialEdit for this uri.
    // Without this flag, a config change or process restart would silently
    // re-enter edit mode after the user has explicitly left it.
    var initialEditConsumed by rememberSaveable(target.uri) { mutableStateOf(false) }
    var editorValue by rememberSaveable(
        target.uri,
        stateSaver = TextFieldValue.Saver,
    ) { mutableStateOf(TextFieldValue("")) }
    // Text currently persisted on disk (i.e. the source of the loaded doc).
    // When this differs from editorValue.text while editing, the buffer is dirty.
    val savedText: String = when (val doc = loaded) {
        is LoadedDocument.PlainText -> doc.text
        is LoadedDocument.Markdown -> doc.source
        else -> ""
    }
    val isDirty = editing && editorValue.text != savedText
    var showDiscardDialog by rememberSaveable(target.uri) { mutableStateOf(false) }

    val isTextual = target.type == DocumentType.MARKDOWN || target.type == DocumentType.PLAIN_TEXT

    LaunchedEffect(target.uri) {
        loaded = null
        error = null
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

    fun startEdit() {
        editorValue = TextFieldValue(savedText)
        editing = true
    }

    // Honor target.initialEdit once, after the document loads. A freshly
    // created file arrives empty, and the user expects to land in edit mode.
    LaunchedEffect(loaded, target.initialEdit) {
        if (
            target.initialEdit &&
            !initialEditConsumed &&
            isTextual &&
            loaded != null &&
            !editing
        ) {
            startEdit()
            initialEditConsumed = true
        } else if (loaded != null && !initialEditConsumed) {
            // Even when initialEdit is false, mark it consumed so later
            // recompositions don't reconsider the flag.
            initialEditConsumed = true
        }
    }

    fun attemptExitEdit() {
        if (isDirty) showDiscardDialog = true else editing = false
    }

    fun saveNow(thenExitEdit: Boolean) {
        val toWrite = editorValue.text
        scope.launch {
            runCatching {
                DocumentSaver.save(context, Uri.parse(target.uri), toWrite)
            }.onSuccess {
                // Update the loaded document so the rendered view reflects the
                // new content when we leave edit mode.
                loaded = when (val d = loaded) {
                    is LoadedDocument.PlainText -> d.copy(text = toWrite)
                    is LoadedDocument.Markdown -> d.copy(source = toWrite)
                    else -> d
                }
                snackbar.showSnackbar("Saved")
                if (thenExitEdit) editing = false
            }.onFailure { e ->
                snackbar.showSnackbar("Save failed: ${e.message ?: e::class.simpleName}")
            }
        }
    }

    // Back button while editing: exit edit mode first (with dirty prompt).
    BackHandler(enabled = editing) { attemptExitEdit() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!editing) {
                ReaderBottomBar(
                    baseFontSizePt = settings.baseFontSizePt,
                    font = settings.readerFont,
                    onBack = onBack,
                    onBaseFontSizeChange = { newSize ->
                        scope.launch {
                            settingsRepo.update { it.copy(baseFontSizePt = newSize) }
                        }
                    },
                    onFontChange = { newFont: ReaderFont ->
                        scope.launch {
                            settingsRepo.update { it.copy(readerFont = newFont) }
                        }
                    },
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (loaded?.displayName ?: target.displayName) + if (isDirty) " •" else "",
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (editing) attemptExitEdit() else onBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (!editing && target.type == DocumentType.MARKDOWN) {
                        TextButton(onClick = { showRaw = !showRaw }) {
                            Text(if (showRaw) "Rendered" else "Raw")
                        }
                    }
                    if (isTextual) {
                        if (editing) {
                            TextButton(
                                enabled = isDirty,
                                onClick = { saveNow(thenExitEdit = false) },
                            ) { Text("Save") }
                            TextButton(onClick = { attemptExitEdit() }) { Text("Done") }
                        } else {
                            TextButton(
                                enabled = loaded != null,
                                onClick = { startEdit() },
                            ) { Text("Edit") }
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
                editing -> TextEditor(
                    value = editorValue,
                    onValueChange = { editorValue = it },
                    monospace = target.type == DocumentType.PLAIN_TEXT ||
                        target.type == DocumentType.MARKDOWN,
                )
                else -> when (val doc = loaded!!) {
                    is LoadedDocument.PlainText -> PlainTextReader(text = doc.text)
                    is LoadedDocument.Markdown -> MarkdownReader(source = doc.source, showRaw = showRaw)
                    is LoadedDocument.Epub -> EpubReader(book = doc.book)
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Unsaved changes") },
            text = { Text("Save your edits before leaving edit mode?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    saveNow(thenExitEdit = true)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    editing = false
                }) { Text("Discard") }
            },
        )
    }
}
