package com.chromasound.app

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.chromasound.app.model.ThemeMode
import com.chromasound.app.ui.ChromaSoundScreen
import com.chromasound.app.ui.ChromaSoundUiState
import com.chromasound.app.ui.ChromaSoundViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: ChromaSoundViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
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
            val uiState         by viewModel.uiState.collectAsState()
            val settings        by viewModel.settings.collectAsState()
            val waveformSamples by viewModel.waveformSamples.collectAsState()

            // Tablet detection via WindowSizeClass
            val windowSizeClass = calculateWindowSizeClass(this)
            val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded

            // Resolve effective dark/light
            val systemDark = isSystemInDarkTheme()
            val isDark = when (settings.themeMode) {
                ThemeMode.DARK   -> true
                ThemeMode.LIGHT  -> false
                ThemeMode.SYSTEM -> systemDark
            }

            // Update status/nav bar icon brightness when theme changes
            LaunchedEffect(isDark) {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars     = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }

            val bgColor = if (isDark) Color(0xFF050508) else Color(0xFFF5F5FA)

            Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
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
                    uiState               = uiState,
                    settings              = settings,
                    waveformSamples       = waveformSamples,
                    isDark                = isDark,
                    isTablet              = isTablet,
                    onStartRequested      = { viewModel.resumeCapture() },
                    onStopRequested       = { viewModel.stopCapture() },
                    onSettingsChange      = { viewModel.updateSettings(it) },
                    onScreenshotRequested = { takeScreenshot() }
                )
            }
        }
    }

    // ── Screenshot ────────────────────────────────────────────────────────────

    private fun takeScreenshot() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage permission needed to save screenshot",
                    Toast.LENGTH_SHORT).show()
                return
            }
        }
        val view   = window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        PixelCopy.request(window, bitmap, { result ->
            if (result == PixelCopy.SUCCESS) saveBitmapToGallery(bitmap)
            else runOnUiThread {
                Toast.makeText(this, "Screenshot failed — try again", Toast.LENGTH_SHORT).show()
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename  = "ChromaSound_$timestamp.png"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ChromaSound")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                uri?.let { u ->
                    contentResolver.openOutputStream(u)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
            } else {
                val dir  = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES)
                val file = java.io.File(java.io.File(dir, "ChromaSound").also { it.mkdirs() }, filename)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                android.media.MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), null, null)
            }
            runOnUiThread {
                Toast.makeText(this, "Saved to Pictures/ChromaSound 📷", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Could not save: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
