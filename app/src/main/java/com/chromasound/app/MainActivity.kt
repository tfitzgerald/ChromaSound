package com.chromasound.app

import android.Manifest
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chromasound.app.ui.ChromaSoundScreen
import com.chromasound.app.ui.ChromaSoundUiState
import com.chromasound.app.ui.ChromaSoundViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChromaSoundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val view = LocalView.current
            SideEffect {
                val win = (view.context as? Activity)?.window ?: return@SideEffect
                WindowInsetsControllerCompat(win, view).apply {
                    isAppearanceLightStatusBars     = false
                    isAppearanceLightNavigationBars = false
                }
                win.statusBarColor     = android.graphics.Color.TRANSPARENT
                win.navigationBarColor = android.graphics.Color.TRANSPARENT
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF050508)) {
                val uiState  by viewModel.uiState.collectAsState()
                val settings by viewModel.settings.collectAsState()

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) viewModel.onPermissionGranted()
                    else         viewModel.onPermissionDenied()
                }

                LaunchedEffect(uiState) {
                    if (uiState is ChromaSoundUiState.RequestingPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                ChromaSoundScreen(
                    uiState          = uiState,
                    settings         = settings,
                    onStartRequested = { viewModel.resumeCapture() },
                    onStopRequested  = { viewModel.stopCapture() },
                    onSettingsChange = { viewModel.updateSettings(it) }
                )
            }
        }
    }
}
