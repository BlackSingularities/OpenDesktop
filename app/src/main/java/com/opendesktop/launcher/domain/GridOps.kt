package com.opendesktop.launcher.domain

import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.Page

/** Czyste operacje na siatce pulpitu – bez Androida, testowalne jednostkowo. */
object GridOps {
    fun overlaps(a: DesktopItem, x: Int, y: Int, w: Int, h: Int): Boolean =
        a.x < x + w && x < a.x + a.w && a.y < y + h && y < a.y + a.h

    fun inBounds(x: Int, y: Int, w: Int, h: Int, cols: Int, rows: Int): Boolean =
        x >= 0 && y >= 0 && x + w <= cols && y + h <= rows

    fun isFree(
        items: List<DesktopItem>, x: Int, y: Int, w: Int, h: Int,
        cols: Int, rows: Int, exclude: String? = null,
    ): Boolean = inBounds(x, y, w, h, cols, rows) && items.none { it.id != exclude && overlaps(it, x, y, w, h) }

    /** Najbliższe wolne miejsce względem preferowanej komórki (ten sam wiersz ma pierwszeństwo). */
    fun findFree(
        items: List<DesktopItem>, w: Int, h: Int, cols: Int, rows: Int,
        prefX: Int = 0, prefY: Int = 0, exclude: String? = null,
    ): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestScore = Int.MAX_VALUE
        for (y in 0..rows - h) for (x in 0..cols - w) {
            if (!isFree(items, x, y, w, h, cols, rows, exclude)) continue
            val dx = x - prefX
            val dy = y - prefY
            val score = dx * dx + dy * dy * 2
            if (score < bestScore) {
                bestScore = score
                best = x to y
            }
        }
        return best
    }

    /** Pierwsze wolne miejsce w kolejności czytania (od lewej, od góry). */
    fun firstFree(items: List<DesktopItem>, w: Int, h: Int, cols: Int, rows: Int, fromBottom: Boolean = false): Pair<Int, Int>? {
        val ys = if (fromBottom) (rows - h downTo 0) else (0..rows - h)
        for (y in ys) for (x in 0..cols - w) {
            if (isFree(items, x, y, w, h, cols, rows)) return x to y
        }
        return null
    }

    fun itemAt(items: List<DesktopItem>, cx: Int, cy: Int, exclude: String? = null): DesktopItem? =
        items.lastOrNull { it.id != exclude && cx >= it.x && cx < it.x + it.w && cy >= it.y && cy < it.y + it.h }

    /**
     * Dopasowuje elementy do nowego rozmiaru siatki. Przy wyłączonym nakładaniu konflikty są
     * przenoszone w najbliższe wolne miejsce, a to, co się nie zmieści – na kolejne ekrany.
     */
    fun normalize(pages: List<Page>, cols: Int, rows: Int, allowOverlap: Boolean): List<Page> {
        val overflow = mutableListOf<DesktopItem>()
        val fixed = pages.map { page ->
            val placed = mutableListOf<DesktopItem>()
            for (raw in page.items) {
                val w = raw.w.coerceIn(1, cols)
                val h = raw.h.coerceIn(1, rows)
                val x = raw.x.coerceIn(0, cols - w)
                val y = raw.y.coerceIn(0, rows - h)
                var item = raw.copy(x = x, y = y, w = w, h = h)
                if (!allowOverlap && !isFree(placed, x, y, w, h, cols, rows)) {
                    val spot = findFree(placed, w, h, cols, rows, x, y)
                    if (spot == null) {
                        overflow += item
                        continue
                    }
                    item = item.copy(x = spot.first, y = spot.second)
                }
                placed += item
            }
            page.copy(items = placed)
        }.toMutableList()
        for (item in overflow) {
            var ok = false
            for (i in fixed.indices) {
                val spot = findFree(fixed[i].items, item.w, item.h, cols, rows) ?: continue
                fixed[i] = fixed[i].copy(items = fixed[i].items + item.copy(x = spot.first, y = spot.second))
                ok = true
                break
            }
            if (!ok) fixed += Page(items = listOf(item.copy(x = 0, y = 0)))
        }
        return fixed
    }
}
