package com.chromasound.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromasound.app.model.FrequencyCircle
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
    uiState:          ChromaSoundUiState,
    settings:         Settings,
    onStartRequested: () -> Unit,
    onStopRequested:  () -> Unit,
    onSettingsChange: (Settings) -> Unit
) {
    var showSettings   by remember { mutableStateOf(false) }
    var showBandColors by remember { mutableStateOf(false) }
    var showHelp       by remember { mutableStateOf(false) }
    var showPresets    by remember { mutableStateOf(false) }

    when {
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
                        state       = uiState,
                        objectShape = settings.objectShape,
                        mirrorMode  = settings.mirrorMode,
                        trailLength = settings.trailLength,
                        onStop      = onStopRequested,
                        onSettings  = { showSettings = true }
                    )
                ChromaSoundUiState.PermissionDenied -> PermissionDeniedScreen()
                else -> IdleScreen(onStartRequested)
            }
        }
    }
}

// ── Idle ──────────────────────────────────────────────────────────────────────
@Composable
private fun IdleScreen(onStart: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "s"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
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
    }
}

// ── Running ───────────────────────────────────────────────────────────────────
@Composable
private fun RunningScreen(
    state:       ChromaSoundUiState.Running,
    objectShape: ObjectShape,
    mirrorMode:  MirrorMode,
    trailLength: Int,
    onStop:      () -> Unit,
    onSettings:  () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        // Single canvas for both lane grid and shapes — one clear covers both layers
        VisualizerCanvas(
            circles     = state.circles,
            bandCount   = state.bandCount,
            shape       = objectShape,
            mirrorMode  = mirrorMode,
            trailLength = trailLength,
            modifier    = Modifier.fillMaxSize()
        )
        TopHud(
            rmsVolume   = state.rmsVolume,
            activeCount = state.activeCount,
            bandCount   = state.bandCount,
            peakHz      = state.peakHz,
            peakDb      = state.peakDb,
            onSettings  = onSettings,
            modifier    = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                .padding(top = 52.dp, start = 20.dp, end = 20.dp)
        )
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
            border = androidx.compose.foundation.BorderStroke(1.dp, UiSubtle)
        ) {
            Text("■  STOP", fontFamily = FontFamily.Monospace,
                letterSpacing = 3.sp, fontSize = 12.sp)
        }
    }
}

// ── Unified visualiser canvas ─────────────────────────────────────────────────
// A single Canvas composable draws everything — background clear, band lane
// grid lines, trail ghosts, and live shapes — in the correct order.
// Using one Canvas means one drawRect() clears everything atomically each frame,
// preventing any layer from accumulating across recompositions.
@Composable
private fun VisualizerCanvas(
    circles:     List<FrequencyCircle>,
    bandCount:   Int,
    shape:       ObjectShape,
    mirrorMode:  MirrorMode,
    trailLength: Int,
    modifier:    Modifier = Modifier
) {
    var nowMs    by remember { mutableStateOf(System.currentTimeMillis()) }
    var angleRad by remember { mutableStateOf(0f) }

    // Trail history: a ring buffer of recent circle snapshots.
    // Each entry is a full List<FrequencyCircle> captured at that frame.
    // Index 0 = most recent past frame, MAX_TRAIL_LENGTH-1 = oldest.
    val trailHistory = remember {
        ArrayDeque<List<FrequencyCircle>>(Settings.MAX_TRAIL_LENGTH)
    }
    // Track the previous circles list so we only push when it changes
    var prevCircles by remember { mutableStateOf<List<FrequencyCircle>>(emptyList()) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { frameMs ->
                angleRad = (frameMs / 2000f * 2f * PI.toFloat()) % (2f * PI.toFloat())
                nowMs    = frameMs
            }
        }
    }

    // Update trail history whenever circles list changes
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

        // ── 1. Clear entire canvas every frame ────────────────────────────────
        // Single drawRect covers all layers — no previous frame content survives.
        drawRect(color = Color(0xFF050508))

        // ── 2. Band lane grid lines ───────────────────────────────────────────
        val laneW = w / bandCount
        for (i in 1 until bandCount) {
            drawLine(
                color       = Color.White.copy(alpha = 0.04f),
                start       = Offset(i * laneW, 0f),
                end         = Offset(i * laneW, h),
                strokeWidth = 1f
            )
        }

        // ── 3. Helper: draw all circles (trails + live) with optional flip ────
        fun drawAll(flipX: Boolean, flipY: Boolean) {
            // Draw trail ghosts first (behind live shapes)
            if (trailLength > 0) {
                val visibleTrail = trailHistory.take(trailLength)
                visibleTrail.forEachIndexed { ghostIndex, ghostCircles ->
                    // ghostIndex 0 = most recent trail frame, most opaque
                    val trailAlpha = (1f - (ghostIndex + 1f) / (trailLength + 1f)) * 0.55f
                    ghostCircles.forEach { circle ->
                        val life = circle.lifeFraction(nowMs)
                        if (life > 0f) {
                            val ghostCircle = if (flipX || flipY) {
                                circle.copy(
                                    x = if (flipX) 1f - circle.x else circle.x,
                                    y = if (flipY) 1f - circle.y else circle.y
                                )
                            } else circle
                            drawShape(ghostCircle, life * trailAlpha, shape, angleRad)
                        }
                    }
                }
            }

            // Draw live shapes
            circles.forEach { circle ->
                val life = circle.lifeFraction(nowMs)
                if (life > 0f) {
                    val drawn = if (flipX || flipY) {
                        circle.copy(
                            x = if (flipX) 1f - circle.x else circle.x,
                            y = if (flipY) 1f - circle.y else circle.y
                        )
                    } else circle
                    drawShape(drawn, life, shape, angleRad)
                }
            }
        }

        // Apply mirror mode via canvas transforms
        when (mirrorMode) {
            MirrorMode.OFF -> {
                drawAll(flipX = false, flipY = false)
            }
            MirrorMode.HORIZONTAL -> {
                // Original left half
                drawAll(flipX = false, flipY = false)
                // Mirrored right half — scale X by -1 around canvas centre
                withTransform({
                    scale(scaleX = -1f, scaleY = 1f, pivot = Offset(w / 2f, h / 2f))
                }) {
                    drawAll(flipX = false, flipY = false)
                }
            }
            MirrorMode.VERTICAL -> {
                drawAll(flipX = false, flipY = false)
                withTransform({
                    scale(scaleX = 1f, scaleY = -1f, pivot = Offset(w / 2f, h / 2f))
                }) {
                    drawAll(flipX = false, flipY = false)
                }
            }
            MirrorMode.QUAD -> {
                // Top-left quadrant: restrict circles to left half, top half
                drawAll(flipX = false, flipY = false)
                // Top-right: mirror horizontally
                withTransform({
                    scale(scaleX = -1f, scaleY = 1f, pivot = Offset(w / 2f, h / 2f))
                }) {
                    drawAll(flipX = false, flipY = false)
                }
                // Bottom-left: mirror vertically
                withTransform({
                    scale(scaleX = 1f, scaleY = -1f, pivot = Offset(w / 2f, h / 2f))
                }) {
                    drawAll(flipX = false, flipY = false)
                }
                // Bottom-right: mirror both
                withTransform({
                    scale(scaleX = -1f, scaleY = -1f, pivot = Offset(w / 2f, h / 2f))
                }) {
                    drawAll(flipX = false, flipY = false)
                }
            }
        }
    }
}

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
    peakHz: String, peakDb: String, onSettings: () -> Unit, modifier: Modifier = Modifier
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
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VOL", color = UiSubtle, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            VolumeBar(rmsVolume, Modifier.width(80.dp).height(6.dp))
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
