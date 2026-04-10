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

package org.ghostsinthelab.app.makedown.ui

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import org.ghostsinthelab.app.makedown.data.DocumentType

sealed interface Screen {
    data object Home : Screen
    data object Settings : Screen
    data class Reader(
        val uri: String,
        val displayName: String,
        val type: DocumentType,
        /** If true, the reader should enter edit mode the first time this
         *  target loads. Consumed after the first use so subsequent restores
         *  (config change, process death) don't re-enter edit mode. */
        val initialEdit: Boolean = false,
    ) : Screen

    companion object {
        val Saver: Saver<Screen, Bundle> = Saver(
            save = { screen ->
                Bundle().apply {
                    when (screen) {
                        is Home -> putString("kind", "home")
                        is Settings -> putString("kind", "settings")
                        is Reader -> {
                            putString("kind", "reader")
                            putString("uri", screen.uri)
                            putString("name", screen.displayName)
                            putString("type", screen.type.name)
                            putBoolean("initialEdit", screen.initialEdit)
                        }
                    }
                }
            },
            restore = { bundle ->
                when (bundle.getString("kind")) {
                    "reader" -> Reader(
                        uri = bundle.getString("uri").orEmpty(),
                        displayName = bundle.getString("name").orEmpty(),
                        type = DocumentType.valueOf(
                            bundle.getString("type") ?: DocumentType.PLAIN_TEXT.name
                        ),
                        initialEdit = bundle.getBoolean("initialEdit", false),
                    )
                    "settings" -> Settings
                    else -> Home
                }
            },
        )
    }
}
