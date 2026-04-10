package org.ghostsinthelab.app.makedown.ui

import android.os.Bundle
import androidx.compose.runtime.saveable.Saver
import org.ghostsinthelab.app.makedown.data.DocumentType

sealed interface Screen {
    data object Home : Screen
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
                    else -> Home
                }
            },
        )
    }
}
