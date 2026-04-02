package com.chromasound.app.ui

import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.chromasound.app.model.FrequencyCircle
import com.chromasound.app.model.BackgroundEffect
import com.chromasound.app.model.MirrorMode
import com.chromasound.app.model.ObjectShape
import com.chromasound.app.model.Settings
import kotlin.math.*

private val BgColor  = Color(0xFF050508)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

// ── Root ──────────────────────────────────────────────────────────────────────
@Composable
fun ChromaSoundScreen(
    uiState:               ChromaSoundUiState,
    settings:              Settings,
    waveformSamples:       List<Float>,
    isDark:                Boolean = true,
    isTablet:              Boolean = false,
    onStartRequested:      () -> Unit,
    onStopRequested:       () -> Unit,
    onSettingsChange:      (Settings) -> Unit,
    onScreenshotRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val chromaColors = if (isDark) DarkChromaColors else LightChromaColors
    var showOnboarding by remember { mutableStateOf(!hasSeenOnboarding(context)) }
    var showSettings   by remember { mutableStateOf(false) }
    var showBandColors by remember { mutableStateOf(false) }
    var showHelp       by remember { mutableStateOf(false) }
    var showPresets    by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalChromaTheme provides chromaColors) {
    when {
        showOnboarding -> OnboardingScreen(
            onDone = { showOnboarding = false }
        )
        showHelp -> HelpScreen(
            onClose = { showHelp = false }
        )
        showBandColors -> BandColorScreen(
            currentSettings  = settings,
            onSettingsChange = onSettingsChange,
            onClose          = { showBandColors = false }
        )
        showPresets -> PresetsScreen(
            currentSettings  = settings,
            onApplySettings  = { onSettingsChange(it) },
            onClose          = { showPresets = false }
        )
        showSettings -> SettingsScreen(
            currentSettings  = settings,
            onSettingsChange = onSettingsChange,
            onClose          = { showSettings = false },
            onOpenBandColors = { showSettings = false; showBandColors = true },
            onOpenHelp       = { showSettings = false; showHelp = true },
            onOpenPresets    = { showSettings = false; showPresets = true }
        )
        else -> Box(
            modifier = Modifier.fillMaxSize().background(BgColor),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is ChromaSoundUiState.Running ->
                    RunningScreen(
                        state             = uiState,
                        objectShape       = settings.objectShape,
                        mirrorMode        = settings.mirrorMode,
                        trailLength       = settings.trailLength,
                        colorAnimSpeed    = settings.colorAnimSpeed,
                        showWaveform      = settings.showWaveform,
                        waveformSamples   = waveformSamples,
                        particlesEnabled  = settings.particlesEnabled,
                        particleThreshold = settings.particleThreshold,
                        oscilloscopeMode  = settings.oscilloscopeMode,
                        backgroundEffect  = settings.backgroundEffect,
                        shapeOpacity      = settings.shapeOpacity,
                        peakHz            = run {
                            // parse "440 Hz" or "2.1 kHz" back to Float
                            val s = uiState.peakHz
                            when {
                                s.contains("kHz") -> s.replace(" kHz","").toFloatOrNull()?.times(1000f) ?: 440f
                                s.contains("Hz")  -> s.replace(" Hz","").toFloatOrNull() ?: 440f
                                else -> 440f
                            }
                        },
                        isTablet          = isTablet,
                        onStop            = onStopRequested,
                        onSettings        = { showSettings = true },
                        onScreenshot      = onScreenshotRequested
                    )
                ChromaSoundUiState.PermissionDenied -> PermissionDeniedScreen()
                else -> IdleScreen(
                    onStart    = onStartRequested,
                    onSettings = { showSettings = true },
                    onPresets  = { showPresets = true },
                    isTablet   = isTablet
                )
            }
        }
    } // end when
    } // end CompositionLocalProvider
}

// ── Idle ──────────────────────────────────────────────────────────────────────
@Composable
private fun IdleScreen(
    onStart:    () -> Unit,
    onSettings: () -> Unit = {},
    onPresets:  () -> Unit = {},
    isTablet:   Boolean    = false
) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "s"
    )
    val horizPad = if (isTablet) 64.dp else 32.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(if (isTablet) 0.55f else 1f).padding(horizPad)
    ) {
        Box(
            Modifier.size((120 * scale).dp).background(
                Brush.radialGradient(listOf(UiAccent.copy(alpha = 0.8f), Color.Transparent)),
                CircleShape
            )
        )
        Spacer(Modifier.height(40.dp))
        Text("CHROMA SOUND", color = UiText, fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace, letterSpacing = 6.sp)
        Spacer(Modifier.height(8.dp))
        Text("30 Hz – 11 kHz  ·  sub-band shading",
            color = UiSubtle, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
        Spacer(Modifier.height(56.dp))
        Button(
            onClick = onStart,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = UiAccent),
            modifier = Modifier.fillMaxWidth(0.65f).height(52.dp)
        ) {
            Text("TAP TO LISTEN", fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, letterSpacing = 3.sp, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onPresets,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = UiAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, UiAccent.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("PRESETS", fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, letterSpacing = 2.sp)
            }
            OutlinedButton(
                onClick = onSettings,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = UiSubtle),
                border = androidx.compose.foundation.BorderStroke(1.dp, UiSubtle.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text("SETTINGS", fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp, letterSpacing = 2.sp)
            }
        }
    }
}

// ── Running ───────────────────────────────────────────────────────────────────
@Composable
private fun RunningScreen(
    state:             ChromaSoundUiState.Running,
    objectShape:       ObjectShape,
    mirrorMode:        MirrorMode,
    trailLength:       Int,
    colorAnimSpeed:    Float,
    showWaveform:      Boolean,
    waveformSamples:   List<Float>,
    particlesEnabled:  Boolean,
    particleThreshold: Float,
    oscilloscopeMode:  Boolean,
    backgroundEffect:  BackgroundEffect,
    shapeOpacity:      Float   = 1.0f,
    peakHz:            Float   = 440f,
    isTablet:          Boolean = false,
    onStop:            () -> Unit,
    onSettings:        () -> Unit,
    onScreenshot:      () -> Unit
) {
    val theme = LocalChromaTheme.current
    if (isTablet) {
        // Tablet: visualiser on the left, info panel on the right
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1.4f).fillMaxHeight()) {
                VisualizerCanvas(
                    circles           = state.circles,
                    bandCount         = state.bandCount,
                    shape             = objectShape,
                    mirrorMode        = mirrorMode,
                    trailLength       = trailLength,
                    beatPulseMs       = state.beatPulseMs,
                    colorAnimSpeed    = colorAnimSpeed,
                    showWaveform      = showWaveform,
                    waveformSamples   = waveformSamples,
                    particlesEnabled  = particlesEnabled,
                    particleThreshold = particleThreshold,
                    oscilloscopeMode  = oscilloscopeMode,
                    backgroundEffect  = backgroundEffect,
                    rmsVolume         = state.rmsVolume,
                    shapeOpacity      = shapeOpacity,
                    peakHz            = peakHz,
                    isDark            = theme.isDark,
                    modifier          = Modifier.fillMaxSize()
                )
                // Tablet HUD overlay
                TopHud(
                    rmsVolume   = state.rmsVolume,
                    activeCount = state.activeCount,
                    bandCount   = state.bandCount,
                    peakHz      = state.peakHz,
                    peakDb      = state.peakDb,
                    bpm         = state.bpm,
                    onSettings  = onSettings,
                    modifier    = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                        .padding(top = 52.dp, start = 20.dp, end = 20.dp)
                )
            }
            // Side info panel
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .background(theme.bgCard)
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Spacer(Modifier.height(52.dp))
                    Text("CHROMASOUND", color = theme.uiAccent, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp)
                    Spacer(Modifier.height(16.dp))
                    TabletStatRow("BANDS",   "${state.activeCount} / ${state.bandCount}", theme)
                    TabletStatRow("PEAK",    state.peakHz, theme)
                    TabletStatRow("LEVEL",   state.peakDb, theme)
                    if (state.bpm > 0f)
                        TabletStatRow("BPM", "${state.bpm.toInt()}", theme)
                    Spacer(Modifier.height(20.dp))
                    Text("VOL", color = theme.uiSubtle, fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                    Spacer(Modifier.height(6.dp))
                    VolumeBar(state.rmsVolume, Modifier.fillMaxWidth().height(8.dp))
                    Spacer(Modifier.height(10.dp))
                    RmsHistoryGraph(state.rmsVolume, Modifier.fillMaxWidth().height(60.dp))
                }
                Column {
                    Button(onClick = onScreenshot,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.uiAccent)
                    ) { Text("📷  SCREENSHOT", fontFamily = FontFamily.Monospace, fontSize = 11.sp) }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.bgColor)
                    ) { Text("⚙  SETTINGS", fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        color = theme.uiSubtle) }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.uiText),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.uiSubtle)
                    ) { Text("■  STOP", fontFamily = FontFamily.Monospace, letterSpacing = 3.sp) }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    } else {
    // Phone layout
    Box(Modifier.fillMaxSize()) {
        VisualizerCanvas(
            circles           = state.circles,
            bandCount         = state.bandCount,
            shape             = objectShape,
            mirrorMode        = mirrorMode,
            trailLength       = trailLength,
            beatPulseMs       = state.beatPulseMs,
            colorAnimSpeed    = colorAnimSpeed,
            showWaveform      = showWaveform,
            waveformSamples   = waveformSamples,
            particlesEnabled  = particlesEnabled,
            particleThreshold = particleThreshold,
            oscilloscopeMode  = oscilloscopeMode,
            backgroundEffect  = backgroundEffect,
            rmsVolume         = state.rmsVolume,
                    shapeOpacity      = shapeOpacity,
                    isDark            = theme.isDark,
            modifier          = Modifier.fillMaxSize()
        )
        TopHud(
            rmsVolume   = state.rmsVolume,
            activeCount = state.activeCount,
            bandCount   = state.bandCount,
            peakHz      = state.peakHz,
            peakDb      = state.peakDb,
            bpm         = state.bpm,
            onSettings  = onSettings,
            modifier    = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .padding(top = 52.dp, start = 20.dp, end = 20.dp)
        )
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onStop,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
                border = androidx.compose.foundation.BorderStroke(1.dp, UiSubtle)
            ) {
                Text("■  STOP", fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onScreenshot,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = UiAccent),
                border = androidx.compose.foundation.BorderStroke(1.dp, UiAccent.copy(alpha = 0.5f))
            ) {
                Text("📷", fontSize = 16.sp)
            }
        }
    } // end phone Box
    } // end phone else
}

// ── Unified visualiser canvas ─────────────────────────────────────────────────
@Composable
private fun VisualizerCanvas(
    circles:           List<FrequencyCircle>,
    bandCount:         Int,
    shape:             ObjectShape,
    mirrorMode:        MirrorMode,
    trailLength:       Int,
    beatPulseMs:       Long,
    colorAnimSpeed:    Float,
    showWaveform:      Boolean,
    waveformSamples:   List<Float>,
    particlesEnabled:  Boolean,
    particleThreshold: Float,
    oscilloscopeMode:  Boolean,
    backgroundEffect:  BackgroundEffect,
    rmsVolume:         Float,
    shapeOpacity:      Float    = 1.0f,
    peakHz:            Float    = 440f,
    isDark:            Boolean  = true,
    modifier:          Modifier = Modifier
) {
    val theme = LocalChromaTheme.current
    var nowMs        by remember { mutableStateOf(System.currentTimeMillis()) }
    var angleRad     by remember { mutableStateOf(0f) }
    var hueOffsetDeg by remember { mutableStateOf(0f) }

    // ── Ribbon state — per-band phase and drift offsets ──────────────────
    // ribbonPhase[band]  = current sine wave phase (advances with angleRad)
    // ribbonDriftX/Y[band] = accumulated drift offset in pixels
    val MAX_RIBBON_BANDS = 24
    val ribbonPhase  = remember { FloatArray(MAX_RIBBON_BANDS) }
    val ribbonDriftX = remember { FloatArray(MAX_RIBBON_BANDS) }
    val ribbonDriftY = remember { FloatArray(MAX_RIBBON_BANDS) }

    // ── Vector shape state — spinning frequency arrows ────────────────────
    // Spawned every ~1 second per band when shape=VECTOR.
    // Lifetime 15s, full opacity for 12s then fade over final 3s.
    data class VectorShape(
        val bandIndex:     Int,
        val centreX:       Float,     // 0–1 normalised
        val centreY:       Float,     // 0–1 normalised
        val halfLength:    Float,     // pixels from centre to each tip
        val color:         Color,
        val spawnTimeMs:   Long,
        val rotationSpeed: Float,     // radians per second
        val initialAngle:  Float      // starting angle at spawn
) {
        val lifetimeMs = 15_000L
        fun isAlive(nowMs: Long) = (nowMs - spawnTimeMs) < lifetimeMs
        fun alpha(nowMs: Long): Float {
            val age = (nowMs - spawnTimeMs).coerceAtLeast(0L)
            val fadeStart = 12_000L
            return if (age < fadeStart) 1f
            else ((lifetimeMs - age).toFloat() / (lifetimeMs - fadeStart)).coerceIn(0f, 1f)
        }
        fun currentAngle(nowMs: Long): Float {
            val age = (nowMs - spawnTimeMs) / 1000f
            return initialAngle + age * rotationSpeed
        }
    }
    val vectors          = remember { mutableStateListOf<VectorShape>() }
    var lastVectorSpawnMs by remember { mutableStateOf(0L) }

    // ── Particle system state ─────────────────────────────────────────────
    data class Particle(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var life: Float,          // 1.0 = fresh, 0.0 = dead
        val color: Color
    )
    val particles = remember { mutableStateListOf<Particle>() }
    // Track last RMS per band for transient detection
    val prevBandRms = remember { FloatArray(32) }

    // ── Terrain ring buffer — last 30 rows of per-band dB levels ─────────
    val TERRAIN_ROWS   = 30
    val terrainHistory = remember { ArrayDeque<FloatArray>() }
    val terrainColors  = remember { ArrayDeque<Array<androidx.compose.ui.graphics.Color>>() }

    // Push new terrain row when circles update (each audio frame)
    if (circles !== prevCircles && backgroundEffect == BackgroundEffect.TERRAIN) {
        if (circles.isNotEmpty()) {
            val heights = FloatArray(bandCount)
            val colors  = Array(bandCount) { androidx.compose.ui.graphics.Color.Transparent }
            circles.forEach { c ->
                val b = c.bandIndex.coerceIn(0, bandCount - 1)
                val h = ((c.decibelLevel - (-80f)) / 60f).coerceIn(0f, 1f)
                if (h > heights[b]) { heights[b] = h; colors[b] = c.color }
            }
            terrainHistory.addFirst(heights)
            terrainColors.addFirst(colors)
            while (terrainHistory.size > TERRAIN_ROWS) {
                terrainHistory.removeLast(); terrainColors.removeLast()
            }
        }
    }

    // ── Starfield state ───────────────────────────────────────────────────
    data class Star(var x: Float, var y: Float, val speed: Float, val size: Float)
    val stars = remember {
        List(120) { Star(
            x     = kotlin.random.Random.nextFloat(),
            y     = kotlin.random.Random.nextFloat(),
            speed = 0.0003f + kotlin.random.Random.nextFloat() * 0.0008f,
            size  = 3f + kotlin.random.Random.nextFloat() * 5f   // 3–8px, clearly visible
        ) }
    }

    // Trail history: a ring buffer of recent circle snapshots.
    // Each entry is a full List<FrequencyCircle> captured at that frame.
    // Index 0 = most recent past frame, MAX_TRAIL_LENGTH-1 = oldest.
    val trailHistory = remember {
        ArrayDeque<List<FrequencyCircle>>(Settings.MAX_TRAIL_LENGTH)
    }
    var prevCircles by remember { mutableStateOf<List<FrequencyCircle>>(emptyList()) }

    // Beat pulse animation — animates from 1.4 back to 1.0 over 250ms on each beat
    var pulseScale by remember { mutableStateOf(1f) }
    val animatedPulse by animateFloatAsState(
        targetValue    = pulseScale,
        animationSpec  = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessHigh
        ),
        label = "beatPulse"
    )
    // Trigger pulse whenever beatPulseMs changes
    LaunchedEffect(beatPulseMs) {
        if (beatPulseMs > 0L) {
            pulseScale = 1.4f
            delay(16L)      // let one frame render at 1.4x before springing back
            pulseScale = 1f
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { frameMs ->
                angleRad = (frameMs / 2000f * 2f * PI.toFloat()) % (2f * PI.toFloat())
                if (colorAnimSpeed > 0f)
                    hueOffsetDeg = (hueOffsetDeg + colorAnimSpeed * 0.6f) % 360f
                nowMs = frameMs
                // Advance ribbon phases each display frame
                if (shape == ObjectShape.RIBBON) {
                    for (b in 0 until MAX_RIBBON_BANDS) {
                        val waveSpeed = 0.8f + b * 0.04f   // treble ripples faster
                        ribbonPhase[b]  = (ribbonPhase[b]  + waveSpeed * 0.016f) % (2f * PI.toFloat())
                        // Drift: each band fans outward from centre slowly
                        val driftAngle = (90f + (b - MAX_RIBBON_BANDS / 2f) * 5f) * PI.toFloat() / 180f
                        val driftSpeed = 0.3f
                        ribbonDriftX[b] = (ribbonDriftX[b] + kotlin.math.cos(driftAngle) * driftSpeed)
                        ribbonDriftY[b] = (ribbonDriftY[b] + kotlin.math.sin(driftAngle) * driftSpeed)
                        // Wrap drift so ribbons don't escape screen forever
                        if (kotlin.math.abs(ribbonDriftX[b]) > 150f) ribbonDriftX[b] *= 0.95f
                        if (kotlin.math.abs(ribbonDriftY[b]) > 150f) ribbonDriftY[b] *= 0.95f
                    }
                }
            }
        }
    }

    if (circles !== prevCircles) {
        if (trailLength > 0 && prevCircles.isNotEmpty()) {
            trailHistory.addFirst(prevCircles)
            while (trailHistory.size > Settings.MAX_TRAIL_LENGTH) {
                trailHistory.removeLast()
            }
        }
        prevCircles = circles
    }

    // Read rmsVolume and nowMs HERE in the composable body — NOT inside Canvas.
    // Canvas{} is a DrawScope lambda, not a @Composable. State reads inside it
    // are invisible to Compose's snapshot system and do NOT trigger redraws.
    // By capturing them here, Compose registers them as dependencies and
    // invalidates VisualizerCanvas every time either value changes.
    val capturedRms    = rmsVolume
    val capturedNowMs  = nowMs
    val capturedPeakHz = peakHz

    // Julia set: rendered into a small bitmap each audio frame (~10fps)
    // Resolution kept low (100×178) for CPU performance — scales up beautifully
    val JULIA_W = 100; val JULIA_H = 178
    val juliaPixels = remember { IntArray(JULIA_W * JULIA_H) }
    var juliaBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    // Recompute Julia set when audio changes (driven by capturedRms/capturedPeakHz)
    LaunchedEffect(capturedRms, capturedPeakHz) {
        if (backgroundEffect == BackgroundEffect.JULIA) {
            // Map frequency (30-11000 Hz) → cx in range (-0.8, 0.4)
            // Map RMS (0-1) → cy in range (-0.5, 0.5)
            val cx = -0.8f + (capturedPeakHz.coerceIn(30f, 11000f) / 11000f) * 1.2f
            val cy = -0.5f + (capturedRms * 25f).coerceIn(0f, 1f) * 1.0f
            // Render Julia set at low resolution on IO thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                renderJulia(juliaPixels, JULIA_W, JULIA_H, cx, cy)
            }
            juliaBitmap = ImageBitmap(JULIA_W, JULIA_H).also { bmp ->
                val pixMap = bmp.prepareToDraw()
            }
            // Use android.graphics.Bitmap for pixel manipulation
            val androidBmp = android.graphics.Bitmap.createBitmap(
                juliaPixels, JULIA_W, JULIA_H, android.graphics.Bitmap.Config.ARGB_8888)
            juliaBitmap = androidBmp.asImageBitmap()
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // ── 0. Background ─────────────────────────────────────────────────
        when (backgroundEffect) {
            // Canvas background is ALWAYS near-black regardless of theme.
            // Light theme applies to UI chrome (settings, HUD cards) but the
            // visualiser canvas is always a dark stage — shapes use BlendMode.Screen
            // which requires a dark background to glow correctly.
            BackgroundEffect.BLOOM -> {
                drawRect(color = Color(0xFF050508))
                // Multiplier of 12 maps real mic RMS range (0.001–0.08) across
                // the full 0–1 bloom range. The previous ×40 caused saturation
                // above rms=0.025 so the bloom never visibly changed with dynamics.
                val rmsAmp = (capturedRms * 12f).coerceIn(0f, 1f)

                // Wide atmospheric wash — base tint always visible, pulses with volume
                drawRect(brush = Brush.radialGradient(
                    listOf(
                        Color(0xFF7C6FFF).copy(alpha = 0.10f + rmsAmp * 0.50f),
                        Color(0xFF3A1F9F).copy(alpha = 0.05f + rmsAmp * 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = maxOf(w, h) * 1.0f
                ))
                // Bright centre — clearly reacts to each beat
                drawRect(brush = Brush.radialGradient(
                    listOf(
                        Color(0xFFD0C8FF).copy(alpha = 0.05f + rmsAmp * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.45f
                ))
            }
            BackgroundEffect.NOISE -> {
                drawRect(color = Color(0xFF050508))
                // Two layers: a coarse slow-changing layer for depth,
                // and a fine fast layer for grain texture
                val rngSlow = kotlin.random.Random(capturedNowMs / 120L) // changes ~8fps
                val rngFast = kotlin.random.Random(capturedNowMs / 33L)  // changes ~30fps

                // Layer 1: large vivid blobs — clearly visible, slow drift
                repeat(80) {
                    val nx = rngSlow.nextFloat() * w
                    val ny = rngSlow.nextFloat() * h
                    val nc = Color(
                        red   = rngSlow.nextFloat(),
                        green = rngSlow.nextFloat(),
                        blue  = rngSlow.nextFloat(),
                        alpha = 0.25f + rngSlow.nextFloat() * 0.35f  // 0.25–0.60
                    )
                    drawCircle(
                        color  = nc,
                        radius = 6f + rngSlow.nextFloat() * 14f,  // 6–20px
                        center = Offset(nx, ny)
                    )
                }
                // Layer 2: fine grain on top for texture
                repeat(300) {
                    val nx = rngFast.nextFloat() * w
                    val ny = rngFast.nextFloat() * h
                    val nc = Color(
                        red   = rngFast.nextFloat(),
                        green = rngFast.nextFloat(),
                        blue  = rngFast.nextFloat(),
                        alpha = 0.15f + rngFast.nextFloat() * 0.20f  // 0.15–0.35
                    )
                    drawCircle(
                        color  = nc,
                        radius = 2f + rngFast.nextFloat() * 3f,  // 2–5px
                        center = Offset(nx, ny)
                    )
                }
            }
            BackgroundEffect.STARFIELD -> {
                drawRect(color = Color(0xFF050508))
                stars.forEach { star ->
                    star.y += star.speed
                    if (star.y > 1f) { star.y = 0f; star.x = kotlin.random.Random.nextFloat() }
                    val cx = star.x * w
                    val cy = star.y * h
                    // Soft glow halo behind each star
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.12f),
                        radius = star.size * 2.5f,
                        center = Offset(cx, cy)
                    )
                    // Bright core
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.75f + star.size * 0.03f),
                        radius = star.size,
                        center = Offset(cx, cy)
                    )
                }
            }
            BackgroundEffect.NONE -> drawRect(color = Color(0xFF050508))
            BackgroundEffect.TERRAIN -> {
                drawRect(color = Color(0xFF020408))  // very dark blue-black base
                drawTerrain(
                    terrainHistory = terrainHistory,
                    terrainColors  = terrainColors,
                    bandCount      = bandCount,
                    canvasW        = w,
                    canvasH        = h
                )
            }
            BackgroundEffect.JULIA -> {
                drawRect(color = Color(0xFF000000))
                val bmp = juliaBitmap
                if (bmp != null) {
                    drawImage(
                        image       = bmp,
                        dstOffset   = androidx.compose.ui.unit.IntOffset.Zero,
                        dstSize     = androidx.compose.ui.unit.IntSize(w.toInt(), h.toInt()),
                        blendMode   = BlendMode.Screen
                    )
                }
            }
        }

        val laneW = w / bandCount
        for (i in 1 until bandCount) {
            drawLine(
                color       = Color.White.copy(alpha = 0.04f),
                start       = Offset(i * laneW, 0f),
                end         = Offset(i * laneW, h),
                strokeWidth = 1f
            )
        }

        // ── 4b. Peak frequency label — floats above loudest shape ─────────────
        val loudestShape = circles.maxByOrNull { it.decibelLevel }
        if (loudestShape != null) {
            val lx = loudestShape.x * w
            val ly = (loudestShape.y * h - loudestShape.radiusPx - 14f).coerceAtLeast(24f)
            val labelHz = if (loudestShape.centreHz >= 1000f)
                "${"%.1f".format(loudestShape.centreHz / 1000f)} kHz"
            else "${"%.0f".format(loudestShape.centreHz)} Hz"
            val life = loudestShape.lifeFraction(nowMs)
            val labelAlpha = (life * 200).toInt().coerceIn(60, 200)
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = loudestShape.color.copy(alpha = 0f).toArgb()
                        .let { android.graphics.Color.argb(labelAlpha,
                            android.graphics.Color.red(loudestShape.color.toArgb()),
                            android.graphics.Color.green(loudestShape.color.toArgb()),
                            android.graphics.Color.blue(loudestShape.color.toArgb())) }
                    textSize  = 28f
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface  = android.graphics.Typeface.MONOSPACE
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(labelHz, lx, ly, paint)
            }
        }

        // ── 4b2. Ribbon shapes — flowing silk wave per frequency band ─────────
        if (shape == ObjectShape.RIBBON) {
            circles.forEach { circle ->
                val b       = circle.bandIndex.coerceIn(0, MAX_RIBBON_BANDS - 1)
                val life    = circle.lifeFraction(nowMs)
                if (life <= 0f) return@forEach

                val alpha   = (if (life > 0.4f) 1f else life / 0.4f) * shapeOpacity
                val col     = shiftedColor(circle)

                // Ribbon geometry
                val bandCx  = circle.x * w + ribbonDriftX[b]   // column centre + drift
                val bandCy  = circle.y * h + ribbonDriftY[b]

                // Amplitude driven by dB — quiet=gentle sway, loud=wild flapping
                val ampNorm = ((circle.decibelLevel - (-80f)) / 80f).coerceIn(0f, 1f)
                val amplitude = ampNorm * circle.radiusPx * 1.4f

                // Max half-width of ribbon (sub-band energy adds shimmer)
                val energy   = circle.subBandEnergies.average().toFloat()
                val maxWidth = circle.radiusPx * (0.25f + energy * 0.35f)

                // Wave undulation frequency — bass=slow, treble=tight
                val waveFreq = 0.5f + b * 0.25f   // cycles per ribbon length

                // Ribbon length — tall strip, slightly tilted per band
                val ribbonLen = h * 0.65f
                val tiltAngle = (b - bandCount / 2f) * 0.04f  // slight diagonal tilt

                // Build ribbon as a closed polygon: 20 control points
                val N = 20
                val topEdgeX = FloatArray(N)
                val topEdgeY = FloatArray(N)
                val botEdgeX = FloatArray(N)
                val botEdgeY = FloatArray(N)

                for (i in 0 until N) {
                    val t      = i.toFloat() / (N - 1)   // 0 → 1 along ribbon
                    // Taper: sin(t*PI) — zero at tips, max at centre
                    val taper  = kotlin.math.sin(t * PI.toFloat()).coerceIn(0f, 1f)
                    val hw     = maxWidth * taper          // half-width at this point

                    // Spine position along ribbon
                    val spineT = t - 0.5f                  // -0.5 → +0.5
                    val sx     = bandCx + spineT * ribbonLen * kotlin.math.sin(tiltAngle)
                    val sy     = bandCy + spineT * ribbonLen * kotlin.math.cos(tiltAngle)

                    // Sine wave lateral displacement
                    val phase  = ribbonPhase[b] + t * waveFreq * 2f * PI.toFloat()
                    val disp   = amplitude * kotlin.math.sin(phase).toFloat()

                    // Perpendicular to ribbon spine (rotated 90°)
                    val perpX  = kotlin.math.cos(tiltAngle).toFloat()
                    val perpY  = -kotlin.math.sin(tiltAngle).toFloat()

                    // Centre of ribbon at this point
                    val cx3    = sx + disp * perpX
                    val cy3    = sy + disp * perpY

                    // Top and bottom edges
                    topEdgeX[i] = cx3 - hw * perpY   // perpendicular offset
                    topEdgeY[i] = cy3 + hw * perpX
                    botEdgeX[i] = cx3 + hw * perpY
                    botEdgeY[i] = cy3 - hw * perpX
                }

                // Build closed polygon path
                val path = Path()
                path.moveTo(topEdgeX[0], topEdgeY[0])
                // Forward along top edge
                for (i in 1 until N) path.lineTo(topEdgeX[i], topEdgeY[i])
                // Back along bottom edge
                for (i in N - 1 downTo 0) path.lineTo(botEdgeX[i], botEdgeY[i])
                path.close()

                // Draw glow fill
                drawPath(path,
                    color     = col.copy(alpha = alpha * 0.30f),
                    blendMode = BlendMode.Screen)
                // Draw bright outline edges
                val edgePath = Path()
                edgePath.moveTo(topEdgeX[0], topEdgeY[0])
                for (i in 1 until N) edgePath.lineTo(topEdgeX[i], topEdgeY[i])
                drawPath(edgePath,
                    color     = col.copy(alpha = alpha * 0.85f),
                    style     = Stroke(width = 1.8f, cap = StrokeCap.Round),
                    blendMode = BlendMode.Screen)
                val botPath = Path()
                botPath.moveTo(botEdgeX[0], botEdgeY[0])
                for (i in 1 until N) botPath.lineTo(botEdgeX[i], botEdgeY[i])
                drawPath(botPath,
                    color     = col.copy(alpha = alpha * 0.85f),
                    style     = Stroke(width = 1.8f, cap = StrokeCap.Round),
                    blendMode = BlendMode.Screen)
            }
        }

        // ── 4c. Vector shapes — spawn every 1s, draw spinning arrows ─────────
        if (shape == ObjectShape.VECTOR) {
            // Spawn new vectors every ~1 second
            if (nowMs - lastVectorSpawnMs > 1000L) {
                lastVectorSpawnMs = nowMs
                circles.forEach { c ->
                    val halfLen = ((c.decibelLevel - (-80f)) / 80f).coerceIn(0f, 1f) *
                                  (c.radiusPx * 1.2f).coerceIn(20f, 180f)
                    if (halfLen > 8f) {
                        // Rotation speed varies by band — bass slow, treble fast
                        val rotSpeed = 0.6f + c.bandIndex * 0.12f
                        // Initial angle randomised so vectors don't all point the same way
                        val initAngle = (c.bandIndex * 1.1f + nowMs * 0.0001f) % (2f * Math.PI.toFloat())
                        vectors.add(VectorShape(
                            bandIndex     = c.bandIndex,
                            centreX       = c.x,
                            centreY       = c.y,
                            halfLength    = halfLen,
                            color         = c.color,
                            spawnTimeMs   = nowMs,
                            rotationSpeed = rotSpeed,
                            initialAngle  = initAngle
                        ))
                    }
                }
                // Expire dead vectors
                val dead = vectors.filter { !it.isAlive(nowMs) }
                vectors.removeAll(dead)
                // Cap at 200
                while (vectors.size > 200) vectors.removeAt(0)
            }
            // Draw all live vectors
            vectors.forEach { v ->
                if (!v.isAlive(nowMs)) return@forEach
                val a = v.alpha(nowMs)
                val angle = v.currentAngle(nowMs)
                val cx2 = v.centreX * w
                val cy2 = v.centreY * h
                val cosA = kotlin.math.cos(angle)
                val sinA = kotlin.math.sin(angle)
                val tipX = cx2 + cosA * v.halfLength
                val tipY = cy2 + sinA * v.halfLength
                val tailX = cx2 - cosA * v.halfLength
                val tailY = cy2 - sinA * v.halfLength

                // Main vector line with glow
                drawLine(
                    color       = v.color.copy(alpha = a * 0.3f),
                    start       = Offset(tailX, tailY),
                    end         = Offset(tipX, tipY),
                    strokeWidth = v.halfLength * 0.12f,
                    blendMode   = BlendMode.Screen
                )
                // Bright core line
                drawLine(
                    color       = v.color.copy(alpha = a * 0.9f),
                    start       = Offset(tailX, tailY),
                    end         = Offset(tipX, tipY),
                    strokeWidth = 2.5f,
                    cap         = StrokeCap.Round,
                    blendMode   = BlendMode.Screen
                )
                // Arrow head — two lines at 140° from tip direction
                val headLen = v.halfLength * 0.28f
                val headAngle1 = angle + 2.44f  // 140 degrees
                val headAngle2 = angle - 2.44f
                drawLine(
                    color       = v.color.copy(alpha = a * 0.9f),
                    start       = Offset(tipX, tipY),
                    end         = Offset(tipX + kotlin.math.cos(headAngle1) * headLen,
                                         tipY + kotlin.math.sin(headAngle1) * headLen),
                    strokeWidth = 2f, cap = StrokeCap.Round, blendMode = BlendMode.Screen
                )
                drawLine(
                    color       = v.color.copy(alpha = a * 0.9f),
                    start       = Offset(tipX, tipY),
                    end         = Offset(tipX + kotlin.math.cos(headAngle2) * headLen,
                                         tipY + kotlin.math.sin(headAngle2) * headLen),
                    strokeWidth = 2f, cap = StrokeCap.Round, blendMode = BlendMode.Screen
                )
                // Centre dot — marks the pivot point
                drawCircle(
                    color     = v.color.copy(alpha = a * 0.7f),
                    radius    = 4f,
                    center    = Offset(cx2, cy2),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // ── 5. Particle explosions ────────────────────────────────────────────
        if (particlesEnabled) {
            val dt = 0.016f  // ~60fps timestep
            // Spawn particles on transients (per circle)
            circles.forEach { circle ->
                val bandIdx = circle.bandIndex.coerceIn(0, prevBandRms.size - 1)
                val rms     = circle.decibelLevel / -80f  // normalise dBFS to 0-1
                val prev    = prevBandRms[bandIdx]
                if (rms - prev > particleThreshold && rms > 0.1f) {
                    // Emit a burst of 8 particles
                    repeat(8) {
                        val angle = kotlin.random.Random.nextFloat() * 2f * PI.toFloat()
                        val speed = 1.5f + kotlin.random.Random.nextFloat() * 3f
                        particles.add(Particle(
                            x = circle.x * w, y = circle.y * h,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed,
                            life  = 1f,
                            color = circle.color
                        ))
                    }
                }
                prevBandRms[bandIdx] = rms
            }
            // Update and draw live particles
            val toRemove = mutableListOf<Particle>()
            particles.forEach { p ->
                p.x    += p.vx;  p.y += p.vy
                p.vx   *= 0.92f; p.vy *= 0.92f  // drag
                p.life -= dt * 2.5f
                if (p.life <= 0f) { toRemove.add(p); return@forEach }
                drawCircle(
                    color  = p.color.copy(alpha = p.life * 0.85f),
                    radius = 3f + p.life * 4f,
                    center = Offset(p.x, p.y),
                    blendMode = BlendMode.Screen
                )
            }
            particles.removeAll(toRemove)
            // Cap particle count
            while (particles.size > 200) particles.removeAt(0)
        }

        // ── 6. Waveform overlay ───────────────────────────────────────────────
        if (showWaveform && waveformSamples.size > 1) {
            val waveH    = h * 1.5f              // full screen height (×10 of original 15%)
            val waveMid  = h * 0.5f              // centre of screen
            val stepX    = w / (waveformSamples.size - 1).toFloat()
            // Glow pass — wide soft halo
            for (i in 0 until waveformSamples.size - 1) {
                val x1 = i * stepX
                val x2 = (i + 1) * stepX
                val y1 = waveMid - waveformSamples[i]  * waveH * 0.45f
                val y2 = waveMid - waveformSamples[i+1] * waveH * 0.45f
                drawLine(
                    color       = Color(0xFF7C6FFF).copy(alpha = 0.25f),
                    start       = Offset(x1, y1),
                    end         = Offset(x2, y2),
                    strokeWidth = 8f
                )
            }
            // Core line — bright sharp stroke
            for (i in 0 until waveformSamples.size - 1) {
                val x1 = i * stepX
                val x2 = (i + 1) * stepX
                val y1 = waveMid - waveformSamples[i]  * waveH * 0.45f
                val y2 = waveMid - waveformSamples[i+1] * waveH * 0.45f
                drawLine(
                    color       = Color(0xFFAA99FF),
                    start       = Offset(x1, y1),
                    end         = Offset(x2, y2),
                    strokeWidth = 2f
                )
            }
        }

        // ── 4. Draw shapes with mirror mode ───────────────────────────────────
        // Mirror mode works by computing reflected coordinates directly.
        // NO canvas transforms are used.
        // Hue drift: when colorAnimSpeed > 0, shift each circle's colour by hueOffsetDeg.
        // Oscilloscope mode: shapes draw as pulsing rings instead of filled objects.

        fun shiftedColor(c: FrequencyCircle): Color {
            if (hueOffsetDeg == 0f) return c.color
            // Convert RGB to HSV, add offset, convert back
            val r = c.color.red; val g = c.color.green; val b = c.color.blue
            val max = maxOf(r, g, b); val min = minOf(r, g, b); val delta = max - min
            val h = when {
                delta == 0f -> 0f
                max == r    -> 60f * (((g - b) / delta) % 6f)
                max == g    -> 60f * (((b - r) / delta) + 2f)
                else        -> 60f * (((r - g) / delta) + 4f)
            }.let { if (it < 0f) it + 360f else it }
            val s = if (max == 0f) 0f else delta / max
            val v = max
            val newH = ((h + hueOffsetDeg) % 360f + 360f) % 360f
            val hi = (newH / 60f).toInt() % 6
            val f  = newH / 60f - hi
            val p  = v * (1f - s); val q = v * (1f - f * s); val t = v * (1f - (1f - f) * s)
            val (nr, ng, nb) = when (hi) {
                0 -> Triple(v, t, p); 1 -> Triple(q, v, p); 2 -> Triple(p, v, t)
                3 -> Triple(p, q, v); 4 -> Triple(t, p, v); else -> Triple(v, p, q)
            }
            return c.color.copy(red = nr, green = ng, blue = nb)
        }


        fun drawShapePulsed(circle: FrequencyCircle, life: Float) {
            val shifted = circle.copy(
                radiusPx = circle.radiusPx * animatedPulse,
                color    = shiftedColor(circle)
            )
            if (oscilloscopeMode) {
                // Ring mode: draw a stroke circle whose radius pulses with waveform phase
                val cx     = shifted.x * size.width
                val cy     = shifted.y * size.height
                val energy = shifted.subBandEnergies.average().toFloat()
                val pulse  = 0.6f + energy * 0.8f  // contract/expand with energy
                val r      = shifted.radiusPx * pulse
                val alpha  = (life * 0.9f).coerceIn(0f, 1f)
                // Outer glow ring
                drawCircle(
                    color     = shifted.color.copy(alpha = alpha * 0.25f),
                    radius    = r * 1.3f,
                    center    = Offset(cx, cy),
                    style     = Stroke(width = r * 0.4f),
                    blendMode = BlendMode.Screen
                )
                // Core ring
                drawCircle(
                    color     = shifted.color.copy(alpha = alpha),
                    radius    = r,
                    center    = Offset(cx, cy),
                    style     = Stroke(width = 2.5f),
                    blendMode = BlendMode.Screen
                )
            } else {
                drawShape(shifted, life, shape, angleRad)
            }
        }

        fun drawCircleAtPos(circle: FrequencyCircle, life: Float, cx: Float, cy: Float) {
            val reflected = circle.copy(
                x        = cx / w,
                y        = cy / h,
                radiusPx = circle.radiusPx * animatedPulse,
                color    = shiftedColor(circle)
            )
            drawShapePulsed(reflected, life)
        }

        circles.forEach { circle ->
            val life = circle.lifeFraction(nowMs)
            if (life <= 0f) return@forEach

            // Original position in pixels
            val ox = circle.x * w
            val oy = circle.y * h

            // Mirror position = reflected across centre
            val mx = w - ox
            val my = h - oy

            when (mirrorMode) {
                MirrorMode.OFF -> {
                    if (trailLength > 0) {
                        trailHistory.take(trailLength).forEachIndexed { gi, ghosts ->
                            val ta = (1f - (gi + 1f) / (trailLength + 1f)) * 0.55f
                            ghosts.filter { it.bandIndex == circle.bandIndex && it.slotIndex == circle.slotIndex }
                                .forEach { g ->
                                    val gl = g.lifeFraction(nowMs)
                                    if (gl > 0f) drawShapePulsed(g, gl * ta)
                                }
                        }
                    }
                    drawShapePulsed(circle, life)
                }
                MirrorMode.HORIZONTAL -> {
                    drawShapePulsed(circle, life)
                    drawCircleAtPos(circle, life, mx, oy)
                    if (trailLength > 0) {
                        trailHistory.take(trailLength).forEachIndexed { gi, ghosts ->
                            val ta = (1f - (gi + 1f) / (trailLength + 1f)) * 0.55f
                            ghosts.filter { it.bandIndex == circle.bandIndex && it.slotIndex == circle.slotIndex }
                                .forEach { g ->
                                    val gl = g.lifeFraction(nowMs)
                                    if (gl > 0f) {
                                        drawShapePulsed(g, gl * ta)
                                        drawCircleAtPos(g, gl * ta, w - g.x * w, g.y * h)
                                    }
                                }
                        }
                    }
                }
                MirrorMode.VERTICAL -> {
                    drawShapePulsed(circle, life)
                    drawCircleAtPos(circle, life, ox, my)
                    if (trailLength > 0) {
                        trailHistory.take(trailLength).forEachIndexed { gi, ghosts ->
                            val ta = (1f - (gi + 1f) / (trailLength + 1f)) * 0.55f
                            ghosts.filter { it.bandIndex == circle.bandIndex && it.slotIndex == circle.slotIndex }
                                .forEach { g ->
                                    val gl = g.lifeFraction(nowMs)
                                    if (gl > 0f) {
                                        drawShapePulsed(g, gl * ta)
                                        drawCircleAtPos(g, gl * ta, g.x * w, h - g.y * h)
                                    }
                                }
                        }
                    }
                }
                MirrorMode.QUAD -> {
                    drawShapePulsed(circle, life)
                    drawCircleAtPos(circle, life, mx, oy)
                    drawCircleAtPos(circle, life, ox, my)
                    drawCircleAtPos(circle, life, mx, my)
                    if (trailLength > 0) {
                        trailHistory.take(trailLength).forEachIndexed { gi, ghosts ->
                            val ta = (1f - (gi + 1f) / (trailLength + 1f)) * 0.55f
                            ghosts.filter { it.bandIndex == circle.bandIndex && it.slotIndex == circle.slotIndex }
                                .forEach { g ->
                                    val gl = g.lifeFraction(nowMs)
                                    if (gl > 0f) {
                                        val gox = g.x * w;  val goy = g.y * h
                                        val gmx = w - gox;  val gmy = h - goy
                                        drawShapePulsed(g, gl * ta)
                                        drawCircleAtPos(g, gl * ta, gmx, goy)
                                        drawCircleAtPos(g, gl * ta, gox, gmy)
                                        drawCircleAtPos(g, gl * ta, gmx, gmy)
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

// ── Oscilloscope ring drawing helper ─────────────────────────────────────────
// When oscilloscopeMode is on, drawShape is replaced by this ring renderer
// called with the same circle — the radius oscillates with waveform phase.

// ── Sub-band gradient builder ─────────────────────────────────────────────────
/**
 * Build a radial gradient brush from [subBandEnergies].
 *
 * Each sub-band maps to one colour stop in the gradient:
 *   - Ring 0 (innermost) = lowest frequency sub-band
 *   - Ring N (outermost) = highest frequency sub-band
 *
 * The colour is always the shape's [baseColor]. Brightness is scaled by
 * the sub-band's energy (0 = near-black, 1 = full brightness), so active
 * frequency slices glow and silent ones are dark.
 *
 * A white hot-spot at the very centre gives every shape a lit-from-inside feel.
 */
private fun subBandGradient(
    cx:              Float,
    cy:              Float,
    radius:          Float,
    baseColor:       Color,
    alpha:           Float,
    subBandEnergies: FloatArray
): Brush {
    val n = subBandEnergies.size.coerceAtLeast(1)
    val stops = buildList {
        // White centre highlight
        add(Color.White.copy(alpha = alpha * 0.55f))
        // One colour stop per sub-band (lowest freq = inside, highest = outside)
        for (i in 0 until n) {
            val energy = subBandEnergies[i]
            // Interpolate: low energy → dark version of colour, high → bright
            val brightness = 0.15f + energy * 0.85f
            add(baseColor.copy(
                red   = (baseColor.red   * brightness).coerceIn(0f, 1f),
                green = (baseColor.green * brightness).coerceIn(0f, 1f),
                blue  = (baseColor.blue  * brightness).coerceIn(0f, 1f),
                alpha = alpha * (0.3f + energy * 0.65f)
            ))
        }
        // Transparent outer edge
        add(Color.Transparent)
    }
    return Brush.radialGradient(
        colors = stops,
        center = Offset(cx, cy),
        radius = radius
    )
}

// ── Shape dispatcher ──────────────────────────────────────────────────────────
private fun DrawScope.drawShape(
    circle:   FrequencyCircle,
    life:     Float,
    shape:    ObjectShape,
    angleRad: Float
) {
    val cx    = circle.x * size.width
    val cy    = circle.y * size.height
    val r     = circle.radiusPx
    val alpha = (if (life > 0.6f) 1f else (life / 0.6f).coerceIn(0f, 1f)) * shapeOpacity

    // Fractal depth driven by dB level: quiet=1, loud=4
    val fractalDepth = when {
        circle.decibelLevel > -15f -> 4
        circle.decibelLevel > -30f -> 3
        circle.decibelLevel > -50f -> 2
        else -> 1
    }
    when (shape) {
        ObjectShape.CIRCLE            -> drawCircleShape(cx, cy, r, circle, alpha)
        ObjectShape.STAR              -> drawStarShape(cx, cy, r, circle, alpha)
        ObjectShape.BOX_2D            -> drawBox2DShape(cx, cy, r, circle, alpha)
        ObjectShape.BOX_3D            -> drawBox3DShape(cx, cy, r, circle, alpha, angleRad)
        ObjectShape.SPHERE            -> drawSphereShape(cx, cy, r, circle, alpha, angleRad)
        ObjectShape.FRACTAL_KOCH      -> drawKochShape(cx, cy, r, circle, alpha, fractalDepth)
        ObjectShape.FRACTAL_SIERPINSKI -> drawSierpinskiShape(cx, cy, r, circle, alpha, fractalDepth)
        ObjectShape.FRACTAL_DRAGON    -> drawDragonShape(cx, cy, r, circle, alpha, fractalDepth)
        ObjectShape.VECTOR            -> { /* Vectors drawn in dedicated section above */ }
        ObjectShape.RIBBON            -> { /* Ribbons drawn in dedicated section above */ }
    }
}

// ── CIRCLE ────────────────────────────────────────────────────────────────────
private fun DrawScope.drawCircleShape(
    cx: Float, cy: Float, r: Float,
    circle: FrequencyCircle, alpha: Float
) {
    val centre = Offset(cx, cy)
    // Outer glow (Screen blend, colour only, no sub-band shading)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(circle.color.copy(alpha = alpha * 0.22f), Color.Transparent),
            center = centre, radius = r * 2.4f
        ),
        radius = r * 2.4f, center = centre, blendMode = BlendMode.Screen
    )
    // Core disc — sub-band radial shading
    drawCircle(
        brush = subBandGradient(cx, cy, r, circle.color, alpha, circle.subBandEnergies),
        radius = r, center = centre, blendMode = BlendMode.Screen
    )
}

// ── STAR ──────────────────────────────────────────────────────────────────────
private fun DrawScope.drawStarShape(
    cx: Float, cy: Float, r: Float,
    circle: FrequencyCircle, alpha: Float
) {
    val outerR = r
    val innerR = r * 0.42f
    val points = 5
    val path   = Path()
    for (i in 0 until points * 2) {
        val angle  = (i * PI / points - PI / 2).toFloat()
        val radius = if (i % 2 == 0) outerR else innerR
        val x = cx + cos(angle) * radius
        val y = cy + sin(angle) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    // Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(circle.color.copy(alpha = alpha * 0.18f), Color.Transparent),
            center = Offset(cx, cy), radius = r * 1.8f
        ),
        radius = r * 1.8f, center = Offset(cx, cy), blendMode = BlendMode.Screen
    )
    // Fill with sub-band gradient — gradient originates at star centre
    drawPath(
        path  = path,
        brush = subBandGradient(cx, cy, r, circle.color, alpha, circle.subBandEnergies),
        blendMode = BlendMode.Screen
    )
    // Outline
    drawPath(path = path, color = circle.color.copy(alpha = alpha * 0.7f),
        style = Stroke(width = 1.5f), blendMode = BlendMode.Screen)
}

// ── BOX 2D ────────────────────────────────────────────────────────────────────
private fun DrawScope.drawBox2DShape(
    cx: Float, cy: Float, r: Float,
    circle: FrequencyCircle, alpha: Float
) {
    val half = r * 0.78f
    val tl   = Offset(cx - half, cy - half)
    val br   = Offset(cx + half, cy + half)

    // Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(circle.color.copy(alpha = alpha * 0.18f), Color.Transparent),
            center = Offset(cx, cy), radius = r * 1.8f
        ),
        radius = r * 1.8f, center = Offset(cx, cy), blendMode = BlendMode.Screen
    )
    // Filled square with sub-band radial shading (radial gradient from centre)
    val path = Path().apply {
        moveTo(tl.x, tl.y); lineTo(br.x, tl.y)
        lineTo(br.x, br.y); lineTo(tl.x, br.y); close()
    }
    drawPath(
        path  = path,
        brush = subBandGradient(cx, cy, r, circle.color, alpha, circle.subBandEnergies),
        blendMode = BlendMode.Screen
    )
    // Outline
    val sw  = 1.8f
    val col = circle.color.copy(alpha = alpha * 0.9f)
    drawLine(col, tl, Offset(br.x, tl.y), sw, StrokeCap.Round)
    drawLine(col, Offset(br.x, tl.y), br, sw, StrokeCap.Round)
    drawLine(col, br, Offset(tl.x, br.y), sw, StrokeCap.Round)
    drawLine(col, Offset(tl.x, br.y), tl, sw, StrokeCap.Round)
}

// ── BOX 3D ────────────────────────────────────────────────────────────────────
private fun DrawScope.drawBox3DShape(
    cx: Float, cy: Float, r: Float,
    circle: FrequencyCircle, alpha: Float, angleRad: Float
) {
    val s = r * 0.65f
    val unitVerts = arrayOf(
        floatArrayOf(-1f,-1f,-1f), floatArrayOf( 1f,-1f,-1f),
        floatArrayOf( 1f, 1f,-1f), floatArrayOf(-1f, 1f,-1f),
        floatArrayOf(-1f,-1f, 1f), floatArrayOf( 1f,-1f, 1f),
        floatArrayOf( 1f, 1f, 1f), floatArrayOf(-1f, 1f, 1f)
    )
    val yaw = angleRad; val pitch = 0.52f
    val cosY = cos(yaw); val sinY = sin(yaw)
    val cosX = cos(pitch); val sinX = sin(pitch)

    val projected = Array(8) { i ->
        val v   = unitVerts[i]
        val rx  = v[0] * cosY + v[2] * sinY
        val ry2 = v[1] * cosX - (-v[0] * sinY + v[2] * cosY) * sinX
        val rz2 = v[1] * sinX + (-v[0] * sinY + v[2] * cosY) * cosX
        val sc  = 4f / (4f + rz2 + 2f)
        Offset(cx + rx * s * sc, cy + ry2 * s * sc)
    }

    // Sub-band gradient glow bubble behind the box
    drawCircle(
        brush = subBandGradient(cx, cy, r * 1.8f, circle.color, alpha * 0.4f, circle.subBandEnergies),
        radius = r * 1.8f, center = Offset(cx, cy), blendMode = BlendMode.Screen
    )

    val edges = arrayOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 0,
        4 to 5, 5 to 6, 6 to 7, 7 to 4,
        0 to 4, 1 to 5, 2 to 6, 3 to 7
    )
    edges.forEach { (a, b) ->
        val avgZ   = (unitVerts[a][2] + unitVerts[b][2]) / 2f
        val bright = (0.4f + (avgZ + 1f) * 0.3f).coerceIn(0.3f, 1f)
        // Tint edge colour by sub-band energy of the corresponding band slice
        val subIdx = ((avgZ + 1f) / 2f * (circle.subBandEnergies.size - 1)).toInt()
            .coerceIn(0, circle.subBandEnergies.size - 1)
        val energy = circle.subBandEnergies[subIdx]
        drawLine(
            color       = circle.color.copy(alpha = alpha * bright * (0.3f + energy * 0.7f)),
            start       = projected[a], end = projected[b],
            strokeWidth = 2f, cap = StrokeCap.Round, blendMode = BlendMode.Screen
        )
    }
    projected.forEach { pt ->
        drawCircle(color = circle.color.copy(alpha = alpha * 0.7f),
            radius = 2.5f, center = pt, blendMode = BlendMode.Screen)
    }
}

// ── SPHERE ────────────────────────────────────────────────────────────────────
private fun DrawScope.drawSphereShape(
    cx: Float, cy: Float, r: Float,
    circle: FrequencyCircle, alpha: Float, angleRad: Float
) {
    val centre = Offset(cx, cy)

    // Base sphere disc — full sub-band radial shading
    drawCircle(
        brush = subBandGradient(cx, cy, r, circle.color, alpha, circle.subBandEnergies),
        radius = r, center = centre, blendMode = BlendMode.Screen
    )
    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(circle.color.copy(alpha = alpha * 0.2f), Color.Transparent),
            center = centre, radius = r * 2.2f
        ),
        radius = r * 2.2f, center = centre, blendMode = BlendMode.Screen
    )

    // Latitude lines — brightness driven by sub-band energy of their radial position
    val latAngles = listOf(-0.5f, 0f, 0.5f)
    latAngles.forEachIndexed { li, lat ->
        val lineR  = r * cos(lat)
        val lineY  = cy + r * sin(lat)
        val subIdx = (li.toFloat() / 2f * (circle.subBandEnergies.size - 1)).toInt()
            .coerceIn(0, circle.subBandEnergies.size - 1)
        val energy = circle.subBandEnergies[subIdx]
        val path   = Path()
        for (i in 0..64) {
            val a  = i.toFloat() / 64f * 2f * PI.toFloat() + angleRad
            val px = cx + lineR * cos(a)
            val py = lineY + lineR * 0.12f * sin(a)
            if (((px - cx).pow(2) + (py - cy).pow(2)) > r * r * 1.02f) {
                path.rewind(); return@forEachIndexed
            }
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, circle.color.copy(alpha = alpha * (0.25f + energy * 0.5f)),
            style = Stroke(width = 1.2f), blendMode = BlendMode.Screen)
    }

    // Longitude lines — brightness driven by sub-band energy
    listOf(0f, PI.toFloat() / 3f, 2f * PI.toFloat() / 3f).forEachIndexed { li, lonOff ->
        val lon    = angleRad + lonOff
        val subIdx = (li.toFloat() / 2f * (circle.subBandEnergies.size - 1)).toInt()
            .coerceIn(0, circle.subBandEnergies.size - 1)
        val energy = circle.subBandEnergies[subIdx]
        val path   = Path()
        for (i in 0..64) {
            val t  = i.toFloat() / 64f * 2f * PI.toFloat()
            val x3 = cos(t) * cos(lon)
            val z3 = cos(t) * sin(lon)
            val y3 = sin(t)
            if (z3 < 0f) { path.rewind(); return@forEachIndexed }
            val px = cx + x3 * r; val py = cy + y3 * r
            if (i == 0 || path.isEmpty) path.moveTo(px, py) else path.lineTo(px, py)
        }
        if (!path.isEmpty)
            drawPath(path, circle.color.copy(alpha = alpha * (0.25f + energy * 0.5f)),
                style = Stroke(width = 1.2f), blendMode = BlendMode.Screen)
    }
}

private fun Float.pow(n: Int): Float = Math.pow(this.toDouble(), n.toDouble()).toFloat()

// ── Top HUD ───────────────────────────────────────────────────────────────────
@Composable
private fun TopHud(
    rmsVolume: Float, activeCount: Int, bandCount: Int,
    peakHz: String, peakDb: String, bpm: Float,
    onSettings: () -> Unit, modifier: Modifier = Modifier
) {
    Row(modifier, Arrangement.SpaceBetween, Alignment.CenterVertically) {
        val blink = rememberInfiniteTransition(label = "blink")
        val dotAlpha by blink.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "d"
        )
        Column(horizontalAlignment = Alignment.Start) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp)
                    .background(Color(0xFFFF4444).copy(alpha = dotAlpha), CircleShape))
                Spacer(Modifier.width(6.dp))
                Text("LIVE", color = UiSubtle, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(2.dp))
            Text("$activeCount / $bandCount bands", color = UiText,
                fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            if (bpm > 0f) {
                Spacer(Modifier.height(2.dp))
                Text("${bpm.toInt()} BPM", color = UiAccent,
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VOL", color = UiSubtle, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            VolumeBar(rmsVolume, Modifier.width(80.dp).height(6.dp))
            Spacer(Modifier.height(6.dp))
            RmsHistoryGraph(rmsVolume, Modifier.width(80.dp).height(28.dp))
        }
        Column(horizontalAlignment = Alignment.End) {
            IconButton(onClick = onSettings,
                modifier = Modifier.size(32.dp).align(Alignment.End)) {
                Text("⚙", color = UiSubtle, fontSize = 18.sp)
            }
            Text(peakHz, color = UiText, fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(peakDb, color = UiSubtle, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace)
        }
    }
}

// ── Tablet stat row ──────────────────────────────────────────────────────────
@Composable
private fun TabletStatRow(label: String, value: String, theme: ChromaColors) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, color = theme.uiSubtle, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Text(value, color = theme.uiText, fontSize = 13.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = theme.uiSubtle.copy(alpha = 0.1f), thickness = 1.dp)
}

// ── RMS history graph ────────────────────────────────────────────────────────
// Scrolling bar chart of the last ~3 seconds of RMS volume.
// Each audio frame (~93ms) appends one bar; we keep 32 bars total.
@Composable
private fun RmsHistoryGraph(rmsVolume: Float, modifier: Modifier = Modifier) {
    val history = remember { ArrayDeque<Float>(32) }
    // Append current volume each recomposition (each audio frame)
    history.addLast(rmsVolume)
    if (history.size > 32) history.removeFirst()

    Canvas(modifier = modifier) {
        val w     = size.width
        val h     = size.height
        val n     = history.size
        val barW  = w / 32f
        val gap   = barW * 0.25f
        // Faint baseline
        drawLine(Color.White.copy(alpha = 0.06f),
            Offset(0f, h), Offset(w, h), strokeWidth = 1f)
        // Bars — oldest on left, newest on right
        history.forEachIndexed { i, rms ->
            val x      = i * barW + gap / 2f
                        // Amplify 8x — raw RMS is typically 0.002-0.1, invisible at 1x
            val amp    = (rms * 8f).coerceIn(0f, 1f)
            val barH   = (amp * h).coerceAtLeast(2f)
            val alpha  = 0.5f + (i.toFloat() / 32f) * 0.5f  // fade older bars
            val color  = when {
                amp > 0.7f -> Color(0xFFFF6B6B).copy(alpha = alpha) // loud  = red
                amp > 0.3f -> Color(0xFF7C6FFF).copy(alpha = alpha) // mid   = purple
                else       -> Color(0xFF42E5F5).copy(alpha = alpha) // quiet = cyan
            }
            drawRect(color = color,
                topLeft    = Offset(x, h - barH),
                size       = androidx.compose.ui.geometry.Size(barW - gap, barH))
        }
    }
}

@Composable
private fun VolumeBar(volume: Float, modifier: Modifier = Modifier) {
    val animVol by animateFloatAsState(
        targetValue = volume,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "vol"
    )
    Box(modifier.clip(RoundedCornerShape(50)).background(UiSubtle.copy(alpha = 0.25f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animVol.coerceIn(0f, 1f))
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(
                listOf(Color(0xFF42E5F5), Color(0xFF7C6FFF), Color(0xFFFF6B6B))
            ))
        )
    }
}

@Composable
private fun PermissionDeniedScreen() {
    Column(Modifier.padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚠", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Microphone\nPermission Denied", color = UiText, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Grant microphone access in\nSettings to use ChromaSound.",
            color = UiSubtle, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center)
    }
}

// ── FRACTAL: Koch Snowflake ───────────────────────────────────────────────────
// A Koch snowflake of depth driven by dB level (1-4).
// The more intense the frequency, the more detailed the snowflake.

private fun DrawScope.drawKochShape(
    cx: Float, cy: Float, r: Float,
    circle: FrequencyCircle, alpha: Float, depth: Int
) {
    val path = Path()
    // Start with equilateral triangle vertices
    val h = r * 0.866f  // sqrt(3)/2
    val ax = cx;        val ay = cy - r
    val bx = cx + h;    val by = cy + r * 0.5f
    val ex = cx - h;    val ey = by

    path.moveTo(ax, ay)
    addKochEdge(path, ax, ay, bx, by, depth)
    addKochEdge(path, bx, by, ex, ey, depth)
    addKochEdge(path, ex, ey, ax, ay, depth)
    path.close()

    // Glow fill
    drawPath(path, color = circle.color.copy(alpha = alpha * 0.35f),
        blendMode = BlendMode.Screen)
    // Bright outline
    drawPath(path, color = circle.color.copy(alpha = alpha),
        style = Stroke(width = 1.8f), blendMode = BlendMode.Screen)
}

private fun addKochEdge(path: Path, x1: Float, y1: Float, x2: Float, y2: Float, depth: Int) {
    if (depth == 0) { path.lineTo(x2, y2); return }
    val dx = x2 - x1; val dy = y2 - y1
    // Divide edge into thirds
    val ax = x1 + dx/3f;       val ay = y1 + dy/3f
    val bx = x1 + 2f*dx/3f;   val by = y1 + 2f*dy/3f
    // Apex of equilateral triangle on this edge
    val px = ax + (bx - ax) * 0.5f - (by - ay) * 0.866f
    val py = ay + (by - ay) * 0.5f + (bx - ax) * 0.866f
    addKochEdge(path, x1, y1, ax, ay, depth - 1)
    addKochEdge(path, ax, ay, px, py, depth - 1)
    addKochEdge(path, px, py, bx, by, depth - 1)
    addKochEdge(path, bx, by, x2, y2, depth - 1)
}

// ── FRACTAL: Sierpinski Triangle ─────────────────────────────────────────────
// Recursive triangle subdivision. Depth 1-4 driven by dB.

private fun DrawScope.drawSierpinskiShape(
    cx: Float, cy: Float, r: Float,
    circle: FrequencyCircle, alpha: Float, depth: Int
) {
    val h = r * 0.866f
    val ax = cx;      val ay = cy - r
    val bx = cx + h;  val by = cy + r * 0.5f
    val ex = cx - h;  val ey = by
    drawSierpinskiTriangle(ax, ay, bx, by, ex, ey, depth, circle.color, alpha)
}

private fun DrawScope.drawSierpinskiTriangle(
    ax: Float, ay: Float,
    bx: Float, by: Float,
    cx: Float, cy: Float,
    depth: Int, color: androidx.compose.ui.graphics.Color, alpha: Float
) {
    if (depth == 0) {
        val path = Path()
        path.moveTo(ax, ay); path.lineTo(bx, by); path.lineTo(cx, cy); path.close()
        drawPath(path, color = color.copy(alpha = alpha * 0.5f),
            blendMode = BlendMode.Screen)
        drawPath(path, color = color.copy(alpha = alpha),
            style = Stroke(width = 1.5f), blendMode = BlendMode.Screen)
        return
    }
    val mABx = (ax + bx) * 0.5f; val mABy = (ay + by) * 0.5f
    val mBCx = (bx + cx) * 0.5f; val mBCy = (by + cy) * 0.5f
    val mCAx = (cx + ax) * 0.5f; val mCAy = (cy + ay) * 0.5f
    drawSierpinskiTriangle(ax, ay, mABx, mABy, mCAx, mCAy, depth-1, color, alpha)
    drawSierpinskiTriangle(mABx, mABy, bx, by, mBCx, mBCy, depth-1, color, alpha)
    drawSierpinskiTriangle(mCAx, mCAy, mBCx, mBCy, cx, cy, depth-1, color, alpha)
}

// ── FRACTAL: Dragon Curve ─────────────────────────────────────────────────────
// The Heighway dragon curve — builds a list of turns iteratively.
// Depth 8-12 driven by dB. Centered and scaled to fit within radius r.

private fun DrawScope.drawDragonShape(
    cx: Float, cy: Float, r: Float,
    circle: FrequencyCircle, alpha: Float, depth: Int
) {
    // Map depth 1-4 to dragon iterations 6-12
    val iter = depth * 3 + 3  // depth1=6, depth4=12
    // Build dragon curve turns (0=left, 1=right)
    val turns = mutableListOf<Int>()
    turns.add(1)
    repeat(iter - 1) {
        val last = turns.toList()
        turns.add(1)
        last.reversed().forEachIndexed { i, t -> turns.add(1 - t) }
    }
    // Walk the curve
    val stepLen = r * 1.4f / kotlin.math.sqrt(turns.size.toFloat())
    var x = cx - stepLen * turns.size * 0.25f
    var y = cy
    var angle = 0f  // in quarter-turns: 0=right,1=up,2=left,3=down
    val pts = mutableListOf(Pair(x, y))
    turns.forEach { turn ->
        angle = ((angle + if (turn == 1) 1 else 3).toInt() % 4).toFloat()
        val (dx, dy) = when (angle.toInt()) {
            0 -> Pair(stepLen, 0f);  1 -> Pair(0f, -stepLen)
            2 -> Pair(-stepLen, 0f); else -> Pair(0f, stepLen)
        }
        x += dx; y += dy
        pts.add(Pair(x, y))
    }
    // Centre the curve
    val minX = pts.minOf { it.first }; val maxX = pts.maxOf { it.first }
    val minY = pts.minOf { it.second }; val maxY = pts.maxOf { it.second }
    val offX = cx - (minX + maxX) * 0.5f
    val offY = cy - (minY + maxY) * 0.5f

    val path = Path()
    path.moveTo(pts[0].first + offX, pts[0].second + offY)
    pts.drop(1).forEach { (px, py) -> path.lineTo(px + offX, py + offY) }
    drawPath(path, color = circle.color.copy(alpha = alpha),
        style = Stroke(width = 1.5f, cap = StrokeCap.Round),
        blendMode = BlendMode.Screen)
}

// ── Julia Set renderer ────────────────────────────────────────────────────────
// Renders a Julia set into an IntArray pixel buffer.
// cx, cy = complex seed (driven by peak frequency + RMS volume).
// Colourised using the escape-time algorithm mapped to the shape's accent colour.

fun renderJulia(pixels: IntArray, w: Int, h: Int, cx: Float, cy: Float) {
    val maxIter = 48
    val scale   = 3.2f  // view window width
    for (py in 0 until h) {
        for (px in 0 until w) {
            // Map pixel to complex plane [-scale/2, scale/2]
            var zx = (px.toFloat() / w - 0.5f) * scale
            var zy = (py.toFloat() / h - 0.5f) * scale * (h.toFloat() / w)
            var iter = 0
            while (iter < maxIter && zx * zx + zy * zy < 4f) {
                val tmp = zx * zx - zy * zy + cx
                zy = 2f * zx * zy + cy
                zx = tmp
                iter++
            }
            // Colour: smooth iteration count → purple/cyan gradient
            if (iter == maxIter) {
                pixels[py * w + px] = 0xFF000000.toInt()  // inside = black
            } else {
                val t = iter.toFloat() / maxIter
                // Gradient: deep purple → cyan → white at edges
                val r = (kotlin.math.sin(t * 3.14f * 2f) * 0.5f + 0.5f)
                val g = (t * t)
                val b = (kotlin.math.sin(t * 3.14f) * 0.8f + 0.2f)
                val ri = (r * 200).toInt().coerceIn(0, 255)
                val gi = (g * 160).toInt().coerceIn(0, 255)
                val bi = (b * 255).toInt().coerceIn(0, 255)
                pixels[py * w + px] = (0xFF shl 24) or (ri shl 16) or (gi shl 8) or bi
            }
        }
    }
}

// ── Terrain Renderer ──────────────────────────────────────────────────────────
//
// Renders a 3D perspective frequency terrain using the painter's algorithm.
// X axis = frequency bands (bass left → treble right)
// Y axis = dB level (height above ground plane)
// Z axis = time (far=old frames, near=newest frame scrolling toward viewer)
//
// Projection:
//   screenX = cx + (worldX / (1 + worldZ * perspective)) 
//   screenY = horizon - worldY * scale / (1 + worldZ * perspective)
//
private fun DrawScope.drawTerrain(
    terrainHistory: ArrayDeque<FloatArray>,
    terrainColors:  ArrayDeque<Array<androidx.compose.ui.graphics.Color>>,
    bandCount:      Int,
    canvasW:        Float,
    canvasH:        Float
) {
    if (terrainHistory.isEmpty()) return

    val rows       = terrainHistory.size
    val horizon    = canvasH * 0.52f      // horizon line — slightly above centre
    val maxHeight  = canvasH * 0.38f      // tallest peak reaches 38% of screen height
    val perspective = 0.06f               // how quickly things shrink with depth
    val cx         = canvasW * 0.5f       // centre X

    // Draw rows back-to-front (painter's algorithm: far first, near last)
    // terrainHistory[0] = newest (near), [rows-1] = oldest (far)
    for (rowIdx in rows - 1 downTo 0) {
        val heights     = terrainHistory[rowIdx]
        val rowColors   = terrainColors.getOrNull(rowIdx) ?: continue
        val numBands    = minOf(heights.size, bandCount).coerceAtLeast(1)

        // Z in world space: 0=near (newest), 1=far (oldest)
        val zNear = rowIdx.toFloat() / rows
        val zFar  = (rowIdx + 1).toFloat() / rows

        // Perspective scale factors
        val scaleNear = 1f / (1f + zNear * perspective * rows)
        val scaleFar  = 1f / (1f + zFar  * perspective * rows)

        // Alpha: far rows are dimmer and more blue-shifted
        val alpha = (1f - zNear * 0.7f).coerceIn(0.15f, 1f)

        for (band in 0 until numBands) {
            val hNear = heights[band]
            val hFar  = if (rowIdx + 1 < rows) terrainHistory[rowIdx + 1][band] else 0f

            // World X positions for this band (left edge and right edge)
            val bandFrac     = (band + 0.5f) / numBands
            val bandFracL    = band.toFloat() / numBands
            val bandFracR    = (band + 1f) / numBands
            val worldXLeft   = (bandFracL - 0.5f) * canvasW * 1.1f
            val worldXRight  = (bandFracR - 0.5f) * canvasW * 1.1f

            // Project the 4 corners of the quad to screen space
            // Near-left, near-right, far-right, far-left
            fun projX(wx: Float, scale: Float) = cx + wx * scale
            fun projYGround(scale: Float) = horizon + canvasH * 0.12f * (1f - scale * 2f)
            fun projYPeak(h: Float, scale: Float) = horizon - h * maxHeight * scale + canvasH * 0.12f * (1f - scale * 2f)

            val nlX = projX(worldXLeft,  scaleNear);  val nlY = projYPeak(hNear, scaleNear)
            val nrX = projX(worldXRight, scaleNear);  val nrY = projYPeak(hNear, scaleNear)
            val frX = projX(worldXRight, scaleFar);   val frY = projYPeak(hFar,  scaleFar)
            val flX = projX(worldXLeft,  scaleFar);   val flY = projYPeak(hFar,  scaleFar)

            // Ground-level corners (for solid quad fill down to ground)
            val nlGY = projYGround(scaleNear)
            val nrGY = projYGround(scaleNear)
            val frGY = projYGround(scaleFar)
            val flGY = projYGround(scaleFar)

            // Get colour for this band (from frequency mapper)
            val col = rowColors.getOrNull(band)
                ?: FrequencyColorMapper.colorForBand(band, 30f * (band + 1).toFloat())

            // Skip bands with no signal
            if (hNear < 0.005f && hFar < 0.005f) continue

            // Draw filled peak quad (peak to ground)
            val fillPath = Path()
            fillPath.moveTo(nlX, nlGY)  // near ground left
            fillPath.lineTo(nlX, nlY)   // near peak left
            fillPath.lineTo(nrX, nrY)   // near peak right
            fillPath.lineTo(nrX, nrGY)  // near ground right
            fillPath.lineTo(frX, frGY)  // far ground right
            fillPath.lineTo(frX, frY)   // far peak right
            fillPath.lineTo(flX, flY)   // far peak left
            fillPath.lineTo(flX, flGY)  // far ground left
            fillPath.close()

            // Fill with frequency colour, dimming with distance
            drawPath(
                path      = fillPath,
                color     = col.copy(alpha = alpha * 0.25f),
                blendMode = BlendMode.Screen
            )

            // Draw the peak ridge line (top edge of each column)
            val ridgePath = Path()
            ridgePath.moveTo(flX, flY)
            ridgePath.lineTo(nlX, nlY)
            ridgePath.lineTo(nrX, nrY)
            ridgePath.lineTo(frX, frY)

            drawPath(
                path      = ridgePath,
                color     = col.copy(alpha = alpha * 0.85f),
                style     = Stroke(width = 1.5f * scaleNear.coerceAtLeast(0.3f)),
                blendMode = BlendMode.Screen
            )
        }
    }

    // Draw the horizon grid line
    drawLine(
        color       = Color(0xFF7C6FFF).copy(alpha = 0.25f),
        start       = Offset(0f, horizon + canvasH * 0.12f),
        end         = Offset(canvasW, horizon + canvasH * 0.12f),
        strokeWidth = 1f
    )
}
