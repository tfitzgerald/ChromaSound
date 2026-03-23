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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromasound.app.model.FrequencyCircle

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgColor  = Color(0xFF050508)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

// ── Root — handles Settings navigation ───────────────────────────────────────
@Composable
fun ChromaSoundScreen(
    uiState: ChromaSoundUiState,
    bandCount: Int,
    onStartRequested: () -> Unit,
    onStopRequested:  () -> Unit,
    onBandCountChange: (Int) -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            currentBandCount  = bandCount,
            onBandCountChange = onBandCountChange,
            onClose           = { showSettings = false }
        )
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(BgColor),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is ChromaSoundUiState.Running ->
                    RunningScreen(
                        state    = uiState,
                        onStop   = onStopRequested,
                        onSettings = { showSettings = true }
                    )
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
        Text("30 Hz – 11 kHz  ·  0.5 s circles",
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
    state: ChromaSoundUiState.Running,
    onStop: () -> Unit,
    onSettings: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {

        BandLaneGrid(bandCount = state.bandCount, modifier = Modifier.fillMaxSize())
        CircleCanvas(circles = state.circles, modifier = Modifier.fillMaxSize())

        TopHud(
            rmsVolume   = state.rmsVolume,
            activeCount = state.activeCount,
            bandCount   = state.bandCount,
            peakHz      = state.peakHz,
            peakDb      = state.peakDb,
            onSettings  = onSettings,
            modifier    = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
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
            val x = i * laneW
            drawLine(
                color = Color.White.copy(alpha = 0.04f),
                start = Offset(x, 0f),
                end   = Offset(x, size.height),
                strokeWidth = 1f
            )
        }
    }
}

// ── Circle canvas ─────────────────────────────────────────────────────────────
@Composable
private fun CircleCanvas(circles: List<FrequencyCircle>, modifier: Modifier = Modifier) {
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { withFrameMillis { nowMs = it } } }

    Canvas(modifier = modifier) {
        circles.forEach { circle ->
            val life = circle.lifeFraction(nowMs)
            if (life > 0f) drawFrequencyCircle(circle, life)
        }
    }
}

private fun DrawScope.drawFrequencyCircle(circle: FrequencyCircle, lifeFraction: Float) {
    val cx = circle.x * size.width
    val cy = circle.y * size.height
    val r  = circle.radiusPx
    val alpha = if (lifeFraction > 0.6f) 1f else (lifeFraction / 0.6f).coerceIn(0f, 1f)

    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(circle.color.copy(alpha = alpha * 0.22f), Color.Transparent),
            center = Offset(cx, cy), radius = r * 2.4f
        ),
        radius = r * 2.4f, center = Offset(cx, cy), blendMode = BlendMode.Screen
    )
    // Core disc
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.5f),
                circle.color.copy(alpha = alpha * 0.95f),
                circle.color.copy(alpha = alpha * 0.4f)
            ),
            center = Offset(cx, cy), radius = r
        ),
        radius = r, center = Offset(cx, cy), blendMode = BlendMode.Screen
    )
}

// ── Top HUD ───────────────────────────────────────────────────────────────────
@Composable
private fun TopHud(
    rmsVolume: Float,
    activeCount: Int,
    bandCount: Int,
    peakHz: String,
    peakDb: String,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, Arrangement.SpaceBetween, Alignment.CenterVertically) {

        // Live dot + band count
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

        // Volume bar
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("VOL", color = UiSubtle, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            VolumeBar(rmsVolume, Modifier.width(80.dp).height(6.dp))
        }

        // Peak + settings gear
        Column(horizontalAlignment = Alignment.End) {
            // ⚙ settings button — small, top-right aligned
            IconButton(
                onClick    = onSettings,
                modifier   = Modifier.size(32.dp).align(Alignment.End)
            ) {
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
