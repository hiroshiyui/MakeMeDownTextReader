package org.ghostsinthelab.app.makedown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import org.ghostsinthelab.app.makedown.ui.ReaderScreen
import org.ghostsinthelab.app.makedown.ui.Screen
import org.ghostsinthelab.app.makedown.ui.SettingsScreen
import org.ghostsinthelab.app.makedown.ui.theme.MakeMeDownTextReaderTheme

class MainActivity : ComponentActivity() {
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

    // Any screen other than Home should route Back to Home.
    BackHandler(enabled = screen !is Screen.Home) {
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
            )
            Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home })
            is Screen.Reader -> ReaderScreen(
                target = current,
                onBack = { screen = Screen.Home },
            )
        }
    }
}
