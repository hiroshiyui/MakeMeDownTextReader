package org.ghostsinthelab.app.makedown

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import org.ghostsinthelab.app.makedown.ui.HomeScreen
import org.ghostsinthelab.app.makedown.ui.ReaderScreen
import org.ghostsinthelab.app.makedown.ui.Screen
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
    var screen: Screen by rememberSaveable(stateSaver = Screen.Saver) {
        mutableStateOf(Screen.Home)
    }

    BackHandler(enabled = screen is Screen.Reader) {
        screen = Screen.Home
    }

    when (val current = screen) {
        Screen.Home -> HomeScreen(onOpen = { screen = it })
        is Screen.Reader -> ReaderScreen(
            target = current,
            onBack = { screen = Screen.Home },
        )
    }
}
