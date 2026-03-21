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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromasound.app.model.ColorBlotch

// ── Color palette ────────────────────────────────────────────────────────────

private val BgDeep   = Color(0xFF050508)
private val BgMid    = Color(0xFF0A0A14)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

// ── Main screen ──────────────────────────────────────────────────────────────

@Composable
fun ChromaSoundScreen(
    uiState: ChromaSoundUiState,
    onStartRequested: () -> Unit,
    onStopRequested: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is ChromaSoundUiState.Running -> RunningScreen(
                state = uiState,
                onStop = onStopRequested
            )
            ChromaSoundUiState.PermissionDenied -> PermissionDeniedScreen()
            else -> IdleScreen(onStart = onStartRequested)
        }
    }
}

// ── Idle / launch screen ─────────────────────────────────────────────────────

@Composable
private fun IdleScreen(onStart: () -> Unit) {
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        // Animated orb
        Box(
            modifier = Modifier
                .size((120 * scale).dp)
                .background(
                    Brush.radialGradient(listOf(UiAccent.copy(alpha = 0.8f), Color.Transparent)),
                    CircleShape
                )
        )
        Spacer(Modifier.height(40.dp))

        Text(
            "CHROMA SOUND",
            color = UiText,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 6.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Sound → Spectrum → Color",
            color = UiSubtle,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(56.dp))

        Button(
            onClick = onStart,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = UiAccent),
            modifier = Modifier.fillMaxWidth(0.65f).height(52.dp)
        ) {
            Text(
                "TAP TO LISTEN",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                fontSize = 13.sp
            )
        }
    }
}

// ── Running screen ───────────────────────────────────────────────────────────

@Composable
private fun RunningScreen(
    state: ChromaSoundUiState.Running,
    onStop: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ── Blotch canvas (bottom layer) ─────────────────────────────────────
        BlotchCanvas(
            blotches = state.blotches,
            modifier = Modifier.fillMaxSize()
        )

        // ── Top HUD strip ────────────────────────────────────────────────────
        TopHud(
            rmsVolume = state.rmsVolume,
            peakFrequency = state.peakFrequencyLabel,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 48.dp, start = 20.dp, end = 20.dp)
        )

        // ── Stop button ──────────────────────────────────────────────────────
        IconStopButton(
            onClick = onStop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
        )
    }
}

// ── Blotch canvas ────────────────────────────────────────────────────────────

@Composable
private fun BlotchCanvas(
    blotches: List<ColorBlotch>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        blotches.forEach { blotch ->
            drawBlotch(blotch, w, h)
        }
    }
}

private fun DrawScope.drawBlotch(blotch: ColorBlotch, canvasW: Float, canvasH: Float) {
    val cx = blotch.x * canvasW
    val cy = blotch.y * canvasH
    val r  = blotch.radius * canvasW

    // Life controls the overall fade-out
    val lifeAlpha = blotch.life

    // Glow: large transparent outer circle
    val glowColor = blotch.color.copy(alpha = blotch.color.alpha * lifeAlpha * 0.35f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = Offset(cx, cy),
            radius = r * 2.2f
        ),
        radius = r * 2.2f,
        center = Offset(cx, cy),
        blendMode = BlendMode.Screen
    )

    // Core: solid(ish) inner blotch
    val coreColor = blotch.color.copy(alpha = blotch.color.alpha * lifeAlpha)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                coreColor,
                coreColor.copy(alpha = coreColor.alpha * 0.4f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = r
        ),
        radius = r,
        center = Offset(cx, cy),
        blendMode = BlendMode.Screen
    )
}

// ── Top HUD ──────────────────────────────────────────────────────────────────

@Composable
private fun TopHud(
    rmsVolume: Float,
    peakFrequency: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Volume meter
        Column(horizontalAlignment = Alignment.Start) {
            Text("VOL", color = UiSubtle, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Spacer(Modifier.height(4.dp))
            VolumeBar(rmsVolume, Modifier.width(100.dp).height(6.dp))
        }

        // Live recording dot
        val blinkAnim = rememberInfiniteTransition(label = "blink")
        val alpha by blinkAnim.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "alpha"
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp)
                    .background(Color(0xFFFF4444).copy(alpha = alpha), CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text("LIVE", color = UiSubtle, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }

        // Peak frequency
        Column(horizontalAlignment = Alignment.End) {
            Text("PEAK", color = UiSubtle, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Text(
                peakFrequency.ifEmpty { "—" },
                color = UiText,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VolumeBar(volume: Float, modifier: Modifier = Modifier) {
    val animVolume by animateFloatAsState(
        targetValue = volume, animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "vol"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(UiSubtle.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animVolume.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF42E5F5), Color(0xFF7C6FFF), Color(0xFFFF6B6B))
                    )
                )
        )
    }
}

// ── Stop button ──────────────────────────────────────────────────────────────

@Composable
private fun IconStopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
        border = androidx.compose.foundation.BorderStroke(1.dp, UiSubtle)
    ) {
        Text(
            "■  STOP",
            fontFamily = FontFamily.Monospace,
            letterSpacing = 3.sp,
            fontSize = 12.sp
        )
    }
}

// ── Permission denied screen ─────────────────────────────────────────────────

@Composable
private fun PermissionDeniedScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(40.dp)
    ) {
        Text("⚠", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Microphone\nPermission Denied",
            color = UiText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Grant microphone permission in\nSettings to use ChromaSound.",
            color = UiSubtle,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}
