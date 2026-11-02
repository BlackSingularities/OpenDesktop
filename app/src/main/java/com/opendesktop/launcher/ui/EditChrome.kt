package com.opendesktop.launcher.ui

import android.app.WallpaperManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FlipToBack
import androidx.compose.material.icons.rounded.FlipToFront
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Transform
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.CallSplit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.opendesktop.launcher.ui.components.iconSizeFor
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opendesktop.launcher.data.ReturnTarget
import android.appwidget.AppWidgetProviderInfo
import com.opendesktop.launcher.R
import com.opendesktop.launcher.data.AppInfo
import com.opendesktop.launcher.data.DesktopItem
import com.opendesktop.launcher.data.DrawerAnimation
import com.opendesktop.launcher.data.GestureAction
import com.opendesktop.launcher.data.ItemType
import com.opendesktop.launcher.data.PageTransition
import com.opendesktop.launcher.data.ShortcutEntry
import com.opendesktop.launcher.data.SystemAction
import com.opendesktop.launcher.data.SystemActions
import com.opendesktop.launcher.data.findActivity
import com.opendesktop.launcher.ui.categories.AssignCategoriesDialog
import com.opendesktop.launcher.ui.categories.CategoriesScreen
import com.opendesktop.launcher.ui.components.ActionMenu
import com.opendesktop.launcher.ui.components.icon
import com.opendesktop.launcher.ui.components.AppPickerDialog
import com.opendesktop.launcher.ui.components.MenuAction
import com.opendesktop.launcher.ui.components.MenuShortcut
import com.opendesktop.launcher.ui.components.TextInputDialog
import com.opendesktop.launcher.ui.components.UndoBar
import com.opendesktop.launcher.ui.desktop.DesktopCallbacks
import com.opendesktop.launcher.ui.desktop.DesktopGrid
import com.opendesktop.launcher.ui.desktop.StableDesktopCallbacks
import com.opendesktop.launcher.ui.desktop.Dock
import com.opendesktop.launcher.ui.desktop.DragController
import com.opendesktop.launcher.ui.desktop.DragPayload
import com.opendesktop.launcher.ui.desktop.DragPreview
import com.opendesktop.launcher.ui.desktop.DragSource
import com.opendesktop.launcher.ui.desktop.DropTarget
import com.opendesktop.launcher.ui.desktop.DropZone
import com.opendesktop.launcher.ui.desktop.FolderDialog
import com.opendesktop.launcher.ui.desktop.PageIndicator
import com.opendesktop.launcher.ui.desktop.SearchStyleDialog
import com.opendesktop.launcher.ui.desktop.GadgetStyleDialog
import com.opendesktop.launcher.ui.desktop.openGadgetTarget
import com.opendesktop.launcher.data.GadgetKind
import com.opendesktop.launcher.ui.desktop.WidgetPicker
import com.opendesktop.launcher.ui.desktop.pageTransition
import com.opendesktop.launcher.ui.drawer.AppDrawer
import com.opendesktop.launcher.ui.onboarding.OnboardingScreen
import com.opendesktop.launcher.ui.settings.SettingsScreen
import com.opendesktop.launcher.ui.settings.UpdateDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun DropZones(drag: DragController, target: DropTarget?, isApp: Boolean) {
    val source = drag.payload?.source
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val zones = buildList {
            add(DropZone.Remove)
            if (isApp) { add(DropZone.Info); add(DropZone.Uninstall) }
        }
        zones.forEach { z ->
            val active = (target as? DropTarget.Zone)?.zone == z
            val label = if (z == DropZone.Remove && source == DragSource.Drawer) "Ukryj" else z.label
            Box(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .onGloballyPositioned { drag.zoneBounds[z] = it.boundsInRoot() }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            active && z == DropZone.Uninstall -> Color(0xCCD32F2F)
                            active -> Color.White.copy(alpha = 0.35f)
                            else -> Color.Black.copy(alpha = 0.35f)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (z) {
                            DropZone.Remove -> if (source == DragSource.Drawer) Icons.Rounded.VisibilityOff else Icons.Rounded.DeleteOutline
                            DropZone.Info -> Icons.Rounded.Info
                            DropZone.Uninstall -> Icons.Rounded.DeleteForever
                        },
                        null, tint = Color.White, modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(label, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
internal fun EditTopBar(
    page: Int,
    count: Int,
    isHome: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onHome: () -> Unit,
    onDone: () -> Unit,
) {
    // Kompaktowe pigułki zamiast belki na całą szerokość: po lewej numer ekranu z przesuwaniem
    // jego pozycji, po prawej „Główny” i białe „Gotowe”
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PageMover(page, count, onMoveLeft, onMoveRight)
        Spacer(Modifier.weight(1f))
        TopBarPill(Icons.Rounded.Home, if (isHome) "Główny" else "Ustaw główny", highlighted = isHome, onClick = onHome)
        TopBarPill(Icons.Rounded.Check, "Gotowe", highlighted = true, onClick = onDone)
    }
}

/**
 * Przesuwanie bieżącego ekranu w kolejności: pigułka w stylu pozostałych przycisków paska,
 * okrągłe strzałki po bokach, a w środku numer i mapka ekranów – bieżący jako wydłużona
 * kreska, więc po przesunięciu widać, jak zmienia miejsce.
 */
@Composable
internal fun PageMover(page: Int, count: Int, onMoveLeft: () -> Unit, onMoveRight: () -> Unit) {
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TopBarIcon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Przesuń ekran w lewo", enabled = page > 0, onClick = onMoveLeft)
        Column(
            Modifier.padding(horizontal = 8.dp).widthIn(min = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Ekran ${page + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            if (count <= 8) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0 until count) {
                        val current = i == page
                        val w by animateDpAsState(if (current) 14.dp else 5.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "pageMark")
                        Box(
                            Modifier
                                .size(w, 5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = if (current) 1f else 0.3f)),
                        )
                    }
                }
            } else {
                Text("z $count", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        TopBarIcon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Przesuń ekran w prawo", enabled = page < count - 1, onClick = onMoveRight)
    }
}

@Composable
internal fun TopBarIcon(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    // Okrągły przycisk z lekkim tłem; po naciśnięciu jaśnieje i lekko się zmniejsza
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(stiffness = 900f), label = "arrowPress")
    Box(
        Modifier
            .size(34.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.3f }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (pressed) 0.3f else 0.14f))
            .then(
                if (enabled) Modifier.pointerInput(onClick) {
                    detectTapGestures(
                        onPress = { pressed = true; tryAwaitRelease(); pressed = false },
                        onTap = { onClick() },
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp)) }
}

@Composable
internal fun TopBarPill(icon: ImageVector, label: String, highlighted: Boolean, onClick: () -> Unit) {
    val fg = if (highlighted) Color.Black else Color.White
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(if (highlighted) Color.White else Color.Black.copy(alpha = 0.55f))
            .pointerInput(onClick) { detectTapGestures { onClick() } }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun EditToolbar(
    bottomInset: Dp,
    onWidgets: () -> Unit,
    onActions: () -> Unit,
    onWallpaper: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
) {
    // Arkusz wtopiony w dół ekranu: tło sięga pod pasek nawigacji, zaokrąglone górne rogi.
    // Przyciski dzielą szerokość po równo
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(sheet)
            .background(Brush.verticalGradient(listOf(Color(0xE61C1F26), Color(0xF20E1014))))
            .border(1.dp, Color.White.copy(alpha = 0.08f), sheet)
            .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 8.dp + bottomInset),
    ) {
        BigToolButton(Icons.Rounded.Widgets, "Widżety", onWidgets)
        BigToolButton(Icons.Rounded.TouchApp, "Akcje", onActions)
        BigToolButton(Icons.Rounded.Wallpaper, "Tapeta", onWallpaper)
        BigToolButton(Icons.Rounded.Add, "Nowy ekran", onAddPage)
        BigToolButton(Icons.Rounded.Delete, "Usuń ekran", onRemovePage)
        BigToolButton(Icons.Rounded.Lock, "Zablokuj", onLock)
        BigToolButton(Icons.Rounded.Settings, "Ustawienia", onSettings)
    }
}

@Composable
internal fun ToolButton(icon: ImageVector, label: String, enabled: Boolean = true, highlighted: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.3f))
            .then(if (enabled) Modifier.pointerInput(Unit) { detectTapGestures { onClick() } } else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, label, tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else Color.White, modifier = Modifier.size(20.dp))
        Text(label, color = if (highlighted) MaterialTheme.colorScheme.onPrimary else Color.White, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
internal fun RowScope.BigToolButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    // Wciśnięcie lekko zmniejsza przycisk i rozjaśnia kółko ikony
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.5f, stiffness = 900f), label = "press")
    Column(
        Modifier
            .weight(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.35f }
            .clip(RoundedCornerShape(16.dp))
            .then(if (enabled) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (pressed) 0.24f else 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** Krótka podpowiedź na dole pulpitu w trybie edycji; po kilku sekundach znika. */
@Composable
internal fun EditHint(visible: Boolean, hasSelection: Boolean, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(visible, hasSelection) {
        shown = visible
        if (visible) { delay(3500); shown = false }
    }
    AnimatedVisibility(shown, modifier = modifier, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        Text(
            if (hasSelection) "Przeciągnij kropki na krawędziach, aby zmienić rozmiar"
            else "Przytrzymaj i przeciągnij, aby przenieść · stuknij widżet, aby zmienić rozmiar",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

/**
 * Kłódka po przytrzymaniu przy zablokowanym układzie. „Odblokuj” otwiera kłódkę (krótka animacja)
 * i dopiero wtedy włącza tryb edycji. Stuknięcie obok zamyka bez zmian.
 */
@Composable
internal fun LockPromptOverlay(
    title: String?,
    hasMenu: Boolean,
    onDismiss: () -> Unit,
    onShowMenu: () -> Unit,
    onUnlock: () -> Unit,
) {
    var unlocking by remember { mutableStateOf(false) }
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    LaunchedEffect(unlocking) {
        if (unlocking) { delay(380); onUnlock() }
    }
    val enter by animateFloatAsState(if (appeared) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "lockIn")
    val bounce by animateFloatAsState(if (unlocking) 1.12f else 1f, spring(dampingRatio = 0.35f, stiffness = 600f), label = "lockBounce")
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = enter.coerceIn(0f, 1f) }
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) { detectTapGestures { if (!unlocking) onDismiss() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 32.dp)
                .graphicsLayer { val s = 0.85f + 0.15f * enter; scaleX = s; scaleY = s }
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xF21C1C1C))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(30.dp))
                // Dotknięcia karty nie zamykają nakładki
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .graphicsLayer { scaleX = bounce; scaleY = bounce }
                    .clip(CircleShape)
                    .background(if (unlocking) Color.White else Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    unlocking,
                    transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut() },
                    label = "lockIcon",
                ) { open ->
                    Icon(
                        if (open) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        null,
                        tint = if (open) Color.Black else Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(if (unlocking) "Odblokowano" else "Układ zablokowany", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (title != null) "Odblokuj, aby przenieść lub zmienić „$title”."
                else "Odblokuj, aby przenosić, zmieniać rozmiar i usuwać elementy pulpitu.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasMenu) TopBarPill(Icons.Rounded.MoreHoriz, "Menu", highlighted = false) { if (!unlocking) onShowMenu() }
                TopBarPill(Icons.Rounded.LockOpen, "Odblokuj", highlighted = true) {
                    if (!unlocking) unlocking = true
                }
            }
        }
    }
}
