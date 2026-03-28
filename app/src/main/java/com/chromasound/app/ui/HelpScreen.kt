package com.chromasound.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgColor  = Color(0xFF050508)
private val BgCard   = Color(0xFF0F0F1A)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

@Composable
fun HelpScreen(onClose: () -> Unit) {
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
                    Text("← BACK", fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.weight(1f))
                Text("HELP", color = UiText, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(80.dp))
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── What is ChromaSound ───────────────────────────────────────────────
        item {
            SectionHeading("WHAT IS CHROMASOUND")
            HelpCard("ChromaSound listens to sound through your microphone and transforms it into a real-time display of glowing shapes. Each frequency range — from deep bass to bright treble — gets its own column of shapes. The louder a frequency, the bigger and brighter its shape.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Getting started ───────────────────────────────────────────────────
        item {
            SectionHeading("GETTING STARTED")
            HelpCard("Tap TAP TO LISTEN on the main screen and grant microphone permission when prompted. Point your phone toward music, speak near it, or play audio through the phone's speaker. Shapes will begin appearing within one audio frame (~90ms).\n\nTo stop, tap the STOP button at the bottom of the visualiser. Your settings are saved automatically — they'll be there next time you open the app.")
            Spacer(Modifier.height(20.dp))
        }

        // ── How it works ─────────────────────────────────────────────────────
        item {
            SectionHeading("HOW IT WORKS")
            HelpCard("ChromaSound captures audio at 44,100 Hz and processes each frame of 4,096 samples through a Fast Fourier Transform (FFT). This splits the sound into its component frequencies.\n\nThe frequency range 30 Hz – 11 kHz is divided into up to 24 bands. For each band, the loudest frequency bin is found and compared against the noise gate threshold. If it's loud enough, a shape spawns at a position within that band's vertical column and grows to a size proportional to the decibel level.\n\nEach shape fades out over its configured lifetime and is then removed.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Settings overview ─────────────────────────────────────────────────
        item {
            SectionHeading("SETTINGS — OVERVIEW")
            HelpCard("Settings are organised into five sections, accessed by tapping ⚙ from the running screen or SETTINGS from the home screen:\n\n🎵 FREQUENCY & TIMING — Controls the number of frequency bands, how long shapes live, and how many can appear per band.\n\n📐 SIZE & POSITION — Controls the minimum and maximum shape radius and how randomly shapes scatter within their band column.\n\n🎙 AUDIO — Microphone sensitivity gain and the noise gate threshold.\n\n🎨 VISUAL — Sub-band shading rings, colour scheme, shape type, and per-band colour overrides.\n\n✨ EFFECTS — Mirror mode, shape trails, beat detection, colour animation, waveform overlay, oscilloscope ring mode, particle explosions, and background effects.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Frequency & timing ────────────────────────────────────────────────
        item {
            SectionHeading("FREQUENCY & TIMING")
            HelpEntry("FREQUENCY BANDS", "How many frequency columns to divide the audio into. More bands = finer frequency resolution but smaller columns. 12–16 is a good starting point.")
            HelpEntry("CIRCLE LIFETIME", "How long each shape stays on screen before fading out. Shorter = more frantic. Longer = shapes linger and overlap.")
            HelpEntry("CIRCLES PER BAND", "Maximum number of shapes that can be active simultaneously within a single frequency band. Increase for denser displays.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Size & position ───────────────────────────────────────────────────
        item {
            SectionHeading("SIZE & POSITION")
            HelpEntry("MINIMUM SIZE", "The radius a shape has at the very quietest detectable level (just above the noise gate). Increase to prevent tiny invisible shapes.")
            HelpEntry("MAXIMUM SIZE", "The radius a shape can reach at the loudest level. Must be at least 10px larger than minimum size.")
            HelpEntry("PLACEMENT", "Controls how randomly shapes scatter horizontally within their band column. Grid-locked keeps all shapes centred in their column. Full random lets them appear anywhere within the column width.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Audio ─────────────────────────────────────────────────────────────
        item {
            SectionHeading("AUDIO")
            HelpEntry("MIC SENSITIVITY", "A gain multiplier applied to the raw dB levels before comparing against the noise gate. Increase if shapes are too small or sparse. Decrease if everything is always at maximum size.")
            HelpEntry("NOISE GATE", "The minimum dB level a frequency band must reach before a shape is spawned. More negative = more sensitive (spawns on quieter sounds). Less negative = only loud sounds trigger shapes.\n\nTypical values: –70 dB captures nearly everything including room noise. –50 dB is a good default. –30 dB only reacts to genuinely loud sounds.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Visual ────────────────────────────────────────────────────────────
        item {
            SectionHeading("VISUAL")
            HelpEntry("SUB-BAND SHADING", "Each shape can be shaded with multiple concentric rings, each driven by a different frequency sub-band within that band's range. The innermost ring = lowest sub-frequency, outermost = highest. Set to 1 for a solid colour with no shading.")
            HelpEntry("COLOR SCHEME", "Rainbow maps bass to red and treble to violet. Inverse Rainbow reverses this — bass is violet, treble is red.")
            HelpEntry("OBJECT SHAPE", "Circle, Star, Box (2D), Box (3D rotating), or Sphere. All shapes use the same sub-band shading and colour logic. Sphere and 3D Box include latitude/longitude line rendering that rotates in real time.")
            HelpEntry("BAND COLOURS", "Override the colour scheme for individual frequency bands. Useful for making the bass band always glow red, or the midrange always green, regardless of the colour scheme setting.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Effects ───────────────────────────────────────────────────────────
        item {
            SectionHeading("EFFECTS")
            HelpEntry("MIRROR MODE", "Reflects shapes across one or both screen axes. Horizontal mirrors left↔right. Vertical mirrors top↔bottom. Quad mode mirrors across both, creating four-fold symmetry. Shapes are drawn at reflected coordinates — the canvas is never transformed.")
            HelpEntry("SHAPE TRAILS", "Leaves ghost copies of shapes at their previous positions. Each ghost frame is slightly more transparent than the previous one. 0 = off. 8 = longest trail.")
            HelpEntry("BEAT SENSITIVITY", "Controls how much louder than the recent RMS average a frame must be to count as a beat. Lower = fires on subtle dynamics. Higher = only hard transients. On a detected beat, all shapes pulse outward and the phone vibrates.")
            HelpEntry("BPM DISPLAY", "After two beats are detected, a BPM readout appears in the top-left of the HUD. It uses the median interval of the last 8 beats to smooth out misfires.")
            HelpEntry("COLOUR ANIMATION", "When set above 0, each shape's hue drifts continuously through the spectrum. Speed 1× = ~36° per second. Speed 3× = fast strobing rainbow.")
            HelpEntry("WAVEFORM OVERLAY", "Draws a thin glowing waveform line across the centre of the screen showing the raw audio PCM amplitude in real time. The line swings dramatically with the audio amplitude.")
            HelpEntry("OSCILLOSCOPE RING MODE", "When enabled, shapes are drawn as hollow rings rather than filled objects. The ring radius contracts and expands with the sub-band energy — louder audio makes rings expand outward. Produces a more technical, waveform-like aesthetic.")
            HelpEntry("PARTICLE EXPLOSIONS", "When a frequency band suddenly exceeds the configured threshold above its recent average (a transient — like a drum hit), a burst of 8 small particles radiates outward from that shape's position. Particles slow down (drag) and fade over about 400ms.")
            HelpEntry("PARTICLE THRESHOLD", "How much above the recent band average a hit must be to trigger a particle burst. Lower = more frequent bursts. Higher = only the sharpest transients.")
            HelpEntry("BACKGROUND EFFECT", "Four options: None (solid near-black), Starfield (80 slow-drifting stars at low opacity), Bloom (background brightness pulses with overall volume), Noise (subtle animated chromatic grain for depth).")
            Spacer(Modifier.height(20.dp))
        }

        // ── Presets ───────────────────────────────────────────────────────────
        item {
            SectionHeading("PRESETS")
            HelpCard("Presets save all your current settings as a named snapshot. Access Presets from the home screen or from the Settings hub.\n\nBuilt-in colour themes apply a curated combination of settings instantly.\n\nShare Current encodes your current settings as a CS:... code that you can send via any messaging app. Recipients paste it using Paste Code to instantly load your exact configuration.\n\nPresets are saved to your device's private storage and backed up automatically to Google Drive — they survive reinstalls and transfer to new phones.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Screenshot ────────────────────────────────────────────────────────
        item {
            SectionHeading("SCREENSHOT")
            HelpCard("Tap the 📷 camera button next to STOP to capture the current visualiser frame as a PNG. The image is saved to Pictures/ChromaSound in your photo gallery. On Android 9 and below, storage permission is required.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Haptic feedback ───────────────────────────────────────────────────
        item {
            SectionHeading("HAPTIC FEEDBACK")
            HelpCard("ChromaSound vibrates briefly (40ms) on every detected beat — the same moment shapes pulse outward. This gives a physical sense of the rhythm in your hand. No setting is needed — it works automatically with the beat detector. If your device has no vibrator, this is silently skipped.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Tips ──────────────────────────────────────────────────────────────
        item {
            SectionHeading("TIPS")
            HelpCard("For music: Point the phone toward the speaker at moderate volume. Start with Mic Sensitivity at ×1.0 and the Noise Gate at –50 dB, then adjust.\n\nFor bass-heavy music: Reduce bands to 8–12 so each bass band is wider. Increase Max Size to 200px+.\n\nFor speech: Increase sensitivity to ×2.0 and lower the noise gate to –60 dB.\n\nFor beat detection: If BPM reads 40 (minimum), reduce Beat Sensitivity. If random sounds trigger beats, increase it. The detector works best with clear kick drums or hand claps.\n\nFor best screenshot wallpapers: Enable Quad mirror mode, set Shape Trails to 4, and use the Sphere shape with the Deep Ocean or Neon Noir preset.")
            Spacer(Modifier.height(20.dp))
        }

        // ── Footer ────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(16.dp))
            Text("ChromaSound  ·  Real-Time Audio Visualiser",
                color = UiSubtle.copy(alpha = 0.5f), fontSize = 10.sp,
                fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            Text("Version 2.2.2",
                color = UiSubtle.copy(alpha = 0.5f), fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Section heading ───────────────────────────────────────────────────────────
@Composable
private fun SectionHeading(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f),
            color = UiAccent.copy(alpha = 0.3f), thickness = 1.dp)
        Text("  $title  ", color = UiAccent, fontSize = 10.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp)
        HorizontalDivider(modifier = Modifier.weight(1f),
            color = UiAccent.copy(alpha = 0.3f), thickness = 1.dp)
    }
}

// ── Help card — paragraph text ────────────────────────────────────────────────
@Composable
private fun HelpCard(text: String) {
    Column(
        Modifier.fillMaxWidth()
            .background(BgCard, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(text, color = UiSubtle, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
    }
}

// ── Help entry — label + explanation ─────────────────────────────────────────
@Composable
private fun HelpEntry(label: String, text: String) {
    Column(
        Modifier.fillMaxWidth()
            .background(BgCard, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(label, color = UiAccent, fontSize = 10.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        Text(text, color = UiSubtle, fontSize = 12.sp,
            fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
    }
    Spacer(Modifier.height(8.dp))
}
