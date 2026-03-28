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
import com.chromasound.app.model.BackgroundEffect
import com.chromasound.app.model.ThemeMode
import com.chromasound.app.model.MirrorMode
import com.chromasound.app.model.ObjectShape
import com.chromasound.app.model.Settings
import kotlin.math.roundToInt

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgColor  = Color(0xFF050508)
private val BgCard   = Color(0xFF0F0F1A)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

// ── Which sub-screen is open ──────────────────────────────────────────────────
private enum class Section { NONE, FREQUENCY, SIZE, AUDIO, VISUAL, EFFECTS }

// ── Root settings screen ──────────────────────────────────────────────────────
@Composable
fun SettingsScreen(
    currentSettings:  Settings,
    onSettingsChange: (Settings) -> Unit,
    onClose:          () -> Unit,
    onOpenBandColors: () -> Unit = {},
    onOpenHelp:       () -> Unit = {},
    onOpenPresets:    () -> Unit = {}
) {
    // All local state in one place — shared across sub-screens
    var bandCount       by remember { mutableStateOf(currentSettings.bandCount.toFloat()) }
    var lifetimeMs      by remember { mutableStateOf(currentSettings.lifetimeMs.toFloat()) }
    var circlesPerBand  by remember { mutableStateOf(currentSettings.circlesPerBand.toFloat()) }
    var minRadius       by remember { mutableStateOf(currentSettings.minRadiusPx) }
    var maxRadius       by remember { mutableStateOf(currentSettings.maxRadiusPx) }
    var placement       by remember { mutableStateOf(currentSettings.placement) }
    var sensitivity     by remember { mutableStateOf(currentSettings.sensitivity) }
    var colorScheme     by remember { mutableStateOf(currentSettings.colorScheme) }
    var objectShape     by remember { mutableStateOf(currentSettings.objectShape) }
    var subBands        by remember { mutableStateOf(currentSettings.subBands.toFloat()) }
    var noiseGateDb     by remember { mutableStateOf(currentSettings.noiseGateDb) }
    var mirrorMode      by remember { mutableStateOf(currentSettings.mirrorMode) }
    var trailLength     by remember { mutableStateOf(currentSettings.trailLength.toFloat()) }
    var beatSensitivity by remember { mutableStateOf(currentSettings.beatSensitivity) }
    var colorAnimSpeed  by remember { mutableStateOf(currentSettings.colorAnimSpeed) }
    var showWaveform      by remember { mutableStateOf(currentSettings.showWaveform) }
    var particlesEnabled  by remember { mutableStateOf(currentSettings.particlesEnabled) }
    var particleThreshold by remember { mutableStateOf(currentSettings.particleThreshold) }
    var oscilloscopeMode  by remember { mutableStateOf(currentSettings.oscilloscopeMode) }
    var backgroundEffect  by remember { mutableStateOf(currentSettings.backgroundEffect) }
    var themeMode         by remember { mutableStateOf(currentSettings.themeMode) }

    var openSection by remember { mutableStateOf(Section.NONE) }

    fun emit() = onSettingsChange(Settings(
        bandCount       = bandCount.roundToInt(),
        lifetimeMs      = lifetimeMs.roundToInt().toLong(),
        circlesPerBand  = circlesPerBand.roundToInt(),
        minRadiusPx     = minRadius,
        maxRadiusPx     = maxRadius.coerceAtLeast(minRadius + 10f),
        placement       = placement,
        sensitivity     = sensitivity,
        colorScheme     = colorScheme,
        objectShape     = objectShape,
        subBands        = subBands.roundToInt(),
        noiseGateDb     = noiseGateDb,
        mirrorMode      = mirrorMode,
        trailLength     = trailLength.roundToInt(),
        beatSensitivity = beatSensitivity,
        colorAnimSpeed  = colorAnimSpeed,
        showWaveform      = showWaveform,
        particlesEnabled  = particlesEnabled,
        particleThreshold = particleThreshold,
        oscilloscopeMode  = oscilloscopeMode,
        backgroundEffect  = backgroundEffect,
        themeMode         = themeMode
    ))

    val sliderColors = SliderDefaults.colors(
        thumbColor         = UiAccent,
        activeTrackColor   = UiAccent,
        inactiveTrackColor = UiSubtle.copy(alpha = 0.35f),
        activeTickColor    = Color.Transparent,
        inactiveTickColor  = Color.Transparent
    )

    when (openSection) {
        Section.FREQUENCY -> FrequencyTimingScreen(
            bandCount      = bandCount,
            lifetimeMs     = lifetimeMs,
            circlesPerBand = circlesPerBand,
            sliderColors   = sliderColors,
            onBandCount    = { bandCount = it; emit() },
            onLifetime     = { lifetimeMs = it; emit() },
            onCircles      = { circlesPerBand = it; emit() },
            onBack         = { openSection = Section.NONE }
        )
        Section.SIZE -> SizePositionScreen(
            minRadius    = minRadius,
            maxRadius    = maxRadius,
            placement    = placement,
            sliderColors = sliderColors,
            onMinRadius  = { minRadius = it; if (maxRadius < it + 10f) maxRadius = it + 10f; emit() },
            onMaxRadius  = { maxRadius = it.coerceAtLeast(minRadius + 10f); emit() },
            onPlacement  = { placement = it; emit() },
            onBack       = { openSection = Section.NONE }
        )
        Section.AUDIO -> AudioScreen(
            sensitivity  = sensitivity,
            noiseGateDb  = noiseGateDb,
            sliderColors = sliderColors,
            onSensitivity = { sensitivity = it; emit() },
            onNoiseGate   = { noiseGateDb = it; emit() },
            onBack        = { openSection = Section.NONE }
        )
        Section.VISUAL -> VisualScreen(
            subBands      = subBands,
            colorScheme   = colorScheme,
            objectShape   = objectShape,
            themeMode     = themeMode,
            sliderColors  = sliderColors,
            onSubBands    = { subBands = it; emit() },
            onColorScheme = { colorScheme = it; emit() },
            onObjectShape = { objectShape = it; emit() },
            onThemeMode   = { themeMode = it; emit() },
            onBandColors  = onOpenBandColors,
            onBack        = { openSection = Section.NONE }
        )
        Section.EFFECTS -> EffectsScreen(
            mirrorMode        = mirrorMode,
            trailLength       = trailLength,
            beatSensitivity   = beatSensitivity,
            colorAnimSpeed    = colorAnimSpeed,
            showWaveform      = showWaveform,
            particlesEnabled  = particlesEnabled,
            particleThreshold = particleThreshold,
            oscilloscopeMode  = oscilloscopeMode,
            backgroundEffect  = backgroundEffect,
            sliderColors      = sliderColors,
            onMirrorMode      = { mirrorMode = it; emit() },
            onTrailLength     = { trailLength = it; emit() },
            onBeatSens        = { beatSensitivity = it; emit() },
            onColorAnim       = { colorAnimSpeed = it; emit() },
            onWaveform        = { showWaveform = it; emit() },
            onParticles       = { particlesEnabled = it; emit() },
            onParticleThresh  = { particleThreshold = it; emit() },
            onOscilloscope    = { oscilloscopeMode = it; emit() },
            onBgEffect        = { backgroundEffect = it; emit() },
            onBack            = { openSection = Section.NONE }
        )
        Section.NONE -> SettingsHubScreen(
            currentSettings = currentSettings,
            onSection       = { openSection = it },
            onClose         = onClose,
            onOpenHelp      = onOpenHelp,
            onOpenPresets   = onOpenPresets
        )
    }
}

// ── Hub screen ────────────────────────────────────────────────────────────────
@Composable
private fun SettingsHubScreen(
    currentSettings: Settings,
    onSection:       (Section) -> Unit,
    onClose:         () -> Unit,
    onOpenHelp:      () -> Unit,
    onOpenPresets:   () -> Unit
) {
    val sections = listOf(
        Triple(Section.FREQUENCY, "FREQUENCY & TIMING",
            "Bands · Lifetime · Circles per band"),
        Triple(Section.SIZE,      "SIZE & POSITION",
            "Min size · Max size · Placement"),
        Triple(Section.AUDIO,     "AUDIO",
            "Mic sensitivity · Noise gate"),
        Triple(Section.VISUAL,    "VISUAL",
            "Sub-band shading · Colour scheme · Shape"),
        Triple(Section.EFFECTS,   "EFFECTS",
            "Mirror · Trails · Beat · Colour animation · Waveform")
    )
    val sectionEmoji = mapOf(
        Section.FREQUENCY to "🎵",
        Section.SIZE      to "📐",
        Section.AUDIO     to "🎙",
        Section.VISUAL    to "🎨",
        Section.EFFECTS   to "✨"
    )
    val sectionAccent = listOf(
        Color(0xFF7C6FFF), Color(0xFF42E5F5),
        Color(0xFFFF6B6B), Color(0xFFFFCC00), Color(0xFF6BFFFF)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgColor).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(56.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
                    border = ButtonDefaults.outlinedButtonColors().let {
                        androidx.compose.foundation.BorderStroke(1.dp, UiSubtle)
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("← BACK", fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("SETTINGS", color = UiText, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onOpenHelp,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UiAccent),
                    border = androidx.compose.foundation.BorderStroke(1.dp, UiAccent.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("?  HELP", fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp, letterSpacing = 2.sp, color = UiAccent)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Tap a section to adjust its settings",
                color = UiSubtle, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
        }

        // ── Section buttons ─────────────────────────────────────────────────────
        itemsIndexed(sections) { idx, (section, label, subtitle) ->
            val accent = sectionAccent[idx]
            val emoji  = sectionEmoji[section] ?: ""
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .clickable { onSection(section) }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp)
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text(emoji, fontSize = 22.sp) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, color = accent, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, color = UiSubtle, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace)
                }
                Text("→", color = accent, fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Presets shortcut ─────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgCard)
                    .clickable { onOpenPresets() }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp)
                        .background(UiAccent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🎛", fontSize = 22.sp) }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("PRESETS", color = UiAccent, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp)
                    Spacer(Modifier.height(3.dp))
                    Text("Save, load and apply colour themes",
                        color = UiSubtle, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Text("→", color = UiAccent, fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Version footer ────────────────────────────────────────────────────────
        item {
            Text("ChromaSound  ·  Version 2.2.0",
                color = UiSubtle.copy(alpha = 0.6f), fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Sub-screen scaffold ───────────────────────────────────────────────────────
@Composable
private fun SubScreen(
    title:   String,
    emoji:   String,
    accent:  Color = UiAccent,
    onBack:  () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(BgColor)
    ) {
        // Fixed header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
                border = androidx.compose.foundation.BorderStroke(1.dp, UiSubtle),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("← BACK", fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(title, color = accent, fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp)
        }
        // Scrollable content
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Column(content = content)
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

// ── FREQUENCY & TIMING ────────────────────────────────────────────────────────
@Composable
private fun FrequencyTimingScreen(
    bandCount:      Float,
    lifetimeMs:     Float,
    circlesPerBand: Float,
    sliderColors:   SliderColors,
    onBandCount:    (Float) -> Unit,
    onLifetime:     (Float) -> Unit,
    onCircles:      (Float) -> Unit,
    onBack:         () -> Unit
) {
    SubScreen("FREQUENCY & TIMING", "🎵", Color(0xFF7C6FFF), onBack) {
        SettingCard("FREQUENCY BANDS", "30 Hz – 11 kHz",
            value = "${bandCount.roundToInt()}", unit = "bands") {
            Slider(value = bandCount, onValueChange = onBandCount,
                valueRange = BandDefinition.MIN_BANDS.toFloat()..BandDefinition.MAX_BANDS.toFloat(),
                steps = BandDefinition.MAX_BANDS - BandDefinition.MIN_BANDS - 1,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("${BandDefinition.MIN_BANDS}", "${BandDefinition.MAX_BANDS}")
        }
        Spacer(Modifier.height(14.dp))
        SettingCard("CIRCLE LIFETIME", "How long each shape stays visible",
            value = formatMs(lifetimeMs.roundToInt().toLong()), unit = "") {
            Slider(value = lifetimeMs, onValueChange = onLifetime,
                valueRange = Settings.MIN_LIFETIME_MS.toFloat()..Settings.MAX_LIFETIME_MS.toFloat(),
                steps = 18, colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels(formatMs(Settings.MIN_LIFETIME_MS), formatMs(Settings.MAX_LIFETIME_MS))
        }
        Spacer(Modifier.height(14.dp))
        SettingCard("CIRCLES PER BAND", "Max simultaneous shapes per frequency band",
            value = "${circlesPerBand.roundToInt()}",
            unit = if (circlesPerBand.roundToInt() == 1) "circle" else "circles") {
            Slider(value = circlesPerBand, onValueChange = onCircles,
                valueRange = Settings.MIN_CIRCLES_PER_BAND.toFloat()..Settings.MAX_CIRCLES_PER_BAND.toFloat(),
                steps = Settings.MAX_CIRCLES_PER_BAND - Settings.MIN_CIRCLES_PER_BAND - 1,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("${Settings.MIN_CIRCLES_PER_BAND}", "${Settings.MAX_CIRCLES_PER_BAND}")
        }
    }
}

// ── SIZE & POSITION ───────────────────────────────────────────────────────────
@Composable
private fun SizePositionScreen(
    minRadius:   Float,
    maxRadius:   Float,
    placement:   Float,
    sliderColors: SliderColors,
    onMinRadius: (Float) -> Unit,
    onMaxRadius: (Float) -> Unit,
    onPlacement: (Float) -> Unit,
    onBack:      () -> Unit
) {
    SubScreen("SIZE & POSITION", "📐", Color(0xFF42E5F5), onBack) {
        SettingCard("MINIMUM SIZE", "Radius at the quietest detectable level",
            value = "${minRadius.roundToInt()}", unit = "px") {
            Slider(value = minRadius, onValueChange = onMinRadius,
                valueRange = Settings.MIN_RADIUS_FLOOR..Settings.MAX_RADIUS_FLOOR,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("${Settings.MIN_RADIUS_FLOOR.roundToInt()} px",
                "${Settings.MAX_RADIUS_FLOOR.roundToInt()} px")
        }
        Spacer(Modifier.height(14.dp))
        SettingCard("MAXIMUM SIZE", "Radius at the loudest detectable level",
            value = "${maxRadius.roundToInt()}", unit = "px") {
            Slider(value = maxRadius, onValueChange = onMaxRadius,
                valueRange = Settings.MIN_RADIUS_CEILING..Settings.MAX_RADIUS_CEILING,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("${Settings.MIN_RADIUS_CEILING.roundToInt()} px",
                "${Settings.MAX_RADIUS_CEILING.roundToInt()} px")
        }
        Spacer(Modifier.height(14.dp))
        SettingCard("PLACEMENT", "How randomly shapes scatter from their band column",
            value = placementLabel(placement), unit = "") {
            Slider(value = placement, onValueChange = onPlacement,
                valueRange = Settings.MIN_PLACEMENT..Settings.MAX_PLACEMENT,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("Grid-locked", "Full random")
        }
    }
}

// ── AUDIO ─────────────────────────────────────────────────────────────────────
@Composable
private fun AudioScreen(
    sensitivity:   Float,
    noiseGateDb:   Float,
    sliderColors:  SliderColors,
    onSensitivity: (Float) -> Unit,
    onNoiseGate:   (Float) -> Unit,
    onBack:        () -> Unit
) {
    SubScreen("AUDIO", "🎙", Color(0xFFFF6B6B), onBack) {
        SettingCard("MIC SENSITIVITY", "Amplify or reduce response to incoming audio",
            value = "×${"%.1f".format(sensitivity)}", unit = "") {
            Slider(value = sensitivity, onValueChange = onSensitivity,
                valueRange = Settings.MIN_SENSITIVITY..Settings.MAX_SENSITIVITY,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("×${"%.1f".format(Settings.MIN_SENSITIVITY)} low",
                "×${"%.1f".format(Settings.MAX_SENSITIVITY)} high")
        }
        Spacer(Modifier.height(14.dp))
        SettingCard("NOISE GATE", "Minimum level a band must reach to appear",
            value = "${noiseGateDb.roundToInt()} dB", unit = "threshold") {
            Slider(value = noiseGateDb, onValueChange = onNoiseGate,
                valueRange = Settings.MIN_NOISE_GATE_DB..Settings.MAX_NOISE_GATE_DB,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("${Settings.MIN_NOISE_GATE_DB.roundToInt()} dB  very sensitive",
                "${Settings.MAX_NOISE_GATE_DB.roundToInt()} dB  loud only")
        }
    }
}

// ── VISUAL ────────────────────────────────────────────────────────────────────
@Composable
private fun VisualScreen(
    subBands:      Float,
    colorScheme:   ColorScheme,
    objectShape:   ObjectShape,
    themeMode:     ThemeMode,
    sliderColors:  SliderColors,
    onSubBands:    (Float) -> Unit,
    onColorScheme: (ColorScheme) -> Unit,
    onObjectShape: (ObjectShape) -> Unit,
    onThemeMode:   (ThemeMode) -> Unit,
    onBandColors:  () -> Unit,
    onBack:        () -> Unit
) {
    SubScreen("VISUAL", "🎨", Color(0xFFFFCC00), onBack) {
        SettingCard("SUB-BAND SHADING",
            "Radial shading rings from centre to edge (1 = solid colour)",
            value = if (subBands.roundToInt() == 1) "Off" else "${subBands.roundToInt()}",
            unit  = if (subBands.roundToInt() == 1) "" else "rings") {
            Slider(value = subBands, onValueChange = onSubBands,
                valueRange = Settings.MIN_SUB_BANDS.toFloat()..Settings.MAX_SUB_BANDS.toFloat(),
                steps = Settings.MAX_SUB_BANDS - Settings.MIN_SUB_BANDS - 1,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("Off (1)", "${Settings.MAX_SUB_BANDS} rings")
        }
        Spacer(Modifier.height(14.dp))

        // Color scheme
        Column(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(16.dp)).padding(20.dp)) {
            Text("COLOR SCHEME", color = UiSubtle, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ColorScheme.entries.forEach { scheme ->
                    ColorSchemeButton(
                        label    = if (scheme == ColorScheme.RAINBOW) "Rainbow" else "Inverse",
                        gradient = if (scheme == ColorScheme.RAINBOW)
                            listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF))
                        else
                            listOf(Color(0xFF0000FF), Color(0xFF00FF00), Color(0xFFFF0000)),
                        selected = colorScheme == scheme,
                        modifier = Modifier.weight(1f),
                        onClick  = { onColorScheme(scheme) }
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Object shape
        Column(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(16.dp)).padding(20.dp)) {
            Text("OBJECT SHAPE", color = UiSubtle, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            val shapes = ObjectShape.entries
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                shapes.forEach { shape ->
                    val selected = objectShape == shape
                    val emoji = when (shape) {
                        ObjectShape.CIRCLE -> "●"; ObjectShape.STAR -> "★"
                        ObjectShape.BOX_2D -> "■"; ObjectShape.BOX_3D -> "⬡"
                        ObjectShape.SPHERE -> "◉"
                    }
                    val label = when (shape) {
                        ObjectShape.CIRCLE -> "Circle"; ObjectShape.STAR -> "Star"
                        ObjectShape.BOX_2D -> "Box"; ObjectShape.BOX_3D -> "3D"
                        ObjectShape.SPHERE -> "Sphere"
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) UiAccent.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp,
                                if (selected) UiAccent else UiSubtle.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp))
                            .clickable { onObjectShape(shape) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emoji, fontSize = 16.sp,
                            color = if (selected) UiAccent else UiSubtle)
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontSize = 8.sp,
                            color = if (selected) UiAccent else UiSubtle,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Theme mode selector
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(16.dp)).padding(20.dp)) {
            Text("DISPLAY THEME", color = UiSubtle, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            val themes = listOf(
                ThemeMode.DARK   to "Dark",
                ThemeMode.SYSTEM to "System",
                ThemeMode.LIGHT  to "Light"
            )
            val themeEmoji = mapOf(
                ThemeMode.DARK to "🌙", ThemeMode.SYSTEM to "⚙", ThemeMode.LIGHT to "☀️"
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { (mode, label) ->
                    val selected = themeMode == mode
                    Column(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) UiAccent.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp, if (selected) UiAccent else UiSubtle.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp))
                            .clickable { onThemeMode(mode) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(themeEmoji[mode] ?: "", fontSize = 16.sp,
                            color = if (selected) UiAccent else UiSubtle)
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontSize = 9.sp,
                            color = if (selected) UiAccent else UiSubtle,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        // Band colours shortcut
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .clickable { onBandColors() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("BAND COLOURS", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(3.dp))
                Text("Override colour per frequency band",
                    color = UiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Text("→", color = UiAccent, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// ── EFFECTS ───────────────────────────────────────────────────────────────────
@Composable
private fun EffectsScreen(
    mirrorMode:        MirrorMode,
    trailLength:       Float,
    beatSensitivity:   Float,
    colorAnimSpeed:    Float,
    showWaveform:      Boolean,
    particlesEnabled:  Boolean,
    particleThreshold: Float,
    oscilloscopeMode:  Boolean,
    backgroundEffect:  BackgroundEffect,
    sliderColors:      SliderColors,
    onMirrorMode:      (MirrorMode) -> Unit,
    onTrailLength:     (Float) -> Unit,
    onBeatSens:        (Float) -> Unit,
    onColorAnim:       (Float) -> Unit,
    onWaveform:        (Boolean) -> Unit,
    onParticles:       (Boolean) -> Unit,
    onParticleThresh:  (Float) -> Unit,
    onOscilloscope:    (Boolean) -> Unit,
    onBgEffect:        (BackgroundEffect) -> Unit,
    onBack:            () -> Unit
) {
    SubScreen("EFFECTS", "✨", Color(0xFF6BFFFF), onBack) {
        // Mirror mode
        val modes = MirrorMode.entries.toList()
        val modeLabels = mapOf(
            MirrorMode.OFF to "Off", MirrorMode.HORIZONTAL to "H",
            MirrorMode.VERTICAL to "V", MirrorMode.QUAD to "Quad")
        val modeEmoji = mapOf(
            MirrorMode.OFF to "▣", MirrorMode.HORIZONTAL to "◫",
            MirrorMode.VERTICAL to "⬒", MirrorMode.QUAD to "⧈")
        Column(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(16.dp)).padding(20.dp)) {
            Text("MIRROR MODE", color = UiSubtle, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            Spacer(Modifier.height(4.dp))
            Text("Reflect shapes across canvas axes",
                color = UiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { mode ->
                    val selected = mirrorMode == mode
                    Column(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) UiAccent.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp,
                                if (selected) UiAccent else UiSubtle.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp))
                            .clickable { onMirrorMode(mode) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(modeEmoji[mode] ?: "", fontSize = 16.sp,
                            color = if (selected) UiAccent else UiSubtle)
                        Spacer(Modifier.height(4.dp))
                        Text(modeLabels[mode] ?: "", fontSize = 9.sp,
                            color = if (selected) UiAccent else UiSubtle,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        val trailInt = trailLength.roundToInt()
        SettingCard("SHAPE TRAILS",
            "Ghost copies behind each shape",
            value  = if (trailInt == 0) "Off" else "$trailInt",
            unit   = if (trailInt == 0) "" else if (trailInt == 1) "ghost" else "ghosts") {
            Slider(value = trailLength, onValueChange = onTrailLength,
                valueRange = Settings.MIN_TRAIL_LENGTH.toFloat()..Settings.MAX_TRAIL_LENGTH.toFloat(),
                steps = Settings.MAX_TRAIL_LENGTH - Settings.MIN_TRAIL_LENGTH - 1,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("Off", "${Settings.MAX_TRAIL_LENGTH} frames")
        }
        Spacer(Modifier.height(14.dp))

        SettingCard("BEAT SENSITIVITY",
            "How loud above average to count as a beat",
            value = "×${"%.1f".format(beatSensitivity)}", unit = "threshold") {
            Slider(value = beatSensitivity, onValueChange = onBeatSens,
                valueRange = Settings.MIN_BEAT_SENSITIVITY..Settings.MAX_BEAT_SENSITIVITY,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("×1.1  very sensitive", "×2.5  hard hits only")
        }
        Spacer(Modifier.height(14.dp))

        SettingCard("COLOUR ANIMATION",
            "Hue cycles continuously over time",
            value = if (colorAnimSpeed == 0f) "Off" else "${"%.1f".format(colorAnimSpeed)}×",
            unit  = if (colorAnimSpeed == 0f) "" else "speed") {
            Slider(value = colorAnimSpeed, onValueChange = onColorAnim,
                valueRange = Settings.MIN_COLOR_ANIM_SPEED..Settings.MAX_COLOR_ANIM_SPEED,
                colors = sliderColors, modifier = Modifier.fillMaxWidth())
            SliderLabels("Off", "3× fast")
        }
        Spacer(Modifier.height(14.dp))

        // Waveform toggle
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(BgCard, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("WAVEFORM OVERLAY", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(3.dp))
                Text(if (showWaveform) "Scrolling audio waveform visible"
                     else "Waveform hidden",
                    color = UiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Switch(
                checked = showWaveform,
                onCheckedChange = onWaveform,
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = UiText,
                    checkedTrackColor   = UiAccent,
                    uncheckedThumbColor = UiSubtle,
                    uncheckedTrackColor = UiSubtle.copy(alpha = 0.3f)
                )
            )
        }
        Spacer(Modifier.height(14.dp))

        // Oscilloscope ring mode
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(BgCard, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("OSCILLOSCOPE RING MODE", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(3.dp))
                Text(if (oscilloscopeMode) "Shapes draw as pulsing rings"
                     else "Shapes draw as filled objects",
                    color = UiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Switch(
                checked = oscilloscopeMode,
                onCheckedChange = onOscilloscope,
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = UiText,
                    checkedTrackColor   = UiAccent,
                    uncheckedThumbColor = UiSubtle,
                    uncheckedTrackColor = UiSubtle.copy(alpha = 0.3f)
                )
            )
        }
        Spacer(Modifier.height(14.dp))

        // Particle explosions
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(BgCard, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("PARTICLE EXPLOSIONS", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(3.dp))
                Text(if (particlesEnabled) "Bursts on loud transients"
                     else "Particle explosions off",
                    color = UiText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Switch(
                checked = particlesEnabled,
                onCheckedChange = onParticles,
                colors = SwitchDefaults.colors(
                    checkedThumbColor   = UiText,
                    checkedTrackColor   = UiAccent,
                    uncheckedThumbColor = UiSubtle,
                    uncheckedTrackColor = UiSubtle.copy(alpha = 0.3f)
                )
            )
        }
        if (particlesEnabled) {
            Spacer(Modifier.height(14.dp))
            SettingCard("PARTICLE THRESHOLD",
                "How loud above band average triggers a burst",
                value = "${"%.0f".format(particleThreshold * 100)}%%", unit = "sensitivity") {
                Slider(value = particleThreshold, onValueChange = onParticleThresh,
                    valueRange = Settings.MIN_PARTICLE_THRESHOLD..Settings.MAX_PARTICLE_THRESHOLD,
                    colors = sliderColors, modifier = Modifier.fillMaxWidth())
                SliderLabels("10%%  very sensitive", "100%%  loud only")
            }
        }
        Spacer(Modifier.height(14.dp))

        // Background effect selector
        Column(Modifier.fillMaxWidth().background(BgCard, RoundedCornerShape(16.dp)).padding(20.dp)) {
            Text("BACKGROUND EFFECT", color = UiSubtle, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            Spacer(Modifier.height(12.dp))
            val bgOptions = listOf(
                BackgroundEffect.NONE     to "None",
                BackgroundEffect.STARFIELD to "Stars",
                BackgroundEffect.BLOOM    to "Bloom",
                BackgroundEffect.NOISE    to "Noise"
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bgOptions.forEach { (effect, label) ->
                    val selected = backgroundEffect == effect
                    val emoji = when(effect) {
                        BackgroundEffect.NONE      -> "■"
                        BackgroundEffect.STARFIELD -> "✦"
                        BackgroundEffect.BLOOM     -> "◉"
                        BackgroundEffect.NOISE     -> "▦"
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) UiAccent.copy(alpha = 0.18f) else Color.Transparent)
                            .border(1.dp,
                                if (selected) UiAccent else UiSubtle.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp))
                            .clickable { onBgEffect(effect) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emoji, fontSize = 16.sp,
                            color = if (selected) UiAccent else UiSubtle)
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontSize = 8.sp,
                            color = if (selected) UiAccent else UiSubtle,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
fun SettingCard(
    label:    String,
    subtitle: String,
    value:    String,
    unit:     String,
    content:  @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = UiText, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(value, color = UiAccent, fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                if (unit.isNotEmpty())
                    Text(unit, color = UiSubtle, fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.End))
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun SliderLabels(start: String, end: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(start, color = UiSubtle, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(end,   color = UiSubtle, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun ColorSchemeButton(
    label:    String,
    gradient: List<Color>,
    selected: Boolean,
    modifier: Modifier,
    onClick:  () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) UiAccent.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp,
                if (selected) UiAccent else UiSubtle.copy(alpha = 0.35f),
                RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(gradient)))
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (selected) UiAccent else UiSubtle,
            fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatMs(ms: Long): String =
    if (ms >= 1000L) "${"%.1f".format(ms / 1000.0)}s" else "${ms}ms"

private fun placementLabel(v: Float): String = when {
    v < 0.1f -> "Grid"; v < 0.4f -> "Slight"; v < 0.7f -> "Mixed"; else -> "Random"
}
