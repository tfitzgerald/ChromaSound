package com.chromasound.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.chromasound.app.model.ColorScheme
import com.chromasound.app.model.MirrorMode
import com.chromasound.app.model.ObjectShape
import com.chromasound.app.model.Settings

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgColor  = Color(0xFF050508)
private val BgCard   = Color(0xFF0F0F1A)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)
private val DangerC  = Color(0xFFFF4444)

// ── Preset data model ─────────────────────────────────────────────────────────

data class NamedPreset(
    val name:           String,
    val bandCount:      Int,
    val lifetimeMs:     Long,
    val circlesPerBand: Int,
    val minRadiusPx:    Float,
    val maxRadiusPx:    Float,
    val placement:      Float,
    val sensitivity:    Float,
    val colorScheme:    ColorScheme,
    val objectShape:    ObjectShape,
    val subBands:       Int,
    val noiseGateDb:    Float,
    val mirrorMode:     MirrorMode  = MirrorMode.OFF,
    val trailLength:    Int         = 0,
    val beatSensitivity:Float       = 1.3f,
    val colorAnimSpeed: Float       = 0f,
    val showWaveform:   Boolean     = false
) {
    fun toSettings(base: Settings) = base.copy(
        bandCount       = bandCount,
        lifetimeMs      = lifetimeMs,
        circlesPerBand  = circlesPerBand,
        minRadiusPx     = minRadiusPx,
        maxRadiusPx     = maxRadiusPx,
        placement       = placement,
        sensitivity     = sensitivity,
        colorScheme     = colorScheme,
        objectShape     = objectShape,
        subBands        = subBands,
        noiseGateDb     = noiseGateDb,
        mirrorMode      = mirrorMode,
        trailLength     = trailLength,
        beatSensitivity = beatSensitivity,
        colorAnimSpeed  = colorAnimSpeed,
        showWaveform    = showWaveform,
        bandColors      = emptyMap()
    )
}

fun Settings.toPreset(name: String) = NamedPreset(
    name            = name,
    bandCount       = bandCount,
    lifetimeMs      = lifetimeMs,
    circlesPerBand  = circlesPerBand,
    minRadiusPx     = minRadiusPx,
    maxRadiusPx     = maxRadiusPx,
    placement       = placement,
    sensitivity     = sensitivity,
    colorScheme     = colorScheme,
    objectShape     = objectShape,
    subBands        = subBands,
    noiseGateDb     = noiseGateDb,
    mirrorMode      = mirrorMode,
    trailLength     = trailLength,
    beatSensitivity = beatSensitivity,
    colorAnimSpeed  = colorAnimSpeed,
    showWaveform    = showWaveform
)

// ── Preset persistence ────────────────────────────────────────────────────────

private const val PREFS_PRESETS = "chromasound_presets"
private const val MAX_PRESETS   = 10

fun loadPresets(context: Context): List<NamedPreset> {
    val prefs = context.getSharedPreferences(PREFS_PRESETS, Context.MODE_PRIVATE)
    val count = prefs.getInt("count", 0)
    return (0 until count).mapNotNull { i ->
        try {
            NamedPreset(
                name            = prefs.getString("${i}_name", null) ?: return@mapNotNull null,
                bandCount       = prefs.getInt("${i}_bandCount",        16),
                lifetimeMs      = prefs.getLong("${i}_lifetimeMs",      500L),
                circlesPerBand  = prefs.getInt("${i}_circlesPerBand",   1),
                minRadiusPx     = prefs.getFloat("${i}_minRadiusPx",    10f),
                maxRadiusPx     = prefs.getFloat("${i}_maxRadiusPx",    160f),
                placement       = prefs.getFloat("${i}_placement",      0.3f),
                sensitivity     = prefs.getFloat("${i}_sensitivity",    1.0f),
                colorScheme     = try { ColorScheme.valueOf(
                    prefs.getString("${i}_colorScheme", ColorScheme.RAINBOW.name)!!)
                } catch (_: Exception) { ColorScheme.RAINBOW },
                objectShape     = try { ObjectShape.valueOf(
                    prefs.getString("${i}_objectShape", ObjectShape.CIRCLE.name)!!)
                } catch (_: Exception) { ObjectShape.CIRCLE },
                subBands        = prefs.getInt("${i}_subBands",         4),
                noiseGateDb     = prefs.getFloat("${i}_noiseGateDb",    -50f),
                mirrorMode      = try { MirrorMode.valueOf(
                    prefs.getString("${i}_mirrorMode", MirrorMode.OFF.name)!!)
                } catch (_: Exception) { MirrorMode.OFF },
                trailLength     = prefs.getInt("${i}_trailLength",      0),
                beatSensitivity = prefs.getFloat("${i}_beatSensitivity",1.3f),
                colorAnimSpeed  = prefs.getFloat("${i}_colorAnimSpeed", 0f),
                showWaveform    = prefs.getBoolean("${i}_showWaveform", false)
            )
        } catch (_: Exception) { null }
    }
}

fun savePresets(context: Context, presets: List<NamedPreset>) {
    val prefs = context.getSharedPreferences(PREFS_PRESETS, Context.MODE_PRIVATE)
    prefs.edit().apply {
        clear()
        putInt("count", presets.size)
        presets.forEachIndexed { i, p ->
            putString("${i}_name",             p.name)
            putInt("${i}_bandCount",           p.bandCount)
            putLong("${i}_lifetimeMs",         p.lifetimeMs)
            putInt("${i}_circlesPerBand",      p.circlesPerBand)
            putFloat("${i}_minRadiusPx",       p.minRadiusPx)
            putFloat("${i}_maxRadiusPx",       p.maxRadiusPx)
            putFloat("${i}_placement",         p.placement)
            putFloat("${i}_sensitivity",       p.sensitivity)
            putString("${i}_colorScheme",      p.colorScheme.name)
            putString("${i}_objectShape",      p.objectShape.name)
            putInt("${i}_subBands",            p.subBands)
            putFloat("${i}_noiseGateDb",       p.noiseGateDb)
            putString("${i}_mirrorMode",       p.mirrorMode.name)
            putInt("${i}_trailLength",         p.trailLength)
            putFloat("${i}_beatSensitivity",   p.beatSensitivity)
            putFloat("${i}_colorAnimSpeed",    p.colorAnimSpeed)
            putBoolean("${i}_showWaveform",    p.showWaveform)
        }
        apply()
    }
}

// ── Built-in colour themes ────────────────────────────────────────────────────

data class Theme(
    val name:     String,
    val emoji:    String,
    val gradient: List<Color>,
    val preset:   NamedPreset
)

val builtInThemes = listOf(
    Theme(
        name = "Neon Noir", emoji = "🌃",
        gradient = listOf(Color(0xFF7C6FFF), Color(0xFFFF6BFF), Color(0xFF6BFFFF)),
        preset = NamedPreset("Neon Noir",  16, 600L,  1, 15f, 180f, 0.5f, 1.2f,
            ColorScheme.RAINBOW, ObjectShape.SPHERE, 6, -55f,
            MirrorMode.OFF, 0, 1.3f, 0f, false)
    ),
    Theme(
        name = "Solar Flare", emoji = "☀️",
        gradient = listOf(Color(0xFFFF6B00), Color(0xFFFFCC00), Color(0xFFFF2200)),
        preset = NamedPreset("Solar Flare", 12, 400L, 2, 20f, 200f, 0.7f, 1.5f,
            ColorScheme.INVERSE_RAINBOW, ObjectShape.CIRCLE, 8, -50f,
            MirrorMode.OFF, 0, 1.3f, 0f, false)
    ),
    Theme(
        name = "Arctic", emoji = "🧊",
        gradient = listOf(Color(0xFF42E5F5), Color(0xFF0080FF), Color(0xFFFFFFFF)),
        preset = NamedPreset("Arctic",     20, 700L,  1,  8f, 140f, 0.3f, 0.9f,
            ColorScheme.INVERSE_RAINBOW, ObjectShape.SPHERE, 4, -52f,
            MirrorMode.HORIZONTAL, 0, 1.3f, 0.5f, false)
    ),
    Theme(
        name = "Deep Ocean", emoji = "🌊",
        gradient = listOf(Color(0xFF004466), Color(0xFF0088AA), Color(0xFF00FFCC)),
        preset = NamedPreset("Deep Ocean", 10, 1000L, 1, 20f, 220f, 0.4f, 1.0f,
            ColorScheme.RAINBOW, ObjectShape.SPHERE, 8, -58f,
            MirrorMode.OFF, 3, 1.3f, 0f, false)
    ),
    Theme(
        name = "Classic", emoji = "📺",
        gradient = listOf(Color(0xFFFF0000), Color(0xFF00FF00), Color(0xFF0000FF)),
        preset = NamedPreset("Classic",    24, 200L,  1, 10f, 120f, 0.0f, 1.0f,
            ColorScheme.RAINBOW, ObjectShape.BOX_2D, 1, -50f,
            MirrorMode.OFF, 0, 1.3f, 0f, false)
    )
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PresetsScreen(
    currentSettings:  Settings,
    onApplySettings:  (Settings) -> Unit,
    onClose:          () -> Unit
) {
    val context  = LocalContext.current
    var presets  by remember { mutableStateOf(loadPresets(context)) }
    var showSaveDialog    by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Int?>(null) }

    fun deletePreset(index: Int) {
        presets = presets.toMutableList().also { it.removeAt(index) }
        savePresets(context, presets)
    }

    fun savePreset(name: String) {
        if (name.isBlank()) return
        val trimmed = name.trim().take(24)
        val updated = presets.toMutableList()
        val existing = updated.indexOfFirst { it.name.equals(trimmed, ignoreCase = true) }
        if (existing >= 0) {
            updated[existing] = currentSettings.toPreset(trimmed)
        } else {
            if (updated.size >= MAX_PRESETS) updated.removeAt(updated.size - 1)
            updated.add(0, currentSettings.toPreset(trimmed))
        }
        presets = updated
        savePresets(context, presets)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(BgColor).padding(horizontal = 20.dp),
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
                Text("PRESETS", color = UiText, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { showSaveDialog = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = UiAccent),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("+ SAVE", fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp, letterSpacing = 2.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap a preset to load it. Tap the built-in themes for an instant look.",
                color = UiSubtle, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Built-in themes ───────────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("COLOUR THEMES", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(builtInThemes) { _, theme ->
                    ThemeCard(theme = theme, onClick = {
                        onApplySettings(theme.preset.toSettings(currentSettings))
                        onClose()
                    })
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Saved presets heading ─────────────────────────────────────────────
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("MY PRESETS", color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Text("${presets.size} / $MAX_PRESETS",
                    color = UiAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(10.dp))
        }

        // Empty state
        if (presets.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(BgCard, RoundedCornerShape(14.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎛", fontSize = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("No saved presets yet", color = UiText,
                        fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(6.dp))
                    Text("Dial in your perfect settings, then tap  + SAVE  above.",
                        color = UiSubtle, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                }
            }
        }

        // ── Saved preset cards ────────────────────────────────────────────────
        itemsIndexed(presets) { index, preset ->
            PresetCard(
                preset  = preset,
                onLoad  = {
                    onApplySettings(preset.toSettings(currentSettings))
                    onClose()
                },
                onDelete = { showDeleteConfirm = index }
            )
            Spacer(Modifier.height(10.dp))
        }

        item { Spacer(Modifier.height(40.dp)) }
    }

    // ── Save name dialog ──────────────────────────────────────────────────────
    if (showSaveDialog) {
        SavePresetDialog(
            onSave    = { name -> savePreset(name); showSaveDialog = false },
            onDismiss = { showSaveDialog = false }
        )
    }

    // ── Delete confirmation ───────────────────────────────────────────────────
    showDeleteConfirm?.let { index ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            containerColor   = BgCard,
            title = {
                Text("Delete Preset?", color = UiText,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("\"${presets.getOrNull(index)?.name}\" will be permanently removed.",
                    color = UiSubtle, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(onClick = { deletePreset(index); showDeleteConfirm = null }) {
                    Text("DELETE", color = DangerC,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("CANCEL", color = UiSubtle, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

// ── Theme card ────────────────────────────────────────────────────────────────

@Composable
private fun ThemeCard(theme: Theme, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .border(1.dp, UiSubtle.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(theme.gradient)))
        Spacer(Modifier.height(8.dp))
        Text(theme.emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(theme.name, color = UiText, fontSize = 10.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Saved preset card ─────────────────────────────────────────────────────────

@Composable
private fun PresetCard(preset: NamedPreset, onLoad: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BgCard)
            .clickable(onClick = onLoad)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp)
            .background(UiAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center) {
            Text(shapeEmoji(preset.objectShape), fontSize = 18.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(preset.name, color = UiText, fontSize = 14.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(presetSummary(preset), color = UiSubtle, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text("LOAD →", color = UiAccent, fontSize = 10.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = onDelete,
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier.size(32.dp)) {
            Text("✕", color = UiSubtle, fontSize = 14.sp)
        }
    }
}

// ── Save dialog ───────────────────────────────────────────────────────────────

@Composable
private fun SavePresetDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()
            .background(BgCard, RoundedCornerShape(16.dp)).padding(24.dp)) {
            Text("SAVE PRESET", color = UiText, fontSize = 16.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp)
            Spacer(Modifier.height(6.dp))
            Text("Give this configuration a name", color = UiSubtle,
                fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name, onValueChange = { if (it.length <= 24) name = it },
                placeholder = { Text("e.g. My Party Setup", color = UiSubtle,
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = UiAccent, unfocusedBorderColor = UiSubtle,
                    focusedTextColor     = UiText,   unfocusedTextColor   = UiText,
                    cursorColor          = UiAccent),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            )
            Spacer(Modifier.height(6.dp))
            Text("${name.length}/24", color = UiSubtle, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, modifier = Modifier.align(Alignment.End))
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = UiSubtle, fontFamily = FontFamily.Monospace) }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { if (name.isNotBlank()) onSave(name) },
                    enabled = name.isNotBlank(), shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = UiAccent,
                        disabledContainerColor = UiSubtle.copy(alpha = 0.3f))) {
                    Text("SAVE", fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
        }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

private fun shapeEmoji(shape: ObjectShape) = when (shape) {
    ObjectShape.CIRCLE -> "●"; ObjectShape.STAR -> "★"; ObjectShape.BOX_2D -> "■"
    ObjectShape.BOX_3D -> "⬡"; ObjectShape.SPHERE -> "◉"
}

private fun presetSummary(p: NamedPreset): String {
    val scheme = if (p.colorScheme == ColorScheme.RAINBOW) "Rainbow" else "Inverse"
    val shape  = p.objectShape.name.lowercase().replace("_", " ")
        .replaceFirstChar { it.uppercase() }
    val ms     = if (p.lifetimeMs >= 1000) "${"%.1f".format(p.lifetimeMs/1000.0)}s"
                 else "${p.lifetimeMs}ms"
    return "${p.bandCount} bands · $ms · $shape · $scheme"
}
