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

package org.ghostsinthelab.app.makedown

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.ghostsinthelab.app.makedown.data.DEFAULT_READER_BASE_PT
import org.ghostsinthelab.app.makedown.data.SettingsRepository
import org.ghostsinthelab.app.makedown.ui.HomeScreen
import org.ghostsinthelab.app.makedown.ui.LocalReaderFontFamily
import org.ghostsinthelab.app.makedown.ui.LocalReaderFontScale
import org.ghostsinthelab.app.makedown.ui.PRIVATE_URI_PREFIX
import org.ghostsinthelab.app.makedown.ui.PrivateSpaceScreen
import org.ghostsinthelab.app.makedown.ui.ReaderScreen
import org.ghostsinthelab.app.makedown.ui.Screen
import org.ghostsinthelab.app.makedown.ui.SettingsScreen
import org.ghostsinthelab.app.makedown.ui.theme.MakeMeDownTextReaderTheme

/**
 * Process-scoped flag tracking whether the user has authenticated their
 * way into the private documents space during the current process.
 *
 * Survives Activity recreation (config change) but **not** process death,
 * because the JVM-level [Object] gets collected with the process. AppRoot
 * relies on this: on cold start it inspects the rememberSaveable Screen
 * state and, if the user was restored to PrivateSpace without [unlocked]
 * being true, redirects to Home so authentication is required again.
 */
object PrivateSpaceSession {
    @Volatile
    var unlocked: Boolean = false
}

// Extends FragmentActivity (rather than the simpler ComponentActivity)
// because androidx.biometric.BiometricPrompt requires a FragmentActivity
// host so it can attach its prompt fragment to the support fragment
// manager. FragmentActivity itself extends ComponentActivity, so all the
// activity-result-contracts and Compose plumbing keeps working.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MakeMeDownTextReaderTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val settings by SettingsRepository.get(context).state.collectAsState()

    var screen: Screen by rememberSaveable(stateSaver = Screen.Saver) {
        mutableStateOf(Screen.Home)
    }

    // Re-auth on cold start: if the saved state put us in PrivateSpace
    // but the in-process unlock flag is false (meaning the process died
    // and was restored from saved state), bounce back to Home so the
    // user has to pass biometric/device-credential again. Runs once per
    // composition; on rotation the flag is preserved and this is a no-op.
    LaunchedEffect(Unit) {
        if (screen is Screen.PrivateSpace && !PrivateSpaceSession.unlocked) {
            screen = Screen.Home
        }
        if (screen is Screen.Reader &&
            (screen as Screen.Reader).uri.startsWith(PRIVATE_URI_PREFIX) &&
            !PrivateSpaceSession.unlocked
        ) {
            // Same guard for a private document opened via the reader
            // when the process was killed mid-read.
            screen = Screen.Home
        }
    }

    BackHandler(enabled = screen !is Screen.Home) {
        if (screen is Screen.PrivateSpace) {
            PrivateSpaceSession.unlocked = false
        }
        screen = Screen.Home
    }

    CompositionLocalProvider(
        LocalReaderFontFamily provides settings.readerFont.toFontFamily(),
        LocalReaderFontScale provides (settings.baseFontSizePt / DEFAULT_READER_BASE_PT),
    ) {
        when (val current = screen) {
            Screen.Home -> HomeScreen(
                onOpen = { screen = it },
                onOpenSettings = { screen = Screen.Settings },
                onOpenPrivateSpace = {
                    PrivateSpaceSession.unlocked = true
                    screen = Screen.PrivateSpace
                },
            )
            Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home })
            Screen.PrivateSpace -> PrivateSpaceScreen(
                onBack = {
                    PrivateSpaceSession.unlocked = false
                    screen = Screen.Home
                },
                onOpen = { screen = it },
            )
            is Screen.Reader -> ReaderScreen(
                target = current,
                // Source-aware back: a reader opened from the private space
                // returns to the private space, not Home, so the user can
                // jump between several private documents without re-auth.
                onBack = {
                    screen = if (current.uri.startsWith(PRIVATE_URI_PREFIX)) {
                        Screen.PrivateSpace
                    } else {
                        Screen.Home
                    }
                },
            )
        }
    }
}
