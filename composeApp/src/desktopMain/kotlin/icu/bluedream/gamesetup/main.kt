package icu.bluedream.gamesetup

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.Navigator
import hoyogamesetup.composeapp.generated.resources.Res
import hoyogamesetup.composeapp.generated.resources.app_window_title
import icu.bluedream.gamesetup.ui.screen.HomeScreen
import icu.bluedream.gamesetup.ui.theme.GameSetupTheme
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_window_title)
    ) {
        GameSetupTheme {
            Surface(
                modifier = Modifier.fillMaxSize().systemBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                Navigator(HomeScreen())
            }
        }
    }
}