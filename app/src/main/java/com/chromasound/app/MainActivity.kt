package com.chromasound.app

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chromasound.app.ui.ChromaSoundScreen
import com.chromasound.app.ui.ChromaSoundUiState
import com.chromasound.app.ui.ChromaSoundViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChromaSoundViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor     = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars     = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF050508)) {
                val uiState         by viewModel.uiState.collectAsState()
                val settings        by viewModel.settings.collectAsState()
                // List<Float> has structural equality so Compose correctly detects
                // content changes each audio frame and recomposes the waveform.
                val waveformSamples by viewModel.waveformSamples.collectAsState()

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
                    waveformSamples  = waveformSamples,
                    onStartRequested = { viewModel.resumeCapture() },
                    onStopRequested  = { viewModel.stopCapture() },
                    onSettingsChange = { viewModel.updateSettings(it) }
                )
            }
        }
    }
}
