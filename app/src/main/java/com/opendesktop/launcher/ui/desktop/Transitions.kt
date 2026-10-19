package com.opendesktop.launcher.ui.desktop

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.opendesktop.launcher.data.PageTransition
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * Efekty przejść między ekranami. `position` w konwencji ViewPagera: 0 = bieżący ekran,
 * 1 = ekran po prawej, -1 = po lewej. Pager i tak przesuwa strony, więc tam, gdzie strona
 * ma „stać w miejscu”, kompensujemy translationX.
 */
fun Modifier.pageTransition(page: Int, state: PagerState, transition: PageTransition): Modifier = graphicsLayer {
    val position = (page - state.currentPage) - state.currentPageOffsetFraction
    val p = position.coerceIn(-1f, 1f)
    val ap = abs(p)
    val w = size.width
    cameraDistance = 14f * density
    when (transition) {
        // Losowe jest rozwijane do konkretnego efektu w LauncherScreen; tu tylko zabezpieczenie
        PageTransition.Slide, PageTransition.Random -> Unit
        PageTransition.Cube -> {
            transformOrigin = TransformOrigin(if (p < 0) 1f else 0f, 0.5f)
            rotationY = 90f * p
            alpha = if (ap >= 0.999f) 0f else 1f
        }
        PageTransition.CubeInside -> {
            transformOrigin = TransformOrigin(if (p < 0) 1f else 0f, 0.5f)
            rotationY = -90f * p
            alpha = if (ap >= 0.999f) 0f else 1f
        }
        PageTransition.Depth -> if (p > 0) {
            translationX = -w * p
            val s = 0.75f + 0.25f * (1 - ap)
            scaleX = s; scaleY = s
            alpha = 1 - p
        }
        PageTransition.Zoom -> {
            val s = maxOf(0.82f, 1 - ap * 0.5f)
            scaleX = s; scaleY = s
            translationX = -w * p * 0.12f
            alpha = 0.4f + 0.6f * (1 - ap)
        }
        PageTransition.Fade -> {
            translationX = -w * p
            alpha = 1 - ap
        }
        PageTransition.Rotate -> {
            transformOrigin = TransformOrigin(0.5f, 1.15f)
            rotationZ = 22f * p
        }
        PageTransition.Flip -> {
            translationX = -w * p
            rotationY = -180f * p
            alpha = if (ap < 0.5f) 1f else 0f
        }
        PageTransition.Stack -> if (p < 0) {
            translationX = -w * p
            val s = 1f - 0.18f * ap
            scaleX = s; scaleY = s
            alpha = 1f - 0.7f * ap
        }
        PageTransition.Carousel -> {
            val s = 1f - 0.22f * ap
            scaleX = s; scaleY = s
            translationX = -w * p * 0.28f
            rotationY = -35f * p
            alpha = 1f - 0.35f * ap
        }
        PageTransition.Accordion -> {
            transformOrigin = TransformOrigin(if (p < 0) 0f else 1f, 0.5f)
            scaleX = 1f - ap
        }
        PageTransition.Wave -> {
            translationY = sin(p * PI.toFloat()) * size.height * 0.06f
            rotationZ = 6f * p
            val s = 1f - 0.1f * ap
            scaleX = s; scaleY = s
        }
    }
}
