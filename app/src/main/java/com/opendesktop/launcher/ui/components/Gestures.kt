package com.opendesktop.launcher.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity

/**
 * Jeden detektor dla ikon: tap, długie przytrzymanie (menu) i przeciąganie po przytrzymaniu.
 * Przed przytrzymaniem niczego nie konsumuje, więc przewijanie ekranów i gest szuflady działają.
 * Ruch palca liczony jest w układzie okna – pomniejszenie pulpitu przy wejściu w edycję (w trakcie
 * przytrzymania) przesuwa ikonę pod palcem, ale nie może samo z siebie zacząć przeciągania.
 * [holdMillis] skraca czas przytrzymania (np. w trybie edycji przeciąganie zaczyna się szybciej).
 */
@Composable
fun Modifier.itemGestures(
    key: Any?,
    enabled: Boolean = true,
    onPress: (Boolean) -> Unit = {},
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onSwipeUp: (() -> Unit)? = null,
    holdMillis: Long? = null,
): Modifier {
    val press by rememberUpdatedState(onPress)
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLongPress)
    val dragStart by rememberUpdatedState(onDragStart)
    val dragEnd by rememberUpdatedState(onDragEnd)
    val swipeUp by rememberUpdatedState(onSwipeUp)
    val hold by rememberUpdatedState(holdMillis)
    val coords = remember { arrayOfNulls<LayoutCoordinates>(1) }
    if (!enabled) return this
    return this.onGloballyPositioned { coords[0] = it }.pointerInput(key) {
        // Położenie palca w oknie (gdy brak współrzędnych – lokalne)
        fun root(p: Offset): Offset = coords[0]?.takeIf { it.isAttached }?.localToRoot(p) ?: p
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val downRoot = root(down.position)
            press(true)
            val slop = viewConfiguration.touchSlop
            // 0 = przytrzymanie, 1 = tap, 2 = ruch/anulowanie, 3 = swipe up
            var outcome = 0
            var swipeDetected = false
            withTimeoutOrNull(hold ?: viewConfiguration.longPressTimeoutMillis) {
                while (outcome == 0) {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull { it.id == down.id }
                    val delta = c?.position?.minus(down.position)
                    outcome = when {
                        c == null -> 2
                        !c.pressed -> if (c.isConsumed) 2 else 1
                        c.isConsumed -> 2
                        delta != null && delta.getDistance() > slop -> {
                            // Wykryj swipe w górę: ruch pionowy > poziomy i kierunek w górę
                            // Ten sam próg co przeciąganie pulpitu – inaczej pulpit przejmuje gest i otwiera szufladę
                            if (swipeUp != null && -delta.y > kotlin.math.abs(delta.x)) {
                                // Konsumujemy, żeby ten sam ruch nie otworzył też szuflady
                                c.consume()
                                swipeDetected = true
                                3
                            } else 2
                        }
                        else -> 0
                    }
                }
            }
            if (outcome == 3 && swipeDetected) {
                press(false)
                swipeUp?.invoke()
                return@awaitEachGesture
            }
            if (outcome != 0) {
                press(false)
                if (outcome == 1) tap()
                return@awaitEachGesture
            }
            long()
            var dragging = false
            // finally: przeciąganie kończy się zawsze, także gdy element zniknie z ekranu w trakcie
            // (anulowana korutyna) – inaczej podgląd ikony „zamarza”, a pulpit blokuje gesty
            try {
                while (true) {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                    if (!c.pressed) {
                        c.consume()
                        break
                    }
                    if (!dragging && (root(c.position) - downRoot).getDistance() > slop) {
                        dragging = true
                        press(false)
                        dragStart()
                    }
                    c.consume()
                }
            } finally {
                press(false)
                if (dragging) dragEnd()
            }
        }
    }
}

/**
 * Gesty dla widoków rysowanych na Canvasie (kula, helisa, koło): tap/przytrzymanie w punkcie
 * oraz przeciąganie z prędkością końcową do bezwładności.
 */
@Composable
fun Modifier.canvasGestures(
    onTouch: (Boolean) -> Unit = {},
    onTap: (Offset) -> Unit,
    onLongPress: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onFling: (Velocity) -> Unit,
): Modifier {
    val touch by rememberUpdatedState(onTouch)
    val tap by rememberUpdatedState(onTap)
    val long by rememberUpdatedState(onLongPress)
    val drag by rememberUpdatedState(onDrag)
    val fling by rememberUpdatedState(onFling)
    return this.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            touch(true)
            val tracker = VelocityTracker()
            tracker.addPosition(down.uptimeMillis, down.position)
            val slop = viewConfiguration.touchSlop
            var outcome = 0
            withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                while (outcome == 0) {
                    val ev = awaitPointerEvent()
                    val c = ev.changes.firstOrNull { it.id == down.id }
                    if (c != null) tracker.addPosition(c.uptimeMillis, c.position)
                    outcome = when {
                        c == null -> 3
                        !c.pressed -> 1
                        (c.position - down.position).getDistance() > slop -> 2
                        else -> 0
                    }
                }
            }
            when (outcome) {
                1 -> tap(down.position)
                0 -> {
                    long(down.position)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        c.consume()
                        if (!c.pressed) break
                    }
                }
                2 -> {
                    var last = down.position
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull { it.id == down.id } ?: break
                        tracker.addPosition(c.uptimeMillis, c.position)
                        if (!c.pressed) break
                        drag(c.position - last)
                        last = c.position
                        c.consume()
                    }
                    fling(tracker.calculateVelocity())
                }
            }
            touch(false)
        }
    }
}
