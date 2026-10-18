package com.opendesktop.launcher

import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.ItemType
import com.opendesktop.launcher.data.Page
import com.opendesktop.launcher.domain.GridOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GridOpsTest {
    private fun app(x: Int, y: Int, w: Int = 1, h: Int = 1) = DesktopItem(type = ItemType.App, x = x, y = y, w = w, h = h, app = "a/b")

    @Test
    fun freeCellDetection() {
        val items = listOf(app(0, 0, 2, 2))
        assertFalse(GridOps.isFree(items, 1, 1, 1, 1, 4, 4))
        assertTrue(GridOps.isFree(items, 2, 0, 2, 2, 4, 4))
        assertFalse(GridOps.isFree(items, 3, 3, 2, 1, 4, 4)) // poza siatką
    }

    @Test
    fun findsNearestFreeSpot() {
        val items = listOf(app(1, 1))
        assertEquals(0 to 1, GridOps.findFree(items, 1, 1, 3, 3, prefX = 1, prefY = 1))
    }

    @Test
    fun fullGridHasNoSpot() {
        val items = (0 until 2).flatMap { x -> (0 until 2).map { y -> app(x, y) } }
        assertNull(GridOps.findFree(items, 1, 1, 2, 2))
    }

    @Test
    fun normalizeMovesOverflowToNewPage() {
        val page = Page(items = listOf(app(0, 0), app(3, 3), app(4, 4)))
        val result = GridOps.normalize(listOf(page), cols = 2, rows = 1, allowOverlap = false)
        assertEquals(2, result.size)
        assertEquals(2, result[0].items.size)
        assertEquals(1, result[1].items.size)
        result.forEach { p -> p.items.forEach { assertTrue(GridOps.inBounds(it.x, it.y, it.w, it.h, 2, 1)) } }
    }

    @Test
    fun normalizeKeepsOverlapsWhenAllowed() {
        val page = Page(items = listOf(app(0, 0, 2, 2), app(1, 1)))
        val result = GridOps.normalize(listOf(page), cols = 4, rows = 4, allowOverlap = true)
        assertEquals(1, result.size)
        assertEquals(1, result[0].items[1].x)
    }
}
