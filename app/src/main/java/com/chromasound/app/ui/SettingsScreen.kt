package com.chromasound.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromasound.app.model.BandDefinition
import com.chromasound.app.model.ColorScheme
import com.chromasound.app.model.ObjectShape
import com.chromasound.app.model.Settings
import kotlin.math.roundToInt

private val BgColor  = Color(0xFF050508)
private val BgCard   = Color(0xFF0F0F1A)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

@Composable
fun SettingsScreen(
    currentSettings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onClose: () -> Unit
) {
    // Local state mirrors — UI stays snappy, ViewModel gets every change live
    var bandCount      by remember { mutableStateOf(currentSettings.bandCount.toFloat()) }
    var lifetimeMs     by remember { mutableStateOf(currentSettings.lifetimeMs.toFloat()) }
    var circlesPerBand by remember { mutableStateOf(currentSettings.circlesPerBand.toFloat()) }
    var minRadius      by remember { mutableStateOf(currentSettings.minRadiusPx) }
    var maxRadius      by remember { mutableStateOf(currentSettings.maxRadiusPx) }
    var placement      by remember { mutableStateOf(currentSettings.placement) }
    var sensitivity    by remember { mutableStateOf(currentSettings.sensitivity) }
    var colorScheme    by remember { mutableStateOf(currentSettings.colorScheme) }
    var objectShape    by remember { mutableStateOf(currentSettings.objectShape) }

    fun emit() = onSettingsChange(Settings(
        bandCount      = bandCount.roundToInt(),
        lifetimeMs     = lifetimeMs.roundToInt().toLong(),
        circlesPerBand = circlesPerBand.roundToInt(),
        minRadiusPx    = minRadius,
        maxRadiusPx    = maxRadius.coerceAtLeast(minRadius + 10f),
        placement      = placement,
        sensitivity    = sensitivity,
        colorScheme    = colorScheme,
        objectShape    = objectShape
    ))

    val bands     = remember(bandCount.roundToInt()) { BandDefinition.build(bandCount.roundToInt()) }
    val bandItems = remember(bands) {
        (0 until bands.count).map { Triple(it + 1, bands.lowerHz[it], bands.upperHz[it]) }
    }

    val sliderColors = SliderDefaults.colors(
        thumbColor         = UiAccent,
        activeTrackColor   = UiAccent,
        inactiveTrackColor = UiSubtle.copy(alpha = 0.35f),
        activeTickColor    = Color.Transparent,
        inactiveTickColor  = Color.Transparent
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgColor).padding(horizontal = 24.dp),
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
                    Text("← BACK", fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("SETTINGS", color = UiText, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(80.dp))
            }
            Spacer(Modifier.height(28.dp))
        }

        // ── 1. Frequency bands ────────────────────────────────────────────────
        item {
            SettingCard("FREQUENCY BANDS", "30 Hz  –  11 kHz",
                value = "${bandCount.roundToInt()}", unit = "bands") {
                Slider(value = bandCount, onValueChange = { bandCount = it; emit() },
                    valueRange = BandDefinition.MIN_BANDS.toFloat()..BandDefinition.MAX_BANDS.toFloat(),
                    steps = BandDefinition.MAX_BANDS - BandDefinition.MIN_BANDS - 1,
                    colors = sliderColors, modifier = Modifier.fillMaxWidth())
                SliderLabels("${BandDefinition.MIN_BANDS}", "${BandDefinition.MAX_BANDS}")
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 2. Circle lifetime ────────────────────────────────────────────────
        item {
            SettingCard("CIRCLE LIFETIME", "How long each circle stays visible",
                value = formatMs(lifetimeMs.roundToInt().toLong()), unit = "") {
                Slider(value = lifetimeMs, onValueChange = { lifetimeMs = it; emit() },
                    valueRange = Settings.MIN_LIFETIME_MS.toFloat()..Settings.MAX_LIFETIME_MS.toFloat(),
                    steps = 18, colors = sliderColors, modifier = Modifier.fillMaxWidth())
                SliderLabels(formatMs(Settings.MIN_LIFETIME_MS), formatMs(Settings.MAX_LIFETIME_MS))
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 3. Circles per band ───────────────────────────────────────────────
        item {
            SettingCard("CIRCLES PER BAND", "Max simultaneous circles per frequency band",
                value = "${circlesPerBand.roundToInt()}",
                unit = if (circlesPerBand.roundToInt() == 1) "circle" else "circles") {
                Slider(value = circlesPerBand, onValueChange = { circlesPerBand = it; emit() },
                    valueRange = Settings.MIN_CIRCLES_PER_BAND.toFloat()..Settings.MAX_CIRCLES_PER_BAND.toFloat(),
                    steps = Settings.MAX_CIRCLES_PER_BAND - Settings.MIN_CIRCLES_PER_BAND - 1,
                    colors = sliderColors, modifier = Modifier.fillMaxWidth())
                SliderLabels("${Settings.MIN_CIRCLES_PER_BAND}", "${Settings.MAX_CIRCLES_PER_BAND}")
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 4. Minimum circle size ────────────────────────────────────────────
        item {
            SettingCard("MINIMUM CIRCLE SIZE", "Radius at the quietest detectable level",
                value = "${minRadius.roundToInt()}", unit = "px") {
                Slider(value = minRadius, onValueChange = {
                    minRadius = it
                    if (maxRadius < minRadius + 10f) maxRadius = minRadius + 10f
                    emit()
                }, valueRange = Settings.MIN_RADIUS_FLOOR..Settings.MAX_RADIUS_FLOOR,
                    colors = sliderColors, modifier = Modifier.fillMaxWidth())
                SliderLabels("${Settings.MIN_RADIUS_FLOOR.roundToInt()} px", "${Settings.MAX_RADIUS_FLOOR.roundToInt()} px")
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 5. Maximum circle size ────────────────────────────────────────────
        item {
            SettingCard("MAXIMUM CIRCLE SIZE", "Radius at the loudest detectable level",
                value = "${maxRadius.roundToInt()}", unit = "px") {
                Slider(value = maxRadius, onValueChange = { maxRadius = it.coerceAtLeast(minRadius + 10f); emit() },
                    valueRange = Settings.MIN_RADIUS_CEILING..Settings.MAX_RADIUS_CEILING,
                    colors = sliderColors, modifier = Modifier.fillMaxWidth())
                SliderLabels("${Settings.MIN_RADIUS_CEILING.roundToInt()} px", "${Settings.MAX_RADIUS_CEILING.roundToInt()} px")
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 6. Placement randomness ───────────────────────────────────────────
        item {
            SettingCard("CIRCLE PLACEMENT", "How randomly circles scatter from their band column",
                value = placementLabel(placement), unit = "") {
                Slider(value = placement, onValueChange = { placement = it; emit() },
                    valueRange = Settings.MIN_PLACEMENT..Settings.MAX_PLACEMENT,
                    colors = sliderColors, modifier = Modifier.fillMaxWidth())
                SliderLabels("Grid-locked", "Full random")
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 7. Microphone sensitivity ─────────────────────────────────────────
        item {
            SettingCard("MIC SENSITIVITY", "Amplify or reduce response to incoming audio",
                value = "×${"%.1f".format(sensitivity)}", unit = "") {
                Slider(value = sensitivity, onValueChange = { sensitivity = it; emit() },
                    valueRange = Settings.MIN_SENSITIVITY..Settings.MAX_SENSITIVITY,
                    colors = sliderColors, modifier = Modifier.fillMaxWidth())
                SliderLabels("×${"%.1f".format(Settings.MIN_SENSITIVITY)} low", "×${"%.1f".format(Settings.MAX_SENSITIVITY)} high")
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── 8. Color scheme toggle ────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text("COLOR SCHEME", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(4.dp))
                Text("Hue order across frequency bands",
                    color = UiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorSchemeButton(
                        label      = "RAINBOW",
                        subLabel   = "Violet → Red",
                        isSelected = colorScheme == ColorScheme.RAINBOW,
                        gradientColors = listOf(
                            Color(0xFF8B00FF), Color(0xFF0000FF), Color(0xFF00FFFF),
                            Color(0xFF00FF00), Color(0xFFFFFF00), Color(0xFFFF0000)
                        ),
                        modifier = Modifier.weight(1f),
                        onClick  = { colorScheme = ColorScheme.RAINBOW; emit() }
                    )
                    ColorSchemeButton(
                        label      = "INVERSE",
                        subLabel   = "Red → Violet",
                        isSelected = colorScheme == ColorScheme.INVERSE_RAINBOW,
                        gradientColors = listOf(
                            Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                            Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFF8B00FF)
                        ),
                        modifier = Modifier.weight(1f),
                        onClick  = { colorScheme = ColorScheme.INVERSE_RAINBOW; emit() }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── 9. Object shape selector ──────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Text("OBJECT SHAPE", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(4.dp))
                Text("Shape used to represent each frequency band",
                    color = UiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(16.dp))

                // Row 1: Circle, Star, 2D Box
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShapeButton(emoji = "●", label = "CIRCLE",
                        isSelected = objectShape == ObjectShape.CIRCLE,
                        modifier = Modifier.weight(1f),
                        onClick = { objectShape = ObjectShape.CIRCLE; emit() })
                    ShapeButton(emoji = "★", label = "STAR",
                        isSelected = objectShape == ObjectShape.STAR,
                        modifier = Modifier.weight(1f),
                        onClick = { objectShape = ObjectShape.STAR; emit() })
                    ShapeButton(emoji = "■", label = "2D BOX",
                        isSelected = objectShape == ObjectShape.BOX_2D,
                        modifier = Modifier.weight(1f),
                        onClick = { objectShape = ObjectShape.BOX_2D; emit() })
                }
                Spacer(Modifier.height(10.dp))
                // Row 2: 3D Box, Sphere — with rotating note
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShapeButton(emoji = "⬡", label = "3D BOX",
                        subLabel = "rotating",
                        isSelected = objectShape == ObjectShape.BOX_3D,
                        modifier = Modifier.weight(1f),
                        onClick = { objectShape = ObjectShape.BOX_3D; emit() })
                    ShapeButton(emoji = "◉", label = "SPHERE",
                        subLabel = "rotating",
                        isSelected = objectShape == ObjectShape.SPHERE,
                        modifier = Modifier.weight(1f),
                        onClick = { objectShape = ObjectShape.SPHERE; emit() })
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Band breakdown ────────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("BAND BREAKDOWN", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Text("${bandCount.roundToInt()} bands", color = UiAccent,
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#",    color = UiSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(32.dp))
                Text("LOW",  color = UiSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("–",    color = UiSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("HIGH", color = UiSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }

        itemsIndexed(bandItems) { index, (number, low, high) ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (index % 2 == 0) BgCard else BgCard.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.width(32.dp)
                        .background(UiAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$number", color = UiAccent, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
                Text(formatBandHz(low), color = UiText, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center)
                Text("–", color = UiSubtle, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(formatBandHz(high), color = UiText, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center)
            }
            if (index == bandItems.size - 1) {
                Spacer(Modifier.fillMaxWidth().height(12.dp)
                    .background(BgCard, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)))
            }
        }

        // ── Done ──────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onClose,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = UiAccent),
                modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
            ) {
                Text("DONE", fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, letterSpacing = 4.sp, fontSize = 13.sp)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Color scheme button ───────────────────────────────────────────────────────

@Composable
private fun ColorSchemeButton(
    label:          String,
    subLabel:       String,
    isSelected:     Boolean,
    gradientColors: List<Color>,
    modifier:       Modifier = Modifier,
    onClick:        () -> Unit
) {
    val borderColor = if (isSelected) UiAccent else UiSubtle.copy(alpha = 0.3f)
    val bgColor     = if (isSelected) UiAccent.copy(alpha = 0.12f) else BgColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Rainbow gradient preview bar
        Box(
            Modifier.fillMaxWidth().height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(gradientColors))
        )
        Spacer(Modifier.height(10.dp))
        Text(label, color = if (isSelected) UiAccent else UiText,
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Text(subLabel, color = UiSubtle, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
        if (isSelected) {
            Spacer(Modifier.height(6.dp))
            Text("✓  ACTIVE", color = UiAccent, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun ShapeButton(
    emoji:      String,
    label:      String,
    subLabel:   String   = "",
    isSelected: Boolean,
    modifier:   Modifier = Modifier,
    onClick:    () -> Unit
) {
    val borderColor = if (isSelected) UiAccent else UiSubtle.copy(alpha = 0.3f)
    val bgColor     = if (isSelected) UiAccent.copy(alpha = 0.12f) else BgColor

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 26.sp,
            color = if (isSelected) UiAccent else UiText)
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (isSelected) UiAccent else UiText,
            fontSize = 10.sp, fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp,
            textAlign = TextAlign.Center)
        if (subLabel.isNotEmpty()) {
            Text(subLabel, color = UiSubtle, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
        }
        if (isSelected) {
            Spacer(Modifier.height(4.dp))
            Text("✓", color = UiAccent, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace)
        }
    }
}

// ── Reusable setting card ─────────────────────────────────────────────────────

@Composable
private fun SettingCard(
    label:    String,
    subLabel: String,
    value:    String,
    unit:     String,
    content:  @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text(label, color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(3.dp))
                Text(subLabel, color = UiText, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(value, color = UiAccent, fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace,
                    lineHeight = 38.sp)
                if (unit.isNotEmpty())
                    Text(unit, color = UiSubtle, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun SliderLabels(min: String, max: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(min, color = UiSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(max, color = UiSubtle, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

// ── Formatters ────────────────────────────────────────────────────────────────

private fun formatBandHz(hz: Float): String =
    if (hz >= 1000f) "${"%.1f".format(hz / 1000f)} kHz" else "${hz.roundToInt()} Hz"

private fun formatMs(ms: Long): String =
    if (ms >= 1000L) "${"%.1f".format(ms / 1000f)} s" else "${ms} ms"

private fun placementLabel(v: Float): String = when {
    v < 0.15f -> "Grid"
    v < 0.40f -> "Slight"
    v < 0.65f -> "Medium"
    v < 0.85f -> "High"
    else       -> "Full"
}
