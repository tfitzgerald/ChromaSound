package com.chromasound.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromasound.app.model.BandDefinition
import kotlin.math.roundToInt

// ── Palette (matches ChromaSoundScreen) ──────────────────────────────────────
private val BgColor   = Color(0xFF050508)
private val BgCard    = Color(0xFF0F0F1A)
private val UiAccent  = Color(0xFF7C6FFF)
private val UiText    = Color(0xFFE0DFF8)
private val UiSubtle  = Color(0xFF5A5870)
private val UiDivider = Color(0xFF1E1E2E)

/**
 * Full-screen settings sheet.
 *
 * Displayed when the user taps the ⚙ button on the main running screen.
 * Currently contains a single slider controlling the number of frequency bands.
 *
 * @param currentBandCount  The value currently active in the ViewModel.
 * @param onBandCountChange Called in real-time as the slider moves.
 * @param onClose           Called when the user taps Done or the back arrow.
 */
@Composable
fun SettingsScreen(
    currentBandCount: Int,
    onBandCountChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    // Local slider state so the UI stays snappy; ViewModel is updated on every change
    var sliderValue by remember(currentBandCount) { mutableStateOf(currentBandCount.toFloat()) }
    val bandCount = sliderValue.roundToInt()

    // Compute the band edges for the current slider position so the user can
    // see the frequency breakdown update in real time as they drag.
    val bands = remember(bandCount) { BandDefinition.build(bandCount) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(56.dp))

        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
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

            Text(
                "SETTINGS",
                color = UiText, fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace, letterSpacing = 4.sp
            )

            Spacer(Modifier.weight(1f))
            // Invisible spacer to balance the back button
            Spacer(Modifier.width(80.dp))
        }

        Spacer(Modifier.height(48.dp))

        // ── Band count card ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "FREQUENCY BANDS",
                        color = UiSubtle, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "30 Hz  –  11 kHz",
                        color = UiText, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                // Large band-count display
                Text(
                    "$bandCount",
                    color = UiAccent, fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Slider ────────────────────────────────────────────────────────
            Slider(
                value = sliderValue,
                onValueChange = { v ->
                    sliderValue = v
                    onBandCountChange(v.roundToInt())
                },
                valueRange = BandDefinition.MIN_BANDS.toFloat()..BandDefinition.MAX_BANDS.toFloat(),
                steps = BandDefinition.MAX_BANDS - BandDefinition.MIN_BANDS - 1,
                colors = SliderDefaults.colors(
                    thumbColor             = UiAccent,
                    activeTrackColor       = UiAccent,
                    inactiveTrackColor     = UiSubtle.copy(alpha = 0.35f),
                    activeTickColor        = Color.Transparent,
                    inactiveTickColor      = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Min / max labels
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("${BandDefinition.MIN_BANDS}", color = UiSubtle,
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("${BandDefinition.MAX_BANDS}", color = UiSubtle,
                    fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(28.dp))
            Divider(color = UiDivider)
            Spacer(Modifier.height(20.dp))

            // ── Live band breakdown table ─────────────────────────────────────
            Text(
                "BAND BREAKDOWN",
                color = UiSubtle, fontSize = 9.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 3.sp
            )
            Spacer(Modifier.height(12.dp))

            // Show all bands in a compact two-column grid
            val halfCount = (bandCount + 1) / 2
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                // Left column: bands 0 .. halfCount-1
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until halfCount) {
                        BandRow(i + 1, bands.lowerHz[i], bands.upperHz[i])
                    }
                }
                // Right column: remaining bands
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in halfCount until bandCount) {
                        BandRow(i + 1, bands.lowerHz[i], bands.upperHz[i])
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        // ── Done button ───────────────────────────────────────────────────────
        Button(
            onClick = onClose,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = UiAccent),
            modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
        ) {
            Text("DONE", fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun BandRow(number: Int, lowHz: Float, highHz: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$number.",
            color = UiSubtle, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(20.dp)
        )
        Text(
            "${formatBandHz(lowHz)} – ${formatBandHz(highHz)}",
            color = UiText, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatBandHz(hz: Float): String =
    if (hz >= 1000f) "${"%.1f".format(hz / 1000f)}k" else "${hz.roundToInt()}"
