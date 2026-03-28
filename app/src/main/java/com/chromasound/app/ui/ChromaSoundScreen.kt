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
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    isDark:            Boolean  = true,
    modifier:          Modifier = Modifier
) {
    val theme = LocalChromaTheme.current
    var nowMs        by remember { mutableStateOf(System.currentTimeMillis()) }
    var angleRad     by remember { mutableStateOf(0f) }
    var hueOffsetDeg by remember { mutableStateOf(0f) }

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

    // ── Starfield state ───────────────────────────────────────────────────
    data class Star(var x: Float, var y: Float, val speed: Float, val size: Float)
    val stars = remember {
        List(80) { Star(
            x     = kotlin.random.Random.nextFloat(),
            y     = kotlin.random.Random.nextFloat(),
            speed = 0.00008f + kotlin.random.Random.nextFloat() * 0.00015f,
            size  = 0.8f + kotlin.random.Random.nextFloat() * 1.6f
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
                // Amplify RMS heavily — raw values are 0.002–0.05 so we need 20x to see anything
                val bloom = (rmsVolume * 20f).coerceIn(0f, 1f) * 0.18f
                drawRect(brush = Brush.radialGradient(
                    listOf(Color(0xFF7C6FFF).copy(alpha = bloom), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.8f
                ))
            }
            BackgroundEffect.NOISE -> {
                drawRect(color = Color(0xFF050508))
                val rng = kotlin.random.Random(nowMs / 33L)
                repeat(400) {
                    val nx = rng.nextFloat() * w
                    val ny = rng.nextFloat() * h
                    val nc = Color(
                        red   = rng.nextFloat(),
                        green = rng.nextFloat(),
                        blue  = rng.nextFloat(),
                        alpha = 0.08f + rng.nextFloat() * 0.12f
                    )
                    drawCircle(
                        color  = nc,
                        radius = 1.5f + rng.nextFloat() * 2.5f,
                        center = Offset(nx, ny)
                    )
                }
            }
            BackgroundEffect.STARFIELD -> {
                drawRect(color = Color(0xFF050508))
                stars.forEach { star ->
                    star.y += star.speed
                    if (star.y > 1f) { star.y = 0f; star.x = kotlin.random.Random.nextFloat() }
                    drawCircle(
                        color  = Color.White.copy(alpha = 0.25f + star.size * 0.15f),
                        radius = star.size,
                        center = Offset(star.x * w, star.y * h)
                    )
                }
            }
            BackgroundEffect.NONE -> drawRect(color = Color(0xFF050508))
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
    val alpha = if (life > 0.6f) 1f else (life / 0.6f).coerceIn(0f, 1f)

    when (shape) {
        ObjectShape.CIRCLE -> drawCircleShape(cx, cy, r, circle, alpha)
        ObjectShape.STAR   -> drawStarShape(cx, cy, r, circle, alpha)
        ObjectShape.BOX_2D -> drawBox2DShape(cx, cy, r, circle, alpha)
        ObjectShape.BOX_3D -> drawBox3DShape(cx, cy, r, circle, alpha, angleRad)
        ObjectShape.SPHERE -> drawSphereShape(cx, cy, r, circle, alpha, angleRad)
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
