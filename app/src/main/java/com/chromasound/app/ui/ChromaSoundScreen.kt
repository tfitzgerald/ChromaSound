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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromasound.app.model.FrequencyCircle
import com.chromasound.app.model.ObjectShape
import com.chromasound.app.model.Settings
import kotlin.math.*

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgColor  = Color(0xFF050508)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

// ── Root ──────────────────────────────────────────────────────────────────────
@Composable
fun ChromaSoundScreen(
    uiState: ChromaSoundUiState,
    settings: Settings,
    onStartRequested: () -> Unit,
    onStopRequested:  () -> Unit,
    onSettingsChange: (Settings) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            currentSettings  = settings,
            onSettingsChange = onSettingsChange,
            onClose          = { showSettings = false }
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(BgColor),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is ChromaSoundUiState.Running ->
                    RunningScreen(uiState, objectShape = settings.objectShape,
                        onStop = onStopRequested, onSettings = { showSettings = true })
                ChromaSoundUiState.PermissionDenied -> PermissionDeniedScreen()
                else -> IdleScreen(onStartRequested)
            }
        }
    }
}

// ── Idle screen ───────────────────────────────────────────────────────────────
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
            Modifier.size((120 * scale).dp)
                .background(
                    Brush.radialGradient(listOf(UiAccent.copy(alpha = 0.8f), Color.Transparent)),
                    CircleShape
                )
        )
        Spacer(Modifier.height(40.dp))
        Text("CHROMA SOUND", color = UiText, fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace, letterSpacing = 6.sp)
        Spacer(Modifier.height(8.dp))
        Text("30 Hz – 11 kHz  ·  0.5 s objects",
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

// ── Running screen ────────────────────────────────────────────────────────────
@Composable
private fun RunningScreen(
    state:       ChromaSoundUiState.Running,
    objectShape: ObjectShape,
    onStop:      () -> Unit,
    onSettings:  () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        BandLaneGrid(bandCount = state.bandCount, modifier = Modifier.fillMaxSize())
        ShapeCanvas(circles = state.circles, shape = objectShape,
            modifier = Modifier.fillMaxSize())
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

// ── Band lane grid ────────────────────────────────────────────────────────────
@Composable
private fun BandLaneGrid(bandCount: Int, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val laneW = size.width / bandCount
        for (i in 1 until bandCount) {
            drawLine(
                color = Color.White.copy(alpha = 0.04f),
                start = Offset(i * laneW, 0f),
                end   = Offset(i * laneW, size.height),
                strokeWidth = 1f
            )
        }
    }
}

// ── Shape canvas ──────────────────────────────────────────────────────────────
@Composable
private fun ShapeCanvas(
    circles:  List<FrequencyCircle>,
    shape:    ObjectShape,
    modifier: Modifier = Modifier
) {
    // nowMs drives lifetime fading; angleRad drives 3-D rotation
    var nowMs    by remember { mutableStateOf(System.currentTimeMillis()) }
    var angleRad by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { frameMs ->
                angleRad = (frameMs / 2000f * 2f * PI.toFloat()) % (2f * PI.toFloat())
                nowMs    = frameMs
            }
        }
    }

    Canvas(modifier = modifier) {
        circles.forEach { circle ->
            val life = circle.lifeFraction(nowMs)
            if (life > 0f) drawShape(circle, life, shape, angleRad)
        }
    }
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
    val col   = circle.color

    when (shape) {
        ObjectShape.CIRCLE  -> drawGlowCircle(cx, cy, r, col, alpha)
        ObjectShape.STAR    -> drawStar(cx, cy, r, col, alpha)
        ObjectShape.BOX_2D  -> drawBox2D(cx, cy, r, col, alpha)
        ObjectShape.BOX_3D  -> drawBox3D(cx, cy, r, col, alpha, angleRad)
        ObjectShape.SPHERE  -> drawSphere(cx, cy, r, col, alpha, angleRad)
    }
}

// ── CIRCLE ────────────────────────────────────────────────────────────────────
private fun DrawScope.drawGlowCircle(
    cx: Float, cy: Float, r: Float, col: Color, alpha: Float
) {
    val centre = Offset(cx, cy)
    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(col.copy(alpha = alpha * 0.22f), Color.Transparent),
            center = centre, radius = r * 2.4f
        ),
        radius = r * 2.4f, center = centre, blendMode = BlendMode.Screen
    )
    // Core disc
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.5f),
                col.copy(alpha = alpha * 0.95f),
                col.copy(alpha = alpha * 0.4f)
            ),
            center = centre, radius = r
        ),
        radius = r, center = centre, blendMode = BlendMode.Screen
    )
}

// ── STAR ──────────────────────────────────────────────────────────────────────
private fun DrawScope.drawStar(
    cx: Float, cy: Float, r: Float, col: Color, alpha: Float
) {
    val outerR = r
    val innerR = r * 0.42f
    val points = 5
    val path   = Path()

    for (i in 0 until points * 2) {
        val angle  = (i * PI / points - PI / 2).toFloat()
        val radius = if (i % 2 == 0) outerR else innerR
        val x      = cx + cos(angle) * radius
        val y      = cy + sin(angle) * radius
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    // Glow behind star
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(col.copy(alpha = alpha * 0.18f), Color.Transparent),
            center = Offset(cx, cy), radius = r * 1.8f
        ),
        radius = r * 1.8f, center = Offset(cx, cy), blendMode = BlendMode.Screen
    )
    // Filled star
    drawPath(
        path  = path,
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.6f),
                col.copy(alpha = alpha * 0.95f)
            ),
            center = Offset(cx, cy), radius = r
        ),
        blendMode = BlendMode.Screen
    )
    // Outline
    drawPath(
        path      = path,
        color     = col.copy(alpha = alpha * 0.7f),
        style     = Stroke(width = 1.5f),
        blendMode = BlendMode.Screen
    )
}

// ── BOX 2D ────────────────────────────────────────────────────────────────────
private fun DrawScope.drawBox2D(
    cx: Float, cy: Float, r: Float, col: Color, alpha: Float
) {
    val half = r * 0.78f   // square half-side
    val tl   = Offset(cx - half, cy - half)
    val tr   = Offset(cx + half, cy - half)
    val br   = Offset(cx + half, cy + half)
    val bl   = Offset(cx - half, cy + half)

    // Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(col.copy(alpha = alpha * 0.18f), Color.Transparent),
            center = Offset(cx, cy), radius = r * 1.8f
        ),
        radius = r * 1.8f, center = Offset(cx, cy), blendMode = BlendMode.Screen
    )
    // Filled square
    val path = Path().apply {
        moveTo(tl.x, tl.y); lineTo(tr.x, tr.y)
        lineTo(br.x, br.y); lineTo(bl.x, bl.y); close()
    }
    drawPath(
        path  = path,
        brush = Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = alpha * 0.4f), col.copy(alpha = alpha * 0.85f)),
            start  = tl, end = br
        ),
        blendMode = BlendMode.Screen
    )
    // Outline edges
    val strokeCol = col.copy(alpha = alpha * 0.9f)
    val sw        = 1.8f
    drawLine(strokeCol, tl, tr, sw, StrokeCap.Round)
    drawLine(strokeCol, tr, br, sw, StrokeCap.Round)
    drawLine(strokeCol, br, bl, sw, StrokeCap.Round)
    drawLine(strokeCol, bl, tl, sw, StrokeCap.Round)
}

// ── BOX 3D (rotating isometric wireframe) ────────────────────────────────────
private fun DrawScope.drawBox3D(
    cx: Float, cy: Float, r: Float, col: Color, alpha: Float, angleRad: Float
) {
    // Half-size of the cube
    val s = r * 0.65f

    // 8 unit-cube vertices: (±1, ±1, ±1)
    val unitVerts = arrayOf(
        floatArrayOf(-1f, -1f, -1f), floatArrayOf( 1f, -1f, -1f),
        floatArrayOf( 1f,  1f, -1f), floatArrayOf(-1f,  1f, -1f),
        floatArrayOf(-1f, -1f,  1f), floatArrayOf( 1f, -1f,  1f),
        floatArrayOf( 1f,  1f,  1f), floatArrayOf(-1f,  1f,  1f)
    )

    // Rotate around Y axis (spin) and tilt around X (isometric view)
    val yaw   = angleRad
    val pitch = 0.52f   // ~30° constant tilt — keeps cube readable

    val cosY = cos(yaw);  val sinY = sin(yaw)
    val cosX = cos(pitch); val sinX = sin(pitch)

    // Project 3-D → 2-D with simple perspective
    val projected = Array(8) { i ->
        val v  = unitVerts[i]
        // Rotate Y
        val rx = v[0] * cosY + v[2] * sinY
        val ry = v[1]
        val rz = -v[0] * sinY + v[2] * cosY
        // Rotate X
        val rx2 = rx
        val ry2 = ry * cosX - rz * sinX
        val rz2 = ry * sinX + rz * cosX
        // Perspective divide
        val depth = 4f   // viewer distance
        val scale = depth / (depth + rz2 + 2f)
        Offset(cx + rx2 * s * scale, cy + ry2 * s * scale)
    }

    // 12 edges of a cube: pairs of vertex indices
    val edges = arrayOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 0,  // back face
        4 to 5, 5 to 6, 6 to 7, 7 to 4,  // front face
        0 to 4, 1 to 5, 2 to 6, 3 to 7   // connecting edges
    )

    // Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(col.copy(alpha = alpha * 0.25f), Color.Transparent),
            center = Offset(cx, cy), radius = r * 2f
        ),
        radius = r * 2f, center = Offset(cx, cy), blendMode = BlendMode.Screen
    )

    // Draw edges with depth-based brightness (front edges brighter)
    edges.forEach { (a, b) ->
        val pA     = projected[a]
        val pB     = projected[b]
        // Average Z of edge — deeper = darker
        val avgRz  = (unitVerts[a][2] + unitVerts[b][2]) / 2f
        val bright = (0.4f + (avgRz + 1f) * 0.3f).coerceIn(0.3f, 1f)
        drawLine(
            color       = col.copy(alpha = alpha * bright),
            start       = pA,
            end         = pB,
            strokeWidth = 2f,
            cap         = StrokeCap.Round,
            blendMode   = BlendMode.Screen
        )
    }

    // Dot at each vertex
    projected.forEachIndexed { i, pt ->
        drawCircle(
            color     = col.copy(alpha = alpha * 0.7f),
            radius    = 2.5f,
            center    = pt,
            blendMode = BlendMode.Screen
        )
    }
}

// ── SPHERE (rotating latitude/longitude wireframe) ───────────────────────────
private fun DrawScope.drawSphere(
    cx: Float, cy: Float, r: Float, col: Color, alpha: Float, angleRad: Float
) {
    val centre = Offset(cx, cy)

    // Base sphere — shaded disc
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.45f),
                col.copy(alpha = alpha * 0.85f),
                col.copy(alpha = alpha * 0.2f)
            ),
            center = Offset(cx - r * 0.2f, cy - r * 0.2f),  // off-centre for 3-D lighting
            radius = r * 1.4f
        ),
        radius = r, center = centre, blendMode = BlendMode.Screen
    )

    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(col.copy(alpha = alpha * 0.2f), Color.Transparent),
            center = centre, radius = r * 2.2f
        ),
        radius = r * 2.2f, center = centre, blendMode = BlendMode.Screen
    )

    // Clip to sphere boundary by only drawing points inside radius
    // 3 latitude lines
    val latAngles = listOf(-0.5f, 0f, 0.5f)   // in radians above/below equator
    latAngles.forEach { lat ->
        val lineR  = r * cos(lat)               // projected circle radius
        val lineY  = cy + r * sin(lat)           // projected Y on screen
        val points = 64
        val path   = Path()
        for (i in 0..points) {
            val a = (i.toFloat() / points) * 2f * PI.toFloat() + angleRad
            val px = cx + lineR * cos(a)
            val py = lineY + lineR * 0.12f * sin(a)  // slight ellipse for depth
            val inside = ((px - cx) * (px - cx) + (py - cy) * (py - cy)) <= r * r * 1.02f
            if (!inside) { path.rewind(); return@forEach }
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, col.copy(alpha = alpha * 0.55f),
            style = Stroke(width = 1.2f), blendMode = BlendMode.Screen)
    }

    // 3 longitude lines (great circles, rotating)
    val lonOffsets = listOf(0f, PI.toFloat() / 3f, 2f * PI.toFloat() / 3f)
    lonOffsets.forEach { lonOff ->
        val lon   = angleRad + lonOff
        val path  = Path()
        val steps = 64
        for (i in 0..steps) {
            val t  = i.toFloat() / steps * 2f * PI.toFloat()
            val x3 = cos(t) * cos(lon)
            val z3 = cos(t) * sin(lon)
            val y3 = sin(t)
            // Only draw the front hemisphere (z3 >= 0)
            if (z3 < 0f) { if (path.isEmpty) {} else path.rewind(); return@forEach }
            val px = cx + x3 * r
            val py = cy + y3 * r
            if (i == 0 || path.isEmpty) path.moveTo(px, py) else path.lineTo(px, py)
        }
        if (!path.isEmpty)
            drawPath(path, col.copy(alpha = alpha * 0.55f),
                style = Stroke(width = 1.2f), blendMode = BlendMode.Screen)
    }
}

// ── Top HUD ───────────────────────────────────────────────────────────────────
@Composable
private fun TopHud(
    rmsVolume:   Float,
    activeCount: Int,
    bandCount:   Int,
    peakHz:      String,
    peakDb:      String,
    onSettings:  () -> Unit,
    modifier:    Modifier = Modifier
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
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "vol"
    )
    Box(modifier.clip(RoundedCornerShape(50))
        .background(UiSubtle.copy(alpha = 0.25f))) {
        Box(Modifier.fillMaxHeight()
            .fillMaxWidth(animVol.coerceIn(0f, 1f))
            .clip(RoundedCornerShape(50))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF42E5F5), Color(0xFF7C6FFF), Color(0xFFFF6B6B))
                )
            )
        )
    }
}

// ── Permission denied ─────────────────────────────────────────────────────────
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
