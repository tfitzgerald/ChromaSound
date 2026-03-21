package com.chromasound.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.chromasound.app.ui.ChromaSoundScreen
import com.chromasound.app.ui.ChromaSoundUiState
import com.chromasound.app.ui.ChromaSoundViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChromaSoundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw edge-to-edge (no status bar gap)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            // Dark status + nav bar icons
            val systemUiController = rememberSystemUiController()
            SideEffect {
                systemUiController.setSystemBarsColor(Color.Transparent, darkIcons = false)
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF050508)
            ) {
                val uiState by viewModel.uiState.collectAsState()

                // Permission launcher
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) viewModel.onPermissionGranted()
                    else viewModel.onPermissionDenied()
                }

                // Drive permission request when state says we need it
                LaunchedEffect(uiState) {
                    if (uiState is ChromaSoundUiState.RequestingPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                ChromaSoundScreen(
                    uiState = uiState,
                    onStartRequested = { viewModel.resumeCapture() },
                    onStopRequested  = { viewModel.stopCapture() }
                )
            }
        }
    }
}

// ── Minimal SystemUiController shim ──────────────────────────────────────────
// (avoids adding the accompanist-systemuicontroller dep for this single use)

import android.app.Activity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

private class SystemUiController(private val view: View) {
    fun setSystemBarsColor(color: Color, darkIcons: Boolean) {
        val window = (view.context as? Activity)?.window ?: return
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = darkIcons
        controller.isAppearanceLightNavigationBars = darkIcons
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }
}

@Composable
private fun rememberSystemUiController(): SystemUiController {
    val view = LocalView.current
    return remember(view) { SystemUiController(view) }
}
