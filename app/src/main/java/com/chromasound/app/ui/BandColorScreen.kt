package com.chromasound.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromasound.app.fft.FrequencyColorMapper
import com.chromasound.app.model.BandDefinition
import com.chromasound.app.model.ColorScheme
import com.chromasound.app.model.Settings
import kotlin.math.*

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgColor  = Color(0xFF050508)
private val BgCard   = Color(0xFF0F0F1A)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Displays every frequency band as a row with a colour swatch.
 * Tapping a swatch opens an inline HSV colour picker for that band.
 * Changes are pushed to [onSettingsChange] in real time.
 *
 * The picker works entirely with Compose Canvas — no external library required.
 */
@Composable
fun BandColorScreen(
    currentSettings:  Settings,
    onSettingsChange: (Settings) -> Unit,
    onClose:          () -> Unit
) {
    val bands = remember(currentSettings.bandCount) {
        BandDefinition.build(currentSettings.bandCount)
    }

    // Local mutable copy of all band color overrides
    val overrides = remember(currentSettings.bandColors) {
        mutableStateMapOf<Int, Color>().also { it.putAll(currentSettings.bandColors) }
    }

    // Which band index has its picker open (-1 = none)
    var activeBand by remember { mutableStateOf(-1) }

    fun emitOverrides() {
        onSettingsChange(currentSettings.copy(bandColors = overrides.toMap()))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgColor)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(56.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, UiSubtle),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("← BACK", fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("BAND COLOURS", color = UiText, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                Spacer(Modifier.weight(1f))
                // Reset all overrides button
                TextButton(onClick = {
                    overrides.clear()
                    activeBand = -1
                    emitOverrides()
                }) {
                    Text("RESET", color = UiSubtle, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap a colour swatch to override it.\nLeave bands at auto to use the selected colour scheme.",
                color = UiSubtle, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── One row per band ──────────────────────────────────────────────────
        itemsIndexed((0 until bands.count).toList()) { _, bandIdx ->
            val autoColor = FrequencyColorMapper.frequencyToColor(
                bands.centreHz[bandIdx], currentSettings.colorScheme
            )
            val activeColor = overrides[bandIdx] ?: autoColor
            val isOverridden = overrides.containsKey(bandIdx)
            val isPickerOpen = activeBand == bandIdx

            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(BgCard, RoundedCornerShape(12.dp))
            ) {
                // ── Band row ──────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { activeBand = if (isPickerOpen) -1 else bandIdx }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Band number badge
                    Box(
                        Modifier.size(32.dp)
                            .background(UiAccent.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${bandIdx + 1}", color = UiAccent, fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))

                    // Frequency range
                    Column(Modifier.weight(1f)) {
                        Text(
                            formatBandHz(bands.lowerHz[bandIdx]) + " – " +
                            formatBandHz(bands.upperHz[bandIdx]),
                            color = UiText, fontSize = 12.sp, fontFamily = FontFamily.Monospace
                        )
                        if (isOverridden) {
                            Text("CUSTOM", color = UiAccent, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        } else {
                            Text("AUTO", color = UiSubtle, fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                        }
                    }

                    // Colour swatch — shows current colour, tapping opens picker
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                            .then(
                                if (isPickerOpen)
                                    Modifier.border(2.dp, UiText, CircleShape)
                                else
                                    Modifier.border(1.dp, UiSubtle.copy(alpha = 0.5f), CircleShape)
                            )
                    )

                    // Reset button (only when overridden)
                    if (isOverridden) {
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                overrides.remove(bandIdx)
                                if (activeBand == bandIdx) activeBand = -1
                                emitOverrides()
                            },
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("✕", color = UiSubtle, fontSize = 14.sp)
                        }
                    } else {
                        Spacer(Modifier.width(40.dp))
                    }
                }

                // ── Inline HSV colour picker (visible when this band is active) ──
                if (isPickerOpen) {
                    HsvColorPicker(
                        initialColor = activeColor,
                        onColorChanged = { newColor ->
                            overrides[bandIdx] = newColor
                            emitOverrides()
                        },
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 16.dp, end = 16.dp, bottom = 16.dp
                        )
                    )
                }
            }
        }

        item { Spacer(Modifier.height(40.dp)) }
    }
}

// ── HSV Colour Picker ─────────────────────────────────────────────────────────

/**
 * A fully self-contained HSV colour picker drawn with Compose Canvas.
 * Consists of:
 *   1. Hue bar       — horizontal gradient strip, drag to set hue (0–360°)
 *   2. SV square     — 2D gradient, drag to set saturation (X) and value (Y)
 *   3. Preview swatch — shows the selected colour with its hex value
 *
 * No external library required.
 */
@Composable
private fun HsvColorPicker(
    initialColor:   Color,
    onColorChanged: (Color) -> Unit,
    modifier:       Modifier = Modifier
) {
    // Decompose initial colour into HSV
    val initHsv = remember(initialColor) { colorToHsv(initialColor) }
    var hue  by remember { mutableStateOf(initHsv[0]) }
    var sat  by remember { mutableStateOf(initHsv[1]) }
    var value by remember { mutableStateOf(initHsv[2]) }

    // Notify caller whenever any component changes
    LaunchedEffect(hue, sat, value) {
        onColorChanged(FrequencyColorMapper.hsvToColor(hue, sat, value))
    }

    val pickedColor = FrequencyColorMapper.hsvToColor(hue, sat, value)
    val hueColor    = FrequencyColorMapper.hsvToColor(hue, 1f, 1f)

    Column(modifier = modifier) {

        // ── 1. Hue bar ────────────────────────────────────────────────────────
        Text("HUE", color = UiSubtle, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val barW = constraints.maxWidth.toFloat()
            val barH = 28.dp

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barH)
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            hue = (offset.x / barW * 360f).coerceIn(0f, 360f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            hue = (change.position.x / barW * 360f).coerceIn(0f, 360f)
                        }
                    }
            ) {
                // Hue rainbow gradient
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.hsv(0f,   1f, 1f),
                            Color.hsv(60f,  1f, 1f),
                            Color.hsv(120f, 1f, 1f),
                            Color.hsv(180f, 1f, 1f),
                            Color.hsv(240f, 1f, 1f),
                            Color.hsv(300f, 1f, 1f),
                            Color.hsv(360f, 1f, 1f)
                        )
                    )
                )
                // Thumb indicator
                val thumbX = hue / 360f * size.width
                drawLine(Color.White, Offset(thumbX, 2f),
                    Offset(thumbX, size.height - 2f), 3f, StrokeCap.Round)
                drawLine(Color.Black.copy(alpha = 0.4f), Offset(thumbX + 3f, 2f),
                    Offset(thumbX + 3f, size.height - 2f), 1f)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── 2. SV (Saturation × Value) square ────────────────────────────────
        Text("SATURATION  ×  BRIGHTNESS", color = UiSubtle, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Spacer(Modifier.height(4.dp))

        BoxWithConstraints(
            Modifier.fillMaxWidth().aspectRatio(1.6f)
        ) {
            val sqW = constraints.maxWidth.toFloat()
            val sqH = constraints.maxHeight.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            sat   = (offset.x / sqW).coerceIn(0f, 1f)
                            value = (1f - offset.y / sqH).coerceIn(0f, 1f)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            sat   = (change.position.x / sqW).coerceIn(0f, 1f)
                            value = (1f - change.position.y / sqH).coerceIn(0f, 1f)
                        }
                    }
            ) {
                drawSvSquare(hueColor, size)
                // Crosshair thumb
                val tx = sat   * size.width
                val ty = (1f - value) * size.height
                drawCircle(Color.White, 8f, Offset(tx, ty),
                    style = Stroke(width = 2.5f))
                drawCircle(Color.Black.copy(alpha = 0.5f), 10f, Offset(tx, ty),
                    style = Stroke(width = 1f))
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── 3. Preview swatch + hex ───────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                    .background(pickedColor)
                    .border(1.dp, UiSubtle.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            )
            Column {
                Text("SELECTED COLOUR", color = UiSubtle, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
                Text(
                    colorToHex(pickedColor),
                    color = UiText, fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            // HSV readout
            Column(horizontalAlignment = Alignment.End) {
                Text("H ${hue.toInt()}°  S ${(sat * 100).toInt()}%  V ${(value * 100).toInt()}%",
                    color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// Draw the saturation-value square for the given hue
private fun DrawScope.drawSvSquare(hueColor: Color, canvasSize: Size) {
    // Layer 1: white→hue horizontal gradient (saturation axis)
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.White, hueColor),
            startX = 0f, endX = canvasSize.width
        ),
        size = canvasSize
    )
    // Layer 2: transparent→black vertical gradient (value axis)
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startY = 0f, endY = canvasSize.height
        ),
        size = canvasSize
    )
}

// ── Utilities ─────────────────────────────────────────────────────────────────

/** Decompose a Compose Color into [hue, saturation, value] (all floats). */
private fun colorToHsv(color: Color): FloatArray {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val delta = max - min
    val v = max
    val s = if (max == 0f) 0f else delta / max
    val h = when {
        delta == 0f -> 0f
        max == r    -> 60f * (((g - b) / delta) % 6f)
        max == g    -> 60f * (((b - r) / delta) + 2f)
        else        -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return floatArrayOf(h, s, v)
}

private fun colorToHex(color: Color): String {
    val r = (color.red   * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue  * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}

private fun formatBandHz(hz: Float): String =
    if (hz >= 1000f) "${"%.1f".format(hz / 1000f)} kHz" else "${hz.toInt()} Hz"

// Kotlin doesn't expose Color.hsv directly in older compose versions, wrap it:
private fun Color.Companion.hsv(h: Float, s: Float, v: Float): Color =
    FrequencyColorMapper.hsvToColor(h, s, v)
