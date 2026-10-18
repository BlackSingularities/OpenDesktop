package com.opendesktop.launcher.ui.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.opendesktop.launcher.data.DesktopItem

enum class DragSource { Desktop, Dock, Drawer }

enum class DropZone(val label: String) { Remove("Usuń"), Uninstall("Odinstaluj"), Info("Informacje") }

data class DragPayload(
    val source: DragSource,
    val item: DesktopItem,
    val fromPageId: String?,
    /** Rozmiar podglądu w pikselach ekranu. */
    val size: Size,
)

sealed interface DropTarget {
    data class Cell(
        val pageId: String,
        val x: Int,
        val y: Int,
        val w: Int,
        val h: Int,
        val valid: Boolean,
        /** Element, z którym upuszczana aplikacja utworzy folder (lub do którego folderu trafi). */
        val mergeWith: String? = null,
    ) : DropTarget

    data class Dock(val index: Int) : DropTarget
    data class Zone(val zone: DropZone) : DropTarget
}

/**
 * Stan przeciągania wspólny dla pulpitu, doku i szuflady. Pozycja palca jest śledzona
 * w układzie współrzędnych całego okna, dzięki czemu element można przenosić między ekranami.
 */
class DragController {
    var payload by mutableStateOf<DragPayload?>(null)
    var pointer by mutableStateOf(Offset.Zero)
    /** Odległość palca od lewego górnego rogu przeciąganego elementu. */
    var grab by mutableStateOf(Offset.Zero)
    val gridBounds = mutableStateMapOf<String, Rect>()
    var dockBounds by mutableStateOf(Rect.Zero)
    val zoneBounds = mutableStateMapOf<DropZone, Rect>()
    /** Ekran utworzony automatycznie podczas przeciągania — usuwany, jeśli zostanie pusty. */
    var autoPageId: String? = null

    val active: Boolean get() = payload != null
}
