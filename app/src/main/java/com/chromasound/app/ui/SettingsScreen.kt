package com.chromasound.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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

private val BgColor   = Color(0xFF050508)
private val BgCard    = Color(0xFF0F0F1A)
private val UiAccent  = Color(0xFF7C6FFF)
private val UiText    = Color(0xFFE0DFF8)
private val UiSubtle  = Color(0xFF5A5870)
private val UiDivider = Color(0xFF1E1E2E)

@Composable
fun SettingsScreen(
    currentBandCount: Int,
    onBandCountChange: (Int) -> Unit,
    onClose: () -> Unit
) {
    var sliderValue by remember(currentBandCount) { mutableStateOf(currentBandCount.toFloat()) }
    val bandCount = sliderValue.roundToInt()
    val bands     = remember(bandCount) { BandDefinition.build(bandCount) }

    // Build the list of band items once per bandCount change
    val bandItems = remember(bands) {
        (0 until bands.count).map { i ->
            Triple(i + 1, bands.lowerHz[i], bands.upperHz[i])
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(56.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Text("SETTINGS", color = UiText, fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(80.dp))
            }
            Spacer(Modifier.height(32.dp))
        }

        // ── Slider card ───────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                // Title + large count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("FREQUENCY BANDS",
                            color = UiSubtle, fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("30 Hz  –  11 kHz",
                            color = UiText, fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        "$bandCount",
                        color = UiAccent, fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(Modifier.height(20.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = { v ->
                        sliderValue = v
                        onBandCountChange(v.roundToInt())
                    },
                    valueRange = BandDefinition.MIN_BANDS.toFloat()..BandDefinition.MAX_BANDS.toFloat(),
                    steps     = BandDefinition.MAX_BANDS - BandDefinition.MIN_BANDS - 1,
                    colors    = SliderDefaults.colors(
                        thumbColor         = UiAccent,
                        activeTrackColor   = UiAccent,
                        inactiveTrackColor = UiSubtle.copy(alpha = 0.35f),
                        activeTickColor    = Color.Transparent,
                        inactiveTickColor  = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("${BandDefinition.MIN_BANDS}", color = UiSubtle,
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("bands", color = UiSubtle,
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text("${BandDefinition.MAX_BANDS}", color = UiSubtle,
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Band breakdown header ─────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BAND BREAKDOWN",
                    color = UiSubtle, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
                Text("$bandCount bands total",
                    color = UiAccent, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(8.dp))

            // Column headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BgCard, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("#", color = UiSubtle, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(32.dp))
                Text("LOW", color = UiSubtle, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f))
                Text("–", color = UiSubtle, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace)
                Text("HIGH", color = UiSubtle, fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }

        // ── One row per band ─────────────────────────────────────────────────
        itemsIndexed(bandItems) { index, (number, low, high) ->
            val isEven = index % 2 == 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isEven) BgCard else BgCard.copy(alpha = 0.6f)
                    )
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Band number badge
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .background(
                            UiAccent.copy(alpha = 0.15f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$number",
                        color = UiAccent, fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold)
                }
                // Low frequency
                Text(formatBandHz(low),
                    color = UiText, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("–", color = UiSubtle, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace)
                // High frequency
                Text(formatBandHz(high),
                    color = UiText, fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            // Rounded bottom corners on last row
            if (index == bandItems.size - 1) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(BgCard, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                )
            }
        }

        // ── Done button ───────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(32.dp))
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
            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun formatBandHz(hz: Float): String =
    if (hz >= 1000f) "${"%.1f".format(hz / 1000f)} kHz" else "${hz.roundToInt()} Hz"
