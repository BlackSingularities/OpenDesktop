package com.opendesktop.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AppInfo(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
    /** Średni kolor ikony (ARGB) – tło kafelków. */
    val color: Int,
    val systemCategory: Int,
    val installTime: Long,
    val isSystem: Boolean,
    val isWork: Boolean,
)

data class ShortcutEntry(val label: String, val icon: ImageBitmap?, val info: ShortcutInfo)

class AppRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    initialShape: IconShape,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    private var shape = initialShape
    private var job: Job? = null

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = reload()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = reload()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = reload()
    }

    init {
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        reload()
    }

    fun setShape(s: IconShape) {
        if (s != shape) {
            shape = s
            reload()
        }
    }

    fun reload() {
        job?.cancel()
        job = scope.launch(Dispatchers.Default) {
            // Seria zdarzeń przy instalacji – przeładowujemy raz
            if (_loaded.value) delay(250)
            val density = context.resources.displayMetrics.density
            val sizePx = (72 * density).toInt().coerceIn(96, 216)
            val me = Process.myUserHandle()
            val result = ArrayList<AppInfo>()
            for (user in launcherApps.profiles) {
                val list = runCatching { launcherApps.getActivityList(null, user) }.getOrDefault(emptyList())
                for (a in list) {
                    runCatching { result += toInfo(a, user, user != me, sizePx) }
                }
            }
            _apps.value = result.distinctBy { it.key }.sortedBy { it.label.lowercase() }
            _loaded.value = true
        }
    }

    private fun toInfo(a: LauncherActivityInfo, user: UserHandle, work: Boolean, size: Int): AppInfo {
        val cn = a.componentName
        val key = cn.flattenToString() + if (work) "#" + user.hashCode() else ""
        val bmp = if (shape == IconShape.Transparent) {
            // Symbol bez tła; plakietkę profilu służbowego nakładamy dopiero na gotowy obrazek
            val bare = renderBare(a.getIcon(0), size)
            if (work) context.packageManager.getUserBadgedIcon(BitmapDrawable(context.resources, bare), user).toBitmap(size, size, Bitmap.Config.ARGB_8888) else bare
        } else render(a.getBadgedIcon(0), size)
        return AppInfo(
            key = key,
            label = a.label?.toString()?.trim().orEmpty().ifBlank { cn.packageName },
            packageName = cn.packageName,
            component = cn,
            user = user,
            icon = bmp.asImageBitmap(),
            color = averageColor(bmp),
            systemCategory = a.applicationInfo.category,
            installTime = a.firstInstallTime,
            // Zaktualizowane aplikacje systemowe da się odinstalować (do wersji fabrycznej) zwykłym dialogiem
            isSystem = (a.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (a.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0,
            isWork = work,
        )
    }

    /**
     * Warstwy ikony adaptacyjnej rysujemy sami. Systemowe AdaptiveIconDrawable.draw() zalewa warstwę
     * na czarno przed rysowaniem, więc ikony z przezroczystym tłem wychodziły na czarnym kwadracie.
     * Dla kształtu systemowego przycinamy do maski urządzenia, dla własnych kształtów maski nie ma.
     */
    private fun render(d: Drawable, size: Int): Bitmap {
        if (d is AdaptiveIconDrawable) {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            if (shape == IconShape.System) {
                d.setBounds(0, 0, size, size)
                c.clipPath(d.iconMask)
            }
            val inset = size / 4
            d.background?.let { it.setBounds(-inset, -inset, size + inset, size + inset); it.draw(c) }
            d.foreground?.let { it.setBounds(-inset, -inset, size + inset, size + inset); it.draw(c) }
            return bmp
        }
        return d.toBitmap(size, size, Bitmap.Config.ARGB_8888)
    }

    /**
     * Ikona bez tła: sama warstwa pierwszego planu ikony adaptacyjnej. Warstwa jest rysowana w całości
     * (bez maski, więc nic nie jest ucinane), a potem kadrowana do faktycznej treści i wpasowana
     * w kwadrat z małym marginesem – symbole różnych aplikacji mają podobną wielkość.
     * Gdy tło jest „wtopione” w rysunek (ikony starego typu, pierwszy plan z pełną planszą, ikony
     * podmienione przez motyw producenta, bitmapy bez kanału alfa – czarne pole), jednolitą planszę
     * stykającą się z krawędzią usuwamy wypełnieniem od brzegów.
     */
    private fun renderBare(d: Drawable, size: Int): Bitmap {
        val adaptive = d as? AdaptiveIconDrawable
        val layer: Bitmap
        if (adaptive != null) {
            // Warstwa ma 108 dp, z czego widoczne w masce jest środkowe 72 dp – rysujemy całe 108
            val full = (size * 1.5f).toInt()
            layer = Bitmap.createBitmap(full, full, Bitmap.Config.ARGB_8888)
            adaptive.foreground?.let { it.setBounds(0, 0, full, full); it.draw(Canvas(layer)) }
            val fgBox = contentBounds(layer)
            // Prawie pusta warstwa (cały rysunek siedzi w tle) – składamy obie warstwy i zdejmujemy planszę
            if (fgBox == null || fgBox.width() * fgBox.height() < full * full / 40) {
                val c = Canvas(layer)
                c.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)
                adaptive.background?.let { it.setBounds(0, 0, full, full); it.draw(c) }
                adaptive.foreground?.let { it.setBounds(0, 0, full, full); it.draw(c) }
            }
        } else {
            layer = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, size, size)
            d.draw(Canvas(layer))
        }
        stripPlate(layer)
        val full = layer.width
        val box = contentBounds(layer)
        if (box == null || box.width() * box.height() < full * full / 60) {
            layer.recycle()
            return render(d, size)
        }
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val margin = size * 0.04f
        val avail = size - margin * 2
        val scale = avail / maxOf(box.width(), box.height())
        val w = box.width() * scale
        val h = box.height() * scale
        val dst = android.graphics.RectF((size - w) / 2f, (size - h) / 2f, (size + w) / 2f, (size + h) / 2f)
        Canvas(out).drawBitmap(layer, box, dst, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG))
        layer.recycle()
        return out
    }

    /**
     * Usuwa jednolitą planszę pod symbolem: jeśli krawędź rysunku jest w większości kryjąca i w jednym
     * kolorze, zdejmujemy ten kolor wypełnieniem od brzegów (środek symbolu w tym samym kolorze zostaje).
     * Plansze gradientowe, zdjęcia i symbole dotykające krawędzi tylko punktowo zostają nietknięte.
     */
    private fun stripPlate(bmp: Bitmap) {
        val box = contentBounds(bmp) ?: return
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        // Obwód prostokąta treści
        val edge = ArrayList<Int>()
        for (x in box.left until box.right) { edge += box.top * w + x; edge += (box.bottom - 1) * w + x }
        for (y in box.top + 1 until box.bottom - 1) { edge += y * w + box.left; edge += y * w + box.right - 1 }
        val opaque = edge.filter { (px[it] ushr 24) >= 200 }
        if (opaque.size < edge.size * 0.4f) return
        // Dominujący kolor obwodu (kubełki po 16 poziomów na kanał)
        val buckets = HashMap<Int, Int>()
        for (i in opaque) { val p = px[i]; val k = ((p shr 20) and 0xF shl 8) or ((p shr 12) and 0xF shl 4) or ((p shr 4) and 0xF); buckets[k] = (buckets[k] ?: 0) + 1 }
        val top = buckets.maxByOrNull { it.value } ?: return
        var r = 0L; var g = 0L; var b = 0L; var n = 0
        for (i in opaque) {
            val p = px[i]
            val k = ((p shr 20) and 0xF shl 8) or ((p shr 12) and 0xF shl 4) or ((p shr 4) and 0xF)
            if (k == top.key) { r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++ }
        }
        val rr = (r / n).toInt(); val gg = (g / n).toInt(); val bb = (b / n).toInt()
        val tol = 48
        fun dist(p: Int) = abs(((p shr 16) and 0xFF) - rr) + abs(((p shr 8) and 0xFF) - gg) + abs((p and 0xFF) - bb)
        if (opaque.count { dist(px[it]) <= tol } < opaque.size * 0.85f) return
        // Wypełnienie od brzegów po pikselach w kolorze planszy (także półprzezroczystych krawędziach planszy)
        val removed = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0; var tail = 0
        for (i in edge) if ((px[i] ushr 24) > 16 && dist(px[i]) <= tol && !removed[i]) { removed[i] = true; queue[tail++] = i }
        while (head < tail) {
            val i = queue[head++]
            val x = i % w; val y = i / w
            for (j in intArrayOf(if (x > 0) i - 1 else -1, if (x < w - 1) i + 1 else -1, if (y > 0) i - w else -1, if (y < h - 1) i + w else -1)) {
                if (j < 0 || removed[j]) continue
                val p = px[j]
                if ((p ushr 24) > 16 && dist(p) <= tol) { removed[j] = true; queue[tail++] = j }
            }
        }
        // Po zdjęciu planszy musi zostać wyraźny symbol – inaczej zostawiamy ikonę jak była
        var left = 0
        for (i in px.indices) if (!removed[i] && (px[i] ushr 24) > 16) left++
        if (left < box.width() * box.height() / 40) return
        for (i in px.indices) {
            if (removed[i]) { px[i] = 0; continue }
            val p = px[i]
            if ((p ushr 24) == 0) continue
            // Wygładzenie styku symbolu z planszą: piksele pośrednie przy usuniętym obszarze półprzezroczyste
            val x = i % w; val y = i / w
            val nearRemoved = (x > 0 && removed[i - 1]) || (x < w - 1 && removed[i + 1]) || (y > 0 && removed[i - w]) || (y < h - 1 && removed[i + w])
            if (!nearRemoved) continue
            val dd = dist(p)
            if (dd < tol * 3) {
                val k = ((dd - tol).toFloat() / (tol * 2)).coerceIn(0f, 1f)
                px[i] = ((((p ushr 24) * k).toInt()) shl 24) or (p and 0xFFFFFF)
            }
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
    }

    /** Najmniejszy prostokąt z widocznymi pikselami (alfa > 16); null, gdy obraz jest pusty. */
    private fun contentBounds(bmp: Bitmap): Rect? {
        val w = bmp.width
        val h = bmp.height
        val row = IntArray(w)
        var top = -1; var bottom = -1; var left = w; var right = -1
        for (y in 0 until h) {
            bmp.getPixels(row, 0, w, 0, y, w, 1)
            var any = false
            for (x in 0 until w) {
                if ((row[x] ushr 24) > 16) {
                    any = true
                    if (x < left) left = x
                    if (x > right) right = x
                }
            }
            if (any) {
                if (top < 0) top = y
                bottom = y
            }
        }
        if (top < 0 || right < left) return null
        return Rect(left, top, right + 1, bottom + 1)
    }

    private fun averageColor(bmp: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(bmp, 8, 8, true)
        var r = 0L; var g = 0L; var b = 0L; var n = 0L
        for (x in 0 until 8) for (y in 0 until 8) {
            val p = small.getPixel(x, y)
            val alpha = p ushr 24
            if (alpha < 128) continue
            r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
        }
        if (n == 0L) return 0xFF445066.toInt()
        return (0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
    }

    fun launch(app: AppInfo, bounds: Rect?, options: Bundle?) {
        runCatching { launcherApps.startMainActivity(app.component, app.user, bounds, options) }
            .onFailure {
                runCatching {
                    context.packageManager.getLaunchIntentForPackage(app.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ?.let(context::startActivity)
                }
            }
    }

    fun openDetails(app: AppInfo) {
        runCatching { launcherApps.startAppDetailsActivity(app.component, app.user, null, null) }
    }

    fun uninstall(app: AppInfo) {
        // Czystej aplikacji systemowej nie da się odinstalować – w informacjach o aplikacji można ją wyłączyć
        if (app.isSystem) return openDetails(app)
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                    .putExtra(Intent.EXTRA_USER, app.user)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Skróty aplikacji – działają, gdy OpenDesktop jest domyślnym launcherem. */
    fun shortcuts(app: AppInfo): List<ShortcutEntry> = runCatching {
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()
        val q = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setActivity(app.component)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        val density = context.resources.displayMetrics.densityDpi
        launcherApps.getShortcuts(q, app.user).orEmpty()
            .filter { it.isEnabled }
            .sortedBy { it.rank }
            .take(5)
            .map { s ->
                val icon = runCatching {
                    launcherApps.getShortcutIconDrawable(s, density)?.toBitmap(96, 96)?.asImageBitmap()
                }.getOrNull()
                ShortcutEntry((s.shortLabel ?: s.longLabel ?: "").toString(), icon, s)
            }
    }.getOrDefault(emptyList())

    /** Skróty wszystkich aplikacji (bez ikon) – do wyszukiwarki. Działa, gdy OpenDesktop jest domyślny. */
    fun allShortcuts(): List<ShortcutInfo> = runCatching {
        if (!launcherApps.hasShortcutHostPermission()) return emptyList()
        val q = LauncherApps.ShortcutQuery().setQueryFlags(
            LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
        )
        launcherApps.profiles.flatMap { user -> runCatching { launcherApps.getShortcuts(q, user).orEmpty() }.getOrDefault(emptyList()) }
            .filter { it.isEnabled }
            .distinctBy { it.`package` + "/" + it.id }
    }.getOrDefault(emptyList())

    fun shortcutEntry(s: ShortcutInfo): ShortcutEntry {
        val density = context.resources.displayMetrics.densityDpi
        val icon = runCatching { launcherApps.getShortcutIconDrawable(s, density)?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
        return ShortcutEntry((s.shortLabel ?: s.longLabel ?: "").toString(), icon, s)
    }

    fun startShortcut(s: ShortcutInfo) {
        runCatching { launcherApps.startShortcut(s, null, null) }
    }
}
