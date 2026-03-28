package com.chromasound.app.ui

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ── Palette ───────────────────────────────────────────────────────────────────
private val BgColor  = Color(0xFF050508)
private val BgCard   = Color(0xFF0F0F1A)
private val UiAccent = Color(0xFF7C6FFF)
private val UiText   = Color(0xFFE0DFF8)
private val UiSubtle = Color(0xFF5A5870)

// ── Onboarding persistence ────────────────────────────────────────────────────
private const val PREFS_ONBOARDING = "chromasound_onboarding"
private const val KEY_SEEN         = "onboarding_seen"

fun hasSeenOnboarding(context: Context): Boolean =
    context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
        .getBoolean(KEY_SEEN, false)

fun markOnboardingSeen(context: Context) =
    context.getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_SEEN, true).apply()

// ── Page data ─────────────────────────────────────────────────────────────────
private data class OnboardingPage(
    val emoji:     String,
    val title:     String,
    val subtitle:  String,
    val body:      String,
    val accent:    Color
)

private val pages = listOf(
    OnboardingPage(
        emoji    = "🎵",
        title    = "Welcome to\nChromaSound",
        subtitle = "Real-time audio visualiser",
        body     = "ChromaSound listens to sound through your microphone and transforms it into a living, breathing display of glowing shapes — one for every frequency band it hears.",
        accent   = Color(0xFF7C6FFF)
    ),
    OnboardingPage(
        emoji    = "🔬",
        title    = "How it works",
        subtitle = "FFT · Frequency bands · Shapes",
        body     = "Each audio frame is analysed using Fast Fourier Transform — splitting sound into up to 24 frequency bands from 30 Hz bass to 11 kHz treble. Louder frequencies spawn bigger, brighter shapes. Beat detection fires a pulse and haptic thump on every rhythmic hit.",
        accent   = Color(0xFF42E5F5)
    ),
    OnboardingPage(
        emoji    = "🚀",
        title    = "Get started",
        subtitle = "A few tips before you begin",
        body     = "Tap TAP TO LISTEN and grant microphone access when prompted. Point your phone toward music or speak near it. Use Settings → AUDIO to adjust sensitivity if shapes are too small or too wild. Use Settings → EFFECTS to enable mirror mode and trails for a more dramatic display.",
        accent   = Color(0xFF6BFFFF)
    )
)

// ── Screen ────────────────────────────────────────────────────────────────────
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context    = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope      = rememberCoroutineScope()

    fun finish() {
        markOnboardingSeen(context)
        onDone()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(BgColor)
    ) {
        // ── Pager ─────────────────────────────────────────────────────────────
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnboardingPage(page = pages[pageIndex])
        }

        // ── Skip button — top right ───────────────────────────────────────────
        TextButton(
            onClick  = { finish() },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 16.dp)
        ) {
            Text("SKIP", color = UiSubtle, fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        }

        // ── Bottom controls ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(pages.size) { i ->
                    val isActive = pagerState.currentPage == i
                    val accent   = pages[i].accent
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (isActive) accent else UiSubtle.copy(alpha = 0.4f)
                            )
                            .size(if (isActive) 10.dp else 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            val isLast = pagerState.currentPage == pages.size - 1
            Button(
                onClick = {
                    if (isLast) {
                        finish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                shape  = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = pages[pagerState.currentPage].accent
                ),
                modifier = Modifier.fillMaxWidth(0.65f).height(52.dp)
            ) {
                Text(
                    if (isLast) "GET STARTED" else "NEXT →",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    fontSize = 13.sp,
                    color = BgColor
                )
            }
        }
    }
}

// ── Single page layout ────────────────────────────────────────────────────────
@Composable
private fun OnboardingPage(page: OnboardingPage) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 0.88f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseInOutSine), RepeatMode.Reverse
        ), label = "s"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glowing emoji orb
        Box(
            modifier = Modifier
                .size((110 * scale).dp)
                .background(
                    Brush.radialGradient(
                        listOf(page.accent.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(page.emoji, fontSize = 52.sp)
        }

        Spacer(Modifier.height(36.dp))

        Text(
            page.title,
            color      = UiText,
            fontSize   = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace,
            textAlign  = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            page.subtitle,
            color      = page.accent,
            fontSize   = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Body text card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgCard, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Text(
                page.body,
                color      = UiSubtle,
                fontSize   = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 20.sp,
                textAlign  = TextAlign.Start
            )
        }

        // Space for bottom controls
        Spacer(Modifier.height(120.dp))
    }
}
