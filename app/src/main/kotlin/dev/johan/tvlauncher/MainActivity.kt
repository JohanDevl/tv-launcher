package dev.johan.tvlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import dev.johan.tvlauncher.ui.home.HomeScreen
import dev.johan.tvlauncher.ui.home.HomeViewModel
import dev.johan.tvlauncher.ui.theme.LauncherTheme
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme {
                HomeScreen(viewModel, homePresses)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onShown()
    }

    // Home pressed (from the launcher itself or from another app): start again from the first tile.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.tryEmit(Unit)
    }
}
