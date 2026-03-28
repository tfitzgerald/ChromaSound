package com.chromasound.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Palette (matches the rest of the app) ─────────────────────────────────────
private val BgColor  = Color(0xFF050508)
private val BgCard   = Color(0xFF0F0F1A)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)
private val TipGreen = Color(0xFF00CC77)
private val WarnAmb  = Color(0xFFFFAA33)

// ── Data model ────────────────────────────────────────────────────────────────

private data class HelpItem(
    val emoji:       String,
    val title:       String,
    val range:       String,       // e.g. "2 – 24"   or ""
    val default:     String,       // e.g. "16"       or ""
    val description: String,
    val tip:         String = ""
)

private data class HelpSection(
    val heading: String,
    val intro:   String = "",
    val items:   List<HelpItem>
)

// ── All help content ──────────────────────────────────────────────────────────

private val sections = listOf(

    HelpSection(
        heading = "How ChromaSound Works",
        intro   = "ChromaSound listens through your microphone, splits the sound into " +
                  "frequency bands, and draws a coloured shape for each band that is " +
                  "active. The louder and more complex the sound, the more shapes appear " +
                  "and the larger they grow.",
        items   = listOf(
            HelpItem(
                emoji       = "🎤",
                title       = "Microphone Input",
                range       = "",
                default     = "",
                description = "ChromaSound captures raw audio at 44,100 samples per second. " +
                              "Each chunk of 4,096 samples is processed in one frame " +
                              "(about 93 ms). No audio is ever recorded or saved — " +
                              "everything happens in memory in real time.",
                tip         = "Point your phone toward a speaker or instrument for the best visual response."
            ),
            HelpItem(
                emoji       = "📊",
                title       = "FFT Analysis",
                range       = "",
                default     = "",
                description = "A Fast Fourier Transform (FFT) converts the raw audio into a " +
                              "spectrum — a measurement of how much energy is present at " +
                              "each frequency. ChromaSound uses this to determine which " +
                              "frequency bands are active and how loud they are.",
                tip         = "Music with a wide frequency range (bass, mids, and treble) " +
                              "will produce the most varied and colourful display."
            )
        )
    ),

    HelpSection(
        heading = "Frequency & Timing",
        items   = listOf(
            HelpItem(
                emoji       = "🎼",
                title       = "Frequency Bands",
                range       = "2 – 24",
                default     = "16",
                description = "Divides the audible range (30 Hz to 11 kHz) into this many " +
                              "bands. More bands means finer frequency detail — each band " +
                              "covers a narrower range of frequencies and triggers its own " +
                              "independent shape. Bands are spaced logarithmically, " +
                              "matching how human hearing works.",
                tip         = "16 bands is a good balance. Try 8 for a bold look with " +
                              "larger shapes, or 24 for very detailed frequency analysis."
            ),
            HelpItem(
                emoji       = "⏱",
                title       = "Circle Lifetime",
                range       = "100 ms – 2000 ms",
                default     = "500 ms",
                description = "How long each shape stays on screen before it fades out " +
                              "completely. Short lifetimes make the display react very " +
                              "quickly to sound — shapes appear and disappear almost " +
                              "instantly. Long lifetimes let shapes linger, creating a " +
                              "denser, more layered display.",
                tip         = "For music with a fast beat, try 300–400 ms. For slow " +
                              "ambient sound, 800–1200 ms creates a beautiful lingering effect."
            ),
            HelpItem(
                emoji       = "🔢",
                title       = "Objects Per Band",
                range       = "1 – 5",
                default     = "1",
                description = "The maximum number of shapes that can be shown simultaneously " +
                              "for each frequency band. At 1, each band shows at most one " +
                              "shape at a time and new sound simply refreshes it. At higher " +
                              "values, multiple shapes can stack up per band while the " +
                              "older ones are still fading out.",
                tip         = "Values of 2 or 3 add depth and layering. Values of 4–5 " +
                              "can be very dense — best with fewer frequency bands."
            )
        )
    ),

    HelpSection(
        heading = "Size & Position",
        items   = listOf(
            HelpItem(
                emoji       = "🔵",
                title       = "Minimum Size",
                range       = "5 – 120 px",
                default     = "10 px",
                description = "The radius of a shape when the sound in that band is just " +
                              "barely above the detection threshold. Very quiet sounds " +
                              "produce shapes close to this size.",
                tip         = "Increase this if shapes feel too small to see clearly in " +
                              "a quiet environment."
            ),
            HelpItem(
                emoji       = "⭕",
                title       = "Maximum Size",
                range       = "20 – 250 px",
                default     = "160 px",
                description = "The radius of a shape when the sound in that band is at its " +
                              "loudest detectable level. A wide gap between minimum and " +
                              "maximum size makes the shapes very responsive to volume — " +
                              "loud sounds produce dramatically larger shapes.",
                tip         = "Try Min=20, Max=200 for very expressive size variation that " +
                              "reacts dramatically to loud sounds."
            ),
            HelpItem(
                emoji       = "🎲",
                title       = "Circle Placement",
                range       = "Grid-locked → Full random",
                default     = "0.3 (Slight)",
                description = "Controls how randomly shapes scatter from their band's " +
                              "centre column. At zero (Grid-locked), every shape for a " +
                              "given band appears in the same vertical column — very " +
                              "ordered and structured. As you increase it, shapes scatter " +
                              "both left-right and up-down, spreading across the whole " +
                              "screen at maximum.",
                tip         = "Grid-locked looks great with the 3D Box shape for a " +
                              "structured geometric display. Full Random with Circles " +
                              "creates a galaxy-like scattered effect."
            )
        )
    ),

    HelpSection(
        heading = "Audio Sensitivity",
        items   = listOf(
            HelpItem(
                emoji       = "🎚",
                title       = "Mic Sensitivity",
                range       = "×0.1 – ×3.0",
                default     = "×1.0",
                description = "A multiplier applied to the measured sound level before " +
                              "comparing it to the detection threshold. At ×1.0 nothing " +
                              "changes. Increasing sensitivity (×2.0, ×3.0) makes quiet " +
                              "sounds trigger and grow shapes they otherwise would not — " +
                              "useful in quiet environments. Reducing sensitivity " +
                              "(×0.5, ×0.1) means only loud sounds produce shapes.",
                tip         = "If you are in a quiet room and few shapes appear, try ×2.0 " +
                              "or ×3.0. If shapes are constantly at maximum size because " +
                              "the environment is loud, try ×0.5."
            )
        )
    ),

    HelpSection(
        heading = "Shape",
        intro   = "Choose the visual shape used to represent each active frequency band. " +
                  "All shapes use the same sub-band radial shading system.",
        items   = listOf(
            HelpItem(
                emoji       = "●",
                title       = "Circle",
                range       = "",
                default     = "Default",
                description = "A glowing disc with a radial gradient from a bright white " +
                              "centre to the band's colour at the edge. An outer glow ring " +
                              "extends beyond the main disc for a neon light effect. The " +
                              "cleanest and most readable shape.",
                tip         = "Best all-around shape for most music types."
            ),
            HelpItem(
                emoji       = "★",
                title       = "Star",
                range       = "",
                default     = "",
                description = "A five-pointed star filled with the sub-band radial gradient. " +
                              "The inner radius is about 42% of the outer radius, giving a " +
                              "sharp, defined star shape. An outer glow ring matches the " +
                              "circle shape for visual consistency.",
                tip         = "Stars at high placement randomness look spectacular with " +
                              "electronic music."
            ),
            HelpItem(
                emoji       = "■",
                title       = "2D Box",
                range       = "",
                default     = "",
                description = "A filled square with a radial gradient emanating from its " +
                              "centre, and outlined edges. The square's side length is " +
                              "proportional to the band's radius, so louder sounds produce " +
                              "larger squares.",
                tip         = "2D Box at zero placement (Grid-locked) creates clean " +
                              "columns of squares like a traditional spectrum analyser."
            ),
            HelpItem(
                emoji       = "⬡",
                title       = "3D Box  (Rotating)",
                range       = "",
                default     = "",
                description = "A wireframe cube that continuously rotates around its " +
                              "vertical axis at one full revolution every 2 seconds. Uses " +
                              "perspective projection so edges closer to the viewer appear " +
                              "larger. Front edges are brighter than back edges. Each edge " +
                              "is individually tinted by the sub-band energy at its depth.",
                tip         = "The 3D Box looks best with sub-band shading set to 4 or more " +
                              "rings, which makes each face of the cube glow differently."
            ),
            HelpItem(
                emoji       = "◉",
                title       = "Sphere  (Rotating)",
                range       = "",
                default     = "",
                description = "A shaded sphere with 3 latitude lines and 3 longitude lines " +
                              "that rotate continuously. The base disc uses sub-band radial " +
                              "shading. Each latitude and longitude line's brightness is " +
                              "individually driven by its corresponding sub-band energy — " +
                              "so different lines can glow at different intensities " +
                              "depending on the frequency content.",
                tip         = "Sphere with 8+ sub-band rings and a long lifetime creates " +
                              "a beautiful slowly-evolving planet-like display."
            )
        )
    ),

    HelpSection(
        heading = "Sub-Band Shading",
        items   = listOf(
            HelpItem(
                emoji       = "🌈",
                title       = "Sub-Band Shading",
                range       = "1 (Off) – 12 rings",
                default     = "4",
                description = "Each frequency band is divided into this many sub-slices. " +
                              "The energy in each slice determines the brightness of one " +
                              "radial ring in the shape — innermost ring = lowest frequency " +
                              "within the band, outermost ring = highest. This means a " +
                              "shape's internal shading reflects the detailed spectral " +
                              "content within its band, not just the overall level.\n\n" +
                              "At 1 (Off), all shapes have a solid uniform colour with a " +
                              "white centre highlight. At 12, each shape has 12 " +
                              "independently lit rings — very fine internal detail.",
                tip         = "4–6 rings is a sweet spot — visible detail without " +
                              "overwhelming the overall colour. Try 1 for a cleaner " +
                              "look, or 12 for maximum visual complexity."
            )
        )
    ),

    HelpSection(
        heading = "Colour",
        items   = listOf(
            HelpItem(
                emoji       = "🟣",
                title       = "Rainbow  (Color Scheme)",
                range       = "",
                default     = "Default",
                description = "Maps frequencies to colours following the visible light " +
                              "spectrum — bass frequencies (30 Hz) appear violet/purple, " +
                              "moving through blue, cyan, green, yellow, to red at treble " +
                              "(11 kHz). This mirrors how light works: low frequencies " +
                              "are warm/violet, high frequencies are energetic/red.",
                tip         = "Rainbow is the most intuitive mapping for musicians — " +
                              "bass is cool and deep, treble is warm and bright."
            ),
            HelpItem(
                emoji       = "🔴",
                title       = "Inverse Rainbow  (Color Scheme)",
                range       = "",
                default     = "",
                description = "The reverse of Rainbow — bass frequencies appear red/warm " +
                              "and treble frequencies appear violet/cool. Some people find " +
                              "this more intuitive since bass \"feels\" warm and heavy while " +
                              "high frequencies feel cool and delicate.",
                tip         = "Try Inverse Rainbow with the Sphere shape for a striking " +
                              "different look."
            ),
            HelpItem(
                emoji       = "🎨",
                title       = "Band Colours",
                range       = "",
                default     = "Auto",
                description = "Opens the Band Colours screen where you can assign a " +
                              "completely custom colour to any individual frequency band " +
                              "using a full HSV colour picker. Tap any band's colour " +
                              "swatch to open the picker — drag the hue bar to choose a " +
                              "colour, then drag the square to adjust brightness and " +
                              "saturation. Bands without an override continue to follow " +
                              "the selected colour scheme automatically.\n\n" +
                              "Tap ✕ next to any band to remove its override. " +
                              "Tap RESET ALL to clear every override at once.",
                tip         = "Try making all bass bands red and all treble bands blue " +
                              "for a dramatic visual contrast, independent of the " +
                              "colour scheme setting."
            )
        )
    ),

    HelpSection(
        heading = "Tips & Tricks",
        items   = listOf(
            HelpItem(
                emoji       = "🎵",
                title       = "Best Settings for Music",
                range       = "",
                default     = "",
                description = "Bands: 16–20  ·  Lifetime: 400–600 ms  ·  " +
                              "Placement: 0.3–0.5  ·  Sub-Band Shading: 4–6  ·  " +
                              "Shape: Sphere or Circle  ·  Sensitivity: ×1.0",
                tip         = "Try pointing the phone at your speaker and lowering the " +
                              "lights for a full light-show effect."
            ),
            HelpItem(
                emoji       = "🗣",
                title       = "Best Settings for Voice",
                range       = "",
                default     = "",
                description = "Bands: 8–12  ·  Lifetime: 500–800 ms  ·  " +
                              "Placement: 0.2  ·  Sensitivity: ×1.5–×2.0  ·  " +
                              "Sub-Band Shading: 1–2  ·  Shape: Circle or Star",
                tip         = "Voice content is concentrated below 4 kHz — fewer bands " +
                              "means each active band produces a larger, more visible shape."
            ),
            HelpItem(
                emoji       = "🌙",
                title       = "Best Settings for Ambient / Quiet",
                range       = "",
                default     = "",
                description = "Sensitivity: ×2.0–×3.0  ·  Lifetime: 800–1500 ms  ·  " +
                              "Min Size: 20–40 px  ·  Placement: 0.7–1.0  ·  " +
                              "Shape: Sphere  ·  Sub-Band Shading: 6–8",
                tip         = "High sensitivity and long lifetime let even very quiet " +
                              "ambient sounds produce a gentle, slowly drifting display."
            ),
            HelpItem(
                emoji       = "📺",
                title       = "Classic Spectrum Analyser Look",
                range       = "",
                default     = "",
                description = "Shape: 2D Box  ·  Placement: 0.0 (Grid-locked)  ·  " +
                              "Bands: 24  ·  Lifetime: 200–300 ms  ·  " +
                              "Sub-Band Shading: 1 (Off)  ·  Color Scheme: Rainbow",
                tip         = "This recreates the look of a traditional hardware spectrum " +
                              "analyser with crisp columns of coloured blocks."
            )
        )
    )
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun HelpScreen(onClose: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(56.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onClose,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UiText),
                    border = androidx.compose.foundation.BorderStroke(1.dp, UiSubtle),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "← BACK",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "HELP",
                    color = UiText, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(80.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "A guide to every control in ChromaSound",
                color = UiSubtle,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── Sections ──────────────────────────────────────────────────────────
        sections.forEach { section ->

            // Section heading
            item {
                SectionHeading(section.heading)
                if (section.intro.isNotEmpty()) {
                    Text(
                        section.intro,
                        color = UiSubtle,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }
            }

            // Help items
            section.items.forEach { item ->
                item {
                    HelpCard(item)
                    Spacer(Modifier.height(10.dp))
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Text(
                "ChromaSound  ·  Real-Time Audio Visualiser",
                color = UiSubtle.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Version 1.5.0",
                color = UiSubtle.copy(alpha = 0.5f),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Section heading ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeading(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, UiAccent.copy(alpha = 0.4f))
                    )
                )
        )
        Text(
            "  $text  ",
            color = UiAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(UiAccent.copy(alpha = 0.4f), Color.Transparent)
                    )
                )
        )
    }
}

// ── Help card ─────────────────────────────────────────────────────────────────

@Composable
private fun HelpCard(item: HelpItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgCard, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        // Title row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Emoji badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(UiAccent.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                    .border(1.dp, UiAccent.copy(alpha = 0.25f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = UiText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                // Range / default badges
                if (item.range.isNotEmpty() || item.default.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (item.range.isNotEmpty()) {
                            Badge(label = "Range", value = item.range, color = UiSubtle)
                        }
                        if (item.default.isNotEmpty()) {
                            Badge(label = "Default", value = item.default, color = UiAccent)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = UiSubtle.copy(alpha = 0.15f), thickness = 1.dp)
        Spacer(Modifier.height(12.dp))

        // Description — handle \n for paragraph breaks
        item.description.split("\n\n").forEach { paragraph ->
            Text(
                paragraph.trim(),
                color = UiText.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Tip box (if present)
        if (item.tip.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TipGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, TipGreen.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("💡", fontSize = 14.sp, modifier = Modifier.padding(top = 1.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    item.tip,
                    color = TipGreen.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Badge ─────────────────────────────────────────────────────────────────────

@Composable
private fun Badge(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label.uppercase(),
            color = color.copy(alpha = 0.7f),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
