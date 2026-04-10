/*
 * MakeMeDown Text Reader
 * Copyright (C) 2026 Hui-Hong You
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
