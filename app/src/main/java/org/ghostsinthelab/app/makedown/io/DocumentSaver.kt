package org.ghostsinthelab.app.makedown.io

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DocumentSaver {

    /**
     * Write [text] as UTF-8 to [uri], truncating any existing content. Throws
     * on failure (caller is expected to handle the error, e.g. via a Snackbar).
     *
     * The `"wt"` mode (write + truncate) ensures the old contents are cleared
     * before the new bytes are written — this matters on providers that keep
     * the existing file length if you only open as `"w"`.
     */
    suspend fun save(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val out = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Cannot open ${uri} for writing")
        out.use { stream ->
            stream.write(bytes)
            stream.flush()
        }
    }
}
