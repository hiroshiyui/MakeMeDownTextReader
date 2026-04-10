package org.ghostsinthelab.app.makedown.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.ghostsinthelab.app.makedown.data.DocumentType
import org.ghostsinthelab.app.makedown.data.RecentFile
import org.ghostsinthelab.app.makedown.data.RecentsRepository
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpen: (Screen.Reader) -> Unit,
) {
    val context = LocalContext.current
    val recents = RecentsRepository.get(context)
    val items by recents.state.collectAsState()
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = OpenDocumentRW(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Try to take a persistable read+write permission so edits can be saved
        // across process restarts. Fall back to read-only if the provider
        // refuses write (e.g. read-only storage).
        val rwFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, rwFlags) }
            .recoverCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        val (displayName, mime) = queryNameAndMime(context, uri)
        val type = DocumentType.detect(mime, displayName)
        val record = RecentFile(
            uri = uri.toString(),
            displayName = displayName,
            type = type,
            lastOpenedAt = System.currentTimeMillis(),
        )
        scope.launch { recents.add(record) }
        onOpen(Screen.Reader(uri = record.uri, displayName = displayName, type = type))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("MakeMeDown") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    launcher.launch(
                        arrayOf(
                            "application/epub+zip",
                            "text/markdown",
                            "text/plain",
                            "text/*",
                            "*/*",
                        )
                    )
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Open file") },
            )
        },
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No recent files.\nTap “Open file” to read an EPUB, Markdown, or plain‑text document.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 80.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.uri }) { recent ->
                RecentRow(
                    recent = recent,
                    onClick = {
                        onOpen(
                            Screen.Reader(
                                uri = recent.uri,
                                displayName = recent.displayName,
                                type = recent.type,
                            )
                        )
                    },
                    onDelete = { scope.launch { recents.remove(recent.uri) } },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecentRow(
    recent: RecentFile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recent.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                AssistChip(
                    onClick = onClick,
                    label = {
                        Text(
                            text = when (recent.type) {
                                DocumentType.EPUB -> "EPUB"
                                DocumentType.MARKDOWN -> "Markdown"
                                DocumentType.PLAIN_TEXT -> "Text"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(),
                )
                Text(
                    text = DateFormat
                        .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(recent.lastOpenedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove from recents",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * [ActivityResultContracts.OpenDocument] variant that additionally requests
 * FLAG_GRANT_WRITE_URI_PERMISSION so the picked document can be saved back.
 * Providers that don't grant write will still return a readable URI; the
 * caller falls back to a read-only persistable permission.
 */
private class OpenDocumentRW : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent {
        return super.createIntent(context, input).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
            )
        }
    }
}

private fun queryNameAndMime(
    context: Context,
    uri: Uri,
): Pair<String, String?> {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri)
    var name: String? = null
    runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = cursor.getString(idx)
            }
        }
    }
    if (name.isNullOrBlank()) {
        name = uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled"
    }
    return name!! to mime
}
