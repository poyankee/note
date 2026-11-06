package com.poyankee.note

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.poyankee.note.core.Background
import com.poyankee.note.core.Geom
import com.poyankee.note.core.InkItem
import com.poyankee.note.core.Item
import com.poyankee.note.core.PaperColors
import com.poyankee.note.core.Template
import com.poyankee.note.core.ShapeItem
import com.poyankee.note.editor.EditorController
import com.poyankee.note.editor.Exporter
import com.poyankee.note.editor.InkView
import com.poyankee.note.editor.InstrumentKind
import com.poyankee.note.editor.LessonRecorder
import com.poyankee.note.editor.Tool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

/**
 * Emülatörde uçtan uca duman testi: uygulama açılır, kalemle çizilir, araçlar denenir, PDF açılır ve dışa aktarılır.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val inst get() = InstrumentationRegistry.getInstrumentation()

    private fun <T> main(block: () -> T): T {
        var r: T? = null
        inst.runOnMainSync { r = block() }
        @Suppress("UNCHECKED_CAST")
        return r as T
    }

    private fun findInk(v: View): InkView? {
        if (v is InkView) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findInk(v.getChildAt(i))?.let { return it }
        return null
    }

    private fun ink(): InkView {
        var found: InkView? = null
        rule.waitUntil(15_000) {
            found = main { findInk(rule.activity.window.decorView) }
            found != null && main { found!!.width } > 0
        }
        return found!!
    }

    private fun items(c: EditorController, page: Int = 0): List<Item> = main { c.nb.pages[page].items.toList() }

    private var downTime = 0L

    private fun event(action: Int, pts: List<Pair<Float, Float>>, tool: Int, actionIndex: Int = 0): MotionEvent {
        val props = Array(pts.size) { i -> MotionEvent.PointerProperties().apply { id = i; toolType = tool } }
        val coords = Array(pts.size) { i -> MotionEvent.PointerCoords().apply { x = pts[i].first; y = pts[i].second; pressure = 0.7f; size = 0.01f } }
        val act = if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP)
            action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT) else action
        val source = if (tool == MotionEvent.TOOL_TYPE_FINGER) InputDevice.SOURCE_TOUCHSCREEN else InputDevice.SOURCE_STYLUS
        return MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), act, pts.size, props, coords, 0, 0, 1f, 1f, 0, 0, source, 0)
    }

    private fun send(v: View, e: MotionEvent) { main { v.dispatchTouchEvent(e) }; e.recycle() }

    /** Kalemle (x0,y0)'dan (x1,y1)'e çizgi çizer. [holdMs] > 0 ise sonda kalemi bekletir. */
    private fun penLine(v: View, x0: Float, y0: Float, x1: Float, y1: Float, steps: Int = 20, holdMs: Long = 0, tool: Int = MotionEvent.TOOL_TYPE_STYLUS) {
        downTime = SystemClock.uptimeMillis()
        send(v, event(MotionEvent.ACTION_DOWN, listOf(x0 to y0), tool))
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            send(v, event(MotionEvent.ACTION_MOVE, listOf((x0 + (x1 - x0) * t) to (y0 + (y1 - y0) * t)), tool))
            Thread.sleep(8)
        }
        if (holdMs > 0) Thread.sleep(holdMs)
        send(v, event(MotionEvent.ACTION_UP, listOf(x1 to y1), tool))
        Thread.sleep(50)
    }

    private fun penPath(v: View, pts: List<Pair<Float, Float>>, holdMs: Long = 0) {
        downTime = SystemClock.uptimeMillis()
        send(v, event(MotionEvent.ACTION_DOWN, listOf(pts[0]), MotionEvent.TOOL_TYPE_STYLUS))
        for (p in pts.drop(1)) { send(v, event(MotionEvent.ACTION_MOVE, listOf(p), MotionEvent.TOOL_TYPE_STYLUS)); Thread.sleep(6) }
        if (holdMs > 0) Thread.sleep(holdMs)
        send(v, event(MotionEvent.ACTION_UP, listOf(pts.last()), MotionEvent.TOOL_TYPE_STYLUS))
        Thread.sleep(50)
    }

    private fun tap(v: View, x: Float, y: Float) = penLine(v, x, y, x, y, steps = 1)

    private fun twoFingerTap(v: View, x: Float, y: Float) {
        Thread.sleep(400) // kalem yeni kalktıysa parmaklar yok sayılır
        downTime = SystemClock.uptimeMillis()
        val f = MotionEvent.TOOL_TYPE_FINGER
        val two = listOf(x to y, (x + 150f) to y)
        send(v, event(MotionEvent.ACTION_DOWN, listOf(two[0]), f))
        send(v, event(MotionEvent.ACTION_POINTER_DOWN, two, f, 1))
        Thread.sleep(60)
        send(v, event(MotionEvent.ACTION_POINTER_UP, two, f, 1))
        send(v, event(MotionEvent.ACTION_UP, listOf(two[0]), f))
        Thread.sleep(100)
    }

    /** Gerçek ekran görüntüsü (diyaloglar ve menüler dahil). CI bunları "screenshots" dalına yükler. */
    private fun shot(name: String) {
        rule.waitForIdle()
        Thread.sleep(400)
        fun sh(cmd: String) {
            val pfd = inst.uiAutomation.executeShellCommand(cmd)
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        }
        sh("mkdir -p /data/local/tmp/shots")
        sh("screencap -p /data/local/tmp/shots/$name.png")
    }

    private fun newNotebook(): InkView {
        shot("01-ana-sayfa")
        // testlerde silgi sildikten sonra kaleme dönmesin (adımlar silgiyle devam ediyor)
        main { com.poyankee.note.data.Settings.get(rule.activity).eraserReturn.value = false; com.poyankee.note.data.Settings.get(rule.activity).autoHideBar.value = false }
        rule.onNodeWithTag("add").performClick()
        rule.onNodeWithText("Yeni defter").performClick()
        return ink()
    }

    /** Bir alanın ne kadarının saf beyaz olduğu (0..1). */
    private fun whiteRatio(tag: String): Float {
        rule.waitForIdle()
        val bmp = rule.onRoot().captureToImage().asAndroidBitmap()
        val b = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        var white = 0; var total = 0
        var y = b.top.toInt() + 2
        while (y < b.bottom.toInt() - 2 && y < bmp.height) {
            var x = b.left.toInt() + 2
            while (x < b.right.toInt() - 2 && x < bmp.width) {
                if (bmp.getPixel(x, y) == android.graphics.Color.WHITE) white++
                total++
                x += 7
            }
            y += 5
        }
        return if (total == 0) 0f else white.toFloat() / total
    }

    @Test
    fun drawingAndTools() {
        val v = newNotebook()
        val c = v.controller
        val w = v.width.toFloat(); val h = v.height.toFloat()

        // 0) Arayüz görünür olmalı: sayfa çizimi üst çubuğun ve araç menüsünün üzerine taşmamalı
        val topWhite = whiteRatio("topbar")
        assertTrue("üst çubuk görünmüyor (beyaz oranı $topWhite)", topWhite > 0.5f)
        assertTrue("araç seçenekleri şeridi görünmüyor", whiteRatio("options") > 0.3f)

        // 0b) Sayfa stili: desen, renk, aralık, saydamlık
        main { c.setPageStyle(0, Background.Paper(Template.ISOMETRIC, PaperColors.CREAM, 14f, 0.3f)) }
        val st = main { c.nb.pages[0].background as Background.Paper }
        assertEquals(Template.ISOMETRIC, st.template); assertEquals(0.3f, st.opacity, 0f)
        main { c.setPageStyle(0, Background.Paper(Template.GRID)) }
        shot("03-bos-defter")
        rule.onAllNodesWithText("Sayfa")[0].performClick()
        shot("04-sayfa-paneli")
        main { c.setPageStyle(0, Background.Paper(Template.ISOMETRIC, PaperColors.CREAM, 20f, 0.4f)) }
        shot("05-izometrik-krem")
        rule.onAllNodesWithText("Sayfa")[0].performClick()
        main { c.setPageStyle(0, Background.Paper(Template.GRID)) }

        // 1) Kalemle yaz
        main { c.selectTool(Tool.PEN) }
        penLine(v, w * 0.2f, h * 0.2f, w * 0.5f, h * 0.25f)
        assertEquals(1, items(c).size)
        assertTrue(items(c)[0] is InkItem)

        // 2) Parmak çizmez (sadece kalem modu)
        Thread.sleep(400)
        penLine(v, w * 0.2f, h * 0.5f, w * 0.4f, h * 0.55f, tool = MotionEvent.TOOL_TYPE_FINGER)
        assertEquals(1, items(c).size)

        // 3) Çizip bekle → düz çizgi şekli
        penLine(v, w * 0.2f, h * 0.35f, w * 0.7f, h * 0.36f, holdMs = 1200)
        val shape = items(c).last()
        assertTrue("şekil bekleniyordu: $shape", shape is ShapeItem && (shape.geom as? Geom.Polyline)?.n == 2)

        // 4) İki parmak dokunuşu = geri al
        twoFingerTap(v, w * 0.3f, h * 0.7f)
        assertEquals(1, items(c).size)

        // 5) Silgi
        main { c.selectTool(Tool.ERASER) }
        penLine(v, w * 0.35f, h * 0.1f, w * 0.35f, h * 0.3f, steps = 30)
        assertEquals(0, items(c).size)
        main { c.undo() }
        assertEquals(1, items(c).size)

        // 6) Kısmi silgi ikiye böler
        main { c.eraserPartial = true }
        penLine(v, w * 0.35f, h * 0.1f, w * 0.35f, h * 0.3f, steps = 30)
        assertEquals(2, items(c).size)
        main { c.undo(); c.eraserPartial = false }

        // 7) Şekil kalemi: daire
        main { c.selectTool(Tool.SHAPE) }
        val cx = w * 0.5f; val cy = h * 0.6f; val r = w * 0.12f
        penPath(v, (0..64).map { i -> val a = i / 60.0 * 2 * Math.PI; (cx + r * Math.cos(a)).toFloat() to (cy + r * Math.sin(a)).toFloat() })
        val circle = items(c).last()
        assertTrue("çember bekleniyordu: ${(circle as? ShapeItem)?.geom}", circle is ShapeItem && circle.geom is Geom.Circle)

        // 8) Doğru parçası, ok, çokgen
        val before = items(c).size
        main { c.selectTool(Tool.LINE) }
        penLine(v, w * 0.1f, h * 0.8f, w * 0.4f, h * 0.8f)
        main { c.selectTool(Tool.ARROW) }
        penLine(v, w * 0.1f, h * 0.85f, w * 0.4f, h * 0.88f)
        main { c.selectTool(Tool.POLYGON) }
        penLine(v, w * 0.75f, h * 0.8f, w * 0.85f, h * 0.8f)
        assertEquals(before + 1 + 2 + 1, items(c).size)

        // 9) Pergel: yarıçap, sonra yay
        main { c.selectTool(Tool.COMPASS) }
        penLine(v, w * 0.5f, h * 0.3f, w * 0.6f, h * 0.3f)
        val arc = (0..30).map { i -> val a = i / 30.0 * Math.PI; (w * 0.5f + w * 0.1f * Math.cos(a)).toFloat() to (h * 0.3f + w * 0.1f * Math.sin(a)).toFloat() }
        penPath(v, arc)
        val g = (items(c).last() as ShapeItem).geom
        assertTrue("yay bekleniyordu: $g", g is Geom.Arc)

        // 10) Lazer ve geçici kalem kalıcı öğe eklemez
        val n = items(c).size
        main { c.selectTool(Tool.LASER) }
        penLine(v, w * 0.2f, h * 0.2f, w * 0.6f, h * 0.6f)
        main { c.selectTool(Tool.TEMP) }
        penLine(v, w * 0.2f, h * 0.6f, w * 0.6f, h * 0.2f)
        assertEquals(n, items(c).size)

        // 11) Kement seçim ve taşıma
        main { c.selectTool(Tool.LASSO) }
        penPath(v, listOf(w * 0.05f to h * 0.05f, w * 0.95f to h * 0.05f, w * 0.95f to h * 0.97f, w * 0.05f to h * 0.97f, w * 0.05f to h * 0.06f))
        assertTrue(main { c.hasSelection })
        main { v.selectionDuplicate() }
        assertTrue(items(c).size > n)
        main { v.clearSelection() }

        // 12) Cetvel kenarına yapışan çizgi
        main { v.toggleInstrument(InstrumentKind.RULER); c.selectTool(Tool.PEN) }
        // cetvel ekranın ortasında; üst kenarı (merkez - 29pt)
        val z = v.zoom
        val edgeY = h / 2f - 29f * z
        val k = items(c).size
        penLine(v, w / 2f - 100f, edgeY - 3f, w / 2f + 100f, edgeY + 2f)
        val ruled = items(c).last() as InkItem
        val ys = (0 until ruled.count).map { ruled.pts[3 * it + 1] }
        assertEquals(k + 1, items(c).size)
        assertTrue("cetvel boyunca düz olmalı", (ys.maxOrNull()!! - ys.minOrNull()!!) < 0.5f)
        main { v.toggleInstrument(InstrumentKind.RULER) }

        // 13) Metin
        main { c.selectTool(Tool.TEXT) }
        tap(v, w * 0.3f, h * 0.15f)
        val req = main { c.textRequest }
        assertTrue(req != null)
        main { c.finishText(req!!, "x² + 2x + 1 = 0  ğüşıöç", 18f) }

        shot("06-cizimler")
        main { c.selectTool(Tool.LINE) }
        shot("07-geometri-araclari")
        main { c.selectTool(Tool.ERASER) }
        shot("08-silgi")
        main { c.selectTool(Tool.PEN) }

        // 14) Sayfa ekle, git, geri dön
        main { c.insertPageAfter(0) }
        assertEquals(2, main { c.nb.pages.size })
        main { c.goToPage(0) }
        assertEquals(0, main { c.currentPage })

        // 15) PDF dışa aktar
        val out = File(rule.activity.cacheDir, "test-export.pdf")
        val pages = main { c.nb.pages.toList() }
        Exporter.export(rule.activity, c.nb, c.store.dir(c.nb.id), pages, out, { null }) { _, _ -> }
        assertTrue(out.length() > 1000)
        ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { assertEquals(2, it.pageCount) } }

        // 16) Ders kaydı (sessiz)
        val rec = main { LessonRecorder(rule.activity, v) }
        main { rec.start(false) }
        penLine(v, w * 0.2f, h * 0.4f, w * 0.6f, h * 0.45f)
        Thread.sleep(1500)
        val name = main { rec.stop() }
        assertTrue("video kaydedilemedi", name != null)

        // 17) Sunum modu
        main { c.presentation = true }
        Thread.sleep(500)
        main { c.presentation = false }
    }

    @Test
    fun pdfImportGotoAndExport() {
        // 3 sayfalık örnek PDF
        val ctx = rule.activity
        val src = File(ctx.cacheDir, "ornek.pdf")
        val doc = PdfDocument()
        for (i in 1..3) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i).create())
            page.canvas.drawText("Sayfa $i", 100f, 100f, Paint().apply { textSize = 30f })
            doc.finishPage(page)
        }
        FileOutputStream(src).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", src)
        main {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
                    .setClass(ctx, MainActivity::class.java)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            )
        }
        var v: InkView? = null
        rule.waitUntil(20_000) {
            v = main { findInk(rule.activity.window.decorView) }
            v != null && main { v!!.width > 0 && v!!.controller.nb.pages.size == 3 }
        }
        val ink = v!!
        val c = ink.controller
        assertTrue(main { c.nb.pages.all { it.background is Background.PdfPage } })
        assertTrue(main { c.pdf != null })

        // kitap sayfa numarası: 2. sayfa kitabın 1. sayfası olsun
        main { c.goToPage(1) }
        Thread.sleep(300)
        assertTrue(main { c.setBookNumberForCurrent(1) })
        assertEquals(1, main { c.nb.pageOffset })
        assertTrue(main { c.goToBookPage(2) })
        Thread.sleep(300)
        assertEquals(2, main { c.currentPage })
        main { c.goBack() }

        // PDF üzerine yaz
        val w = ink.width.toFloat(); val h = ink.height.toFloat()
        main { c.selectTool(Tool.PEN) }
        penLine(ink, w * 0.2f, h * 0.3f, w * 0.6f, h * 0.35f)
        // araya boş sayfa ekle
        main { c.insertPageAfter(0) }
        assertEquals(4, main { c.nb.pages.size })

        // dışa aktar: 4 sayfa (3 PDF + 1 boş), yazılar dahil
        val out = File(ctx.cacheDir, "test-pdf-export.pdf")
        val pages = main { c.nb.pages.toList() }
        Exporter.export(ctx, c.nb, c.store.dir(c.nb.id), pages, out, { null }) { _, _ -> }
        ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { assertEquals(4, it.pageCount) } }

        shot("09-pdf")
        main { ink.controller.presentation = true }
        shot("10-sunum")
        main { ink.controller.presentation = false }
        // ikinci ekran (CI'da sanal ekran açılırsa)
        Thread.sleep(1500)
        android.util.Log.i("SmokeTest", "secondScreenActive=" + main { c.secondScreenActive })
    }

    @Test
    fun newFeatures() {
        val v = newNotebook()
        val c = v.controller
        val w = v.width.toFloat(); val h = v.height.toFloat()

        // yeni sayfa düz beyaz
        main { c.insertPageAfter(0) }
        val np = main { c.nb.pages[1].background as Background.Paper }
        assertEquals(Template.BLANK, np.template)
        // sayfa silme geri alınabilir
        main { c.deletePage(1) }
        assertEquals(1, main { c.nb.pages.size })
        main { c.undo() }
        assertEquals(2, main { c.nb.pages.size })
        main { c.goToPage(0, false) }

        // koordinat deseni + fonksiyon grafiği
        main { c.setPageStyle(0, Background.Paper(Template.COORDINATE)) }
        val err = main { c.plotFunctions(listOf("x^2-4" to 0xFF1565C0.toInt(), "2x+1" to 0xFFD32F2F.toInt())) }
        assertEquals(null, err)
        assertTrue(main { c.nb.pages[0].items.count { it is ShapeItem } } >= 2)
        val bad = main { c.plotFunctions(listOf("x^^2" to 0)) }
        assertTrue(bad != null)
        shot("11-grafik")

        // formül
        val fr = com.poyankee.note.editor.FormulaRequest(0, null)
        main { c.finishFormula(fr, "x = \\frac{-b \\pm \\sqrt{b^{2}-4ac}}{2a}", 22f) }
        rule.waitUntil(5000) { main { c.nb.pages[0].items.any { it is com.poyankee.note.core.ImageItem && it.source.isNotEmpty() } } }
        main { v.clearSelection(); c.selectTool(Tool.PEN) }

        // damga ve yapışkan not
        main { c.insertStamp("Dikkat!", 0xFFB91C1C.toInt(), 0xFFFEE2E2.toInt()) }
        main { v.clearSelection() }
        main { c.requestStickyNote() }
        val req = main { c.textRequest }!!
        main { c.finishText(req, "Sınavda çıkar:\nkök formülü", 14f) }
        assertTrue(main { c.nb.pages[0].items.any { it is com.poyankee.note.core.TextItem && it.bg != 0 } })

        // 3B şekil
        main { c.selectTool(Tool.SOLID); c.solidKind = com.poyankee.note.core.Solids.Kind.CYLINDER }
        val before = items(c).size
        penLine(v, w * 0.6f, h * 0.55f, w * 0.8f, h * 0.8f)
        assertTrue(items(c).size >= before + 4)
        assertTrue(items(c).any { it is ShapeItem && it.dashed })

        // kopyala / yapıştır
        main { c.selectTool(Tool.LASSO) }
        penPath(v, listOf(w * 0.55f to h * 0.5f, w * 0.9f to h * 0.5f, w * 0.9f to h * 0.85f, w * 0.55f to h * 0.85f, w * 0.55f to h * 0.51f))
        assertTrue(main { c.hasSelection })
        main { v.selectionCopy() }
        val n0 = items(c).size
        main { v.clearSelection(); c.paste() }
        assertTrue(items(c).size > n0)
        main { v.clearSelection() }

        // şekil köşesi sürükleme: üçgen çiz, seç, köşeyi taşı
        main { c.selectTool(Tool.POLYGON); c.polygonSides = 3 }
        penLine(v, w * 0.25f, h * 0.6f, w * 0.25f, h * 0.5f)
        val tri = items(c).last() as ShapeItem
        main { c.selectTool(Tool.LASSO); v.select(0, listOf(tri)) }
        shot("12-sekil-duzenleme")
        main { v.clearSelection() }

        // ders araçları
        main { c.curtain = true; c.startTimer(90); c.selectTool(Tool.SPOT) }
        penLine(v, w * 0.4f, h * 0.3f, w * 0.45f, h * 0.32f)
        shot("13-perde-spot-sayac")
        main { c.curtain = false; c.stopTimer(); c.selectTool(Tool.PEN) }

        // sayfa döndürme (geri alınabilir)
        val pw = main { c.nb.pages[0].width }
        main { c.rotatePage(0) }
        assertEquals(pw, main { c.nb.pages[0].height }, 0.01f)
        main { c.undo() }
        assertEquals(pw, main { c.nb.pages[0].width }, 0.01f)

        // yedekle ve geri yükle
        val zip = File(rule.activity.cacheDir, "yedek.zip")
        FileOutputStream(zip).use { c.store.backupTo(it) }
        assertTrue(zip.length() > 500)
        val restored = java.io.FileInputStream(zip).use { c.store.restoreFrom(it) }
        assertTrue(restored >= 1)

        // dışa aktar (dolgu, not, formül, grafik dahil)
        val out = File(rule.activity.cacheDir, "yeni.pdf")
        val pages = main { c.nb.pages.toList() }
        Exporter.export(rule.activity, c.nb, c.store.dir(c.nb.id), pages, out, { c.loadImageBlocking(it) }) { _, _ -> }
        assertTrue(out.length() > 1000)

        // koyu tema görünümü
        main { com.poyankee.note.data.Settings.get(rule.activity).theme.value = "DARK" }
        shot("14-koyu-tema")
        main { com.poyankee.note.data.Settings.get(rule.activity).theme.value = "SYSTEM" }
        // menü daraltma
        main { com.poyankee.note.data.Settings.get(rule.activity).dockCollapsed.value = true }
        shot("15-menu-daraltilmis")
        main { com.poyankee.note.data.Settings.get(rule.activity).dockCollapsed.value = false }

        // yazarken otomatik yeni sayfa (son sayfanın altına yazınca)
        val pagesBefore = main { c.nb.pages.size }
        main { c.selectTool(Tool.PEN); v.scrollToFraction(1f) }
        Thread.sleep(300)
        val lastTop = main { v.pageScreenRect(pagesBefore - 1) }
        penLine(v, lastTop.left + 60f, lastTop.bottom - 20f, lastTop.left + 260f, lastTop.bottom - 25f)
        assertEquals(pagesBefore + 1, main { c.nb.pages.size })
        main { c.undo(); c.undo() }
        assertEquals(pagesBefore, main { c.nb.pages.size })
        // aşağı kaydırınca üst çubuk gizlenir
        main { c.topBarHidden = true }
        rule.waitForIdle()
        assertTrue(rule.onAllNodesWithTag("topbar").fetchSemanticsNodes().isEmpty())
        main { c.topBarHidden = false }
        rule.waitForIdle()
        // kaydırınca sağda sayfa çubuğu
        main { c.scrollTick++ }
        shot("16b-sayfa-cubugu")

        // sayfa boyutu: tam ekran oranı, geri alınabilir
        val (fw, fh) = main { c.fullScreenSize() }
        main { c.setPageSize(0, fw, fh, false) }
        assertEquals(fh, main { c.nb.pages[0].height }, 0.5f)
        main { c.undo() }
        assertEquals(842f, main { c.nb.pages[0].height }, 0.5f)
        main { c.setPageSize(0, fw, fh, false) }
        main { c.setPageStyle(0, Background.Paper(Template.GRID, 0xFFE8F4FF.toInt())) }

        // seçili araca tekrar dokununca seçenekler açılır
        main { c.selectTool(Tool.PEN) }
        rule.onNodeWithTag("tool-PEN").performClick()
        shot("16-arac-secenekleri")
        rule.onNodeWithTag("tool-PEN").performClick()
        rule.waitForIdle()

        // odak modu: sağ kenarda dikey
        val st = com.poyankee.note.data.Settings.get(rule.activity)
        main { st.presX.value = 5000f; st.presY.value = -500f; c.presentation = true }
        rule.waitForIdle(); Thread.sleep(500)
        // ekran dışına konmuş panel içeri çekilmiş olmalı
        assertTrue("odak paneli ekrana çekilmedi", st.presX.value < 2000f)
        shot("17-odak-dikey")
        main { c.presentation = false; st.presX.value = 0f; st.presY.value = 0f }
    }

    @Test
    fun libraryViews() {
        val st = com.poyankee.note.data.Settings.get(rule.activity)
        newNotebook()
        main { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(5000) { rule.onAllNodesWithTag("add").fetchSemanticsNodes().isNotEmpty() }
        main { st.libraryView.value = "LIST" }
        shot("18-liste-gorunumu")
        rule.onNodeWithText("Seç").performClick()
        rule.onNodeWithText("Tümünü seç").performClick()
        shot("19-toplu-secim")
        main { st.libraryView.value = "SMALL" }
        shot("20-kucuk-kapak")
        main { st.libraryView.value = "MEDIUM" }
        rule.onNodeWithText("Hiçbiri").performClick()
        rule.onNodeWithContentDescription("Seçimi bitir").performClick()

        // yapılacaklar
        val todos = com.poyankee.note.data.TodoList.get(rule.activity)
        main { todos.add("Ödevleri kontrol et"); todos.add("9. sınıf: üçgenler sunumu") }
        val t0 = main { todos.items.first { it.text == "Ödevleri kontrol et" } }
        main { todos.toggle(t0) }
        assertTrue(main { todos.items.first { it.id == t0.id }.done })
        assertEquals(t0.copy(done = true), todos.decode(todos.encode(t0.copy(done = true))))
        rule.onAllNodesWithText("Yapılacaklar")[0].performClick()
        shot("21-yapilacaklar")
        rule.onNodeWithContentDescription("Kapat").performClick()

        // çöp kutusu: sil → çöpte görünür → geri getir
        val store = com.poyankee.note.data.Store(rule.activity)
        val nb = main { store.create("Silinecek", Background.Paper(Template.BLANK), com.poyankee.note.core.PageSize.A4_PORTRAIT, "") }
        store.delete(nb.id); store.flush()
        Thread.sleep(300)
        assertTrue(store.listTrash().any { it.first.id == nb.id })
        assertTrue(store.list().none { it.id == nb.id })
        store.restoreFromTrash(nb.id)
        assertTrue(store.list().any { it.id == nb.id })
        store.delete(nb.id); store.flush(); Thread.sleep(300)

        // renk teması
        main { st.colorTheme.value = "OCEAN" }
        shot("22-tema-okyanus")
        main { st.colorTheme.value = "DEFAULT" }
    }

    @Test
    fun libraryWrongBookSearch() {
        val v = newNotebook()
        val c = v.controller
        val ctx = rule.activity

        // hazır kütüphane: panel ve ekleme
        rule.onNodeWithContentDescription("Kütüphane").performClick()
        rule.waitForIdle()
        shot("23-kutuphane")
        val before = items(c).size
        main { c.insertLibrary(com.poyankee.note.core.Library.entries.first { it.id == "unit_circle" }.build()) }
        assertTrue(items(c).size > before + 10)
        assertTrue(main { c.hasSelection })
        rule.onAllNodesWithText("Kesir modelleri")[0].performClick()
        shot("24-kesir-modelleri")
        rule.onNodeWithContentDescription("Kütüphane").performClick()
        // kütüphaneye kaydet
        val sel = main { v.selectedItems() }
        assertTrue(com.poyankee.note.data.UserLibrary.get(ctx).save("Birim çemberim", sel) > 0)
        assertTrue(com.poyankee.note.data.UserLibrary.get(ctx).entries.any { it.name == "Birim çemberim" })
        shot("25-secim-cubugu")
        main { v.clearSelection() }

        // arama
        val req = com.poyankee.note.editor.TextRequest(0, 100f, 600f, null)
        main { c.finishText(req, "Pisagor teoremi örneği", 16f) }
        val found = c.searchBlocking("pisagor")
        assertTrue("arama sonuç vermedi", found.any { it.first == 0 })

        // değer tablosundan grafik
        assertEquals(null, main { c.plotPoints(listOf(-1f to 1f, 0f to 0f, 1f to 1f), true, 0xFF1565C0.toInt()) })

        // yanlış defteri: dikdörtgen içine alınan bölge ayrı deftere eklenir
        val st = com.poyankee.note.data.Settings.get(ctx)
        main { c.captureToWrongNotebook(0, 40f, 40f, 400f, 300f) }
        rule.waitUntil(15_000) { st.wrongNotebook.value.isNotEmpty() && File(c.store.dir(st.wrongNotebook.value), "notebook.bin").exists() &&
            (c.store.dir(st.wrongNotebook.value).listFiles()?.any { it.name.startsWith("wq_") } == true) }
        Thread.sleep(500)
        val wb = c.store.load(st.wrongNotebook.value)
        assertTrue(wb.pages.last().items.any { it is com.poyankee.note.core.ImageItem })
        assertEquals(c.nb.id, st.wrongSource(wb.pages.last().id)?.first)

        // depolama ve hata kaydı
        val u = com.poyankee.note.data.StorageInfo.usage(ctx, c.store)
        assertTrue(u.total > 0)
        com.poyankee.note.data.CrashLog.list(ctx)

        // renkli seçim çubuğu, kaynağa git: yanlış defterini aç
        main { c.goToPage(0) }
    }

    @Test
    fun finalBatch() {
        val v = newNotebook()
        val c = v.controller
        val ctx = rule.activity
        val st = com.poyankee.note.data.Settings.get(ctx)
        val w = v.width.toFloat(); val h = v.height.toFloat()

        // bilgi kutusu ve konu özeti
        main { c.selectTool(Tool.PEN) }
        penLine(v, w * 0.3f, h * 0.3f, w * 0.5f, h * 0.32f)
        penLine(v, w * 0.3f, h * 0.36f, w * 0.45f, h * 0.38f)
        main { c.selectTool(Tool.LASSO) }
        penPath(v, listOf(w * 0.25f to h * 0.25f, w * 0.6f to h * 0.25f, w * 0.6f to h * 0.45f, w * 0.25f to h * 0.45f, w * 0.25f to h * 0.26f))
        assertTrue(main { c.hasSelection })
        main { c.makeInfoBox(EditorController.InfoBox.RULE) }
        assertTrue(items(c).any { it is com.poyankee.note.core.TextItem && it.text.startsWith(com.poyankee.note.editor.BOX_MARK) })
        shot("26-bilgi-kutusu")
        val pagesBefore = main { c.nb.pages.size }
        assertEquals(1, main { c.buildSummary() })
        assertEquals(pagesBefore + 1, main { c.nb.pages.size })
        shot("27-konu-ozeti")
        main { c.goToPage(0) }

        // hareketli geometri: üçgen seçiliyken ölçüler
        main { c.selectTool(Tool.POLYGON); c.polygonSides = 3 }
        penLine(v, w * 0.6f, h * 0.7f, w * 0.6f, h * 0.55f)
        val tri = items(c).last() as ShapeItem
        main { c.selectTool(Tool.LASSO); v.select(0, listOf(tri)) }
        val m = com.poyankee.note.core.Measure.polygon((tri.geom as Geom.Polyline).pts)
        assertEquals(180f, m.angles.sum(), 0.5f)
        shot("28-canli-olcu")
        main { v.clearSelection() }

        // sayfa no + tarih, çözüm katmanı (kırmızı çözümler gizlenir)
        main { st.pageFooter.value = 2; c.selectTool(Tool.PEN); c.setColor(0xFFD32F2F.toInt()) }
        penLine(v, w * 0.2f, h * 0.8f, w * 0.5f, h * 0.82f)
        main { st.hideSolutions.value = true; v.refreshAll() }
        shot("29-sayfa-no-cozum-gizli")
        val out = File(ctx.cacheDir, "soru-kagidi.pdf")
        val pages = main { c.nb.pages.toList() }
        Exporter.export(ctx, c.nb, c.store.dir(c.nb.id), pages, out, { null }, 2, st.solutionColor.value) { _, _ -> }
        assertTrue(out.length() > 500)
        main { st.hideSolutions.value = false; st.pageFooter.value = 0; c.setColor(0xFF1A1A1A.toInt()); v.refreshAll() }

        // arayüz boyutu ve göz dostu mod
        main { st.uiSize.value = 3 }
        shot("30-cok-buyuk-arayuz")
        main { st.uiSize.value = 1; st.eyeComfort.value = true }
        shot("31-goz-dostu")
        main { st.eyeComfort.value = false }

        // arayüz stilleri: Canlı ve Koyu (üstte araç şeridi)
        main { st.uiStyle.value = "VIVID"; c.selectTool(Tool.PEN) }
        shot("33-stil-canli")
        main { st.uiStyle.value = "DARK" }
        shot("34-stil-koyu")
        main { st.uiStyle.value = "CLEAN" }

        // PIN deneme sınırı
        repeat(5) { com.poyankee.note.ui.PinGuard.fail() }
        assertTrue(com.poyankee.note.ui.PinGuard.waitSeconds() > 0)
        com.poyankee.note.ui.PinGuard.success()
        assertEquals(0, com.poyankee.note.ui.PinGuard.waitSeconds())

        // ayarlar yedeğe girer
        assertTrue(c.store.exportSettings().contains("pageFooter"))

        // İngilizce arayüz (sözlükten)
        com.poyankee.note.core.L.dict = ctx.assets.open("i18n/en.tsv").bufferedReader().use { com.poyankee.note.core.L.parse(it.readText()) }
        main { com.poyankee.note.core.L.english = true }
        main { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitUntil(5000) { rule.onAllNodesWithText("Notebooks").fetchSemanticsNodes().isNotEmpty() }
        shot("32-ingilizce")
        main { com.poyankee.note.core.L.english = false }
    }

    @Test
    fun pdfOperations() {
        val ctx = rule.activity
        val src = File(ctx.cacheDir, "ornek3.pdf")
        val doc = PdfDocument()
        for (i in 1..3) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i).create())
            page.canvas.drawText("Sayfa $i", 100f, 100f, Paint().apply { textSize = 30f })
            doc.finishPage(page)
        }
        FileOutputStream(src).use { doc.writeTo(it) }
        doc.close()
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", src)
        val store = com.poyankee.note.data.Store(ctx)
        val nb = store.importPdf(ctx, uri)
        assertEquals(3, nb.pages.size)

        fun pdfPages(id: String): Int {
            val n = store.load(id)
            val f = File(store.dir(id), n.pdfFile!!)
            return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY).use { fd -> PdfRenderer(fd).use { it.pageCount } }
        }
        // ayır: 2-3. sayfalar
        val part = com.poyankee.note.data.PdfOps.extract(ctx, store, nb.id, listOf(1, 2), "Bölüm")
        assertEquals(2, store.load(part.id).pages.size)
        assertEquals(2, pdfPages(part.id))
        // birleştir
        val merged = com.poyankee.note.data.PdfOps.merge(ctx, store, listOf(nb.id, part.id), "Birleşik")
        assertEquals(5, store.load(merged.id).pages.size)
        assertEquals(5, pdfPages(merged.id))
        // başka PDF'ten sayfa ekle (1. sayfadan sonra)
        val added = com.poyankee.note.data.PdfOps.insertPdf(ctx, store, nb.id, uri, 0)
        assertEquals(3, added)
        val after = store.load(nb.id)
        assertEquals(6, after.pages.size)
        assertEquals(6, pdfPages(nb.id))
        assertEquals(3, (after.pages[1].background as Background.PdfPage).index)
        // fotoğraflardan defter
        val png = File(ctx.cacheDir, "foto.png")
        val bmp = android.graphics.Bitmap.createBitmap(400, 600, android.graphics.Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.LTGRAY) }
        FileOutputStream(png).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val imgUri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", png)
        val photos = com.poyankee.note.data.PdfOps.fromImages(ctx, store, listOf(imgUri, imgUri), "Fotoğraflar", "")
        assertEquals(2, store.load(photos.id).pages.size)
        assertEquals(892f, store.load(photos.id).pages[0].height, 2f)
        for (id in listOf(nb.id, part.id, merged.id, photos.id)) store.delete(id)
        store.flush()
    }

    @Test
    fun pdfRotateCrop() {
        val ctx = rule.activity
        val src = File(ctx.cacheDir, "ornek2.pdf")
        val doc = PdfDocument()
        for (i in 1..2) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i).create())
            page.canvas.drawText("Sayfa $i", 100f, 100f, Paint().apply { textSize = 30f })
            page.canvas.drawRect(80f, 300f, 500f, 700f, Paint().apply { color = android.graphics.Color.BLUE })
            doc.finishPage(page)
        }
        FileOutputStream(src).use { doc.writeTo(it) }
        doc.close()
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", src)
        main {
            ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").setClass(ctx, MainActivity::class.java).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }
        var v: InkView? = null
        rule.waitUntil(20_000) {
            v = main { findInk(rule.activity.window.decorView) }
            v != null && main { v!!.width > 0 && v!!.controller.nb.pages.size == 2 }
        }
        val c = v!!.controller
        main { c.rotatePage(0) }
        assertEquals(842f, main { c.nb.pages[0].width }, 0.5f)
        main { c.setCrop(1, 0.1f, 0f, 0.2f, 0f) }
        assertEquals(842f * 0.7f, main { c.nb.pages[1].height }, 0.5f)
        // döndürülmüş / kırpılmış PDF sayfası gerçekten çiziliyor mu (koyu yazı pikseli olmalı)
        fun darkPixels(bg: Background.PdfPage): Int {
            val latch = java.util.concurrent.CountDownLatch(1)
            var bmp: android.graphics.Bitmap? = null
            c.pdf!!.runOnPdfThread { bmp = c.pdf!!.renderBlocking(bg, 1f); latch.countDown() }
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS)
            val b = bmp ?: return -1
            var n = 0
            for (y in 0 until b.height step 2) for (x in 0 until b.width step 2) if ((b.getPixel(x, y) and 0xFF) < 100) n++
            return n
        }
        val r0 = main { c.nb.pages[0].background as Background.PdfPage }
        val r1 = main { c.nb.pages[1].background as Background.PdfPage }
        assertTrue("döndürülmüş PDF boş çiziliyor", darkPixels(r0) > 20)
        assertTrue("kırpılmış PDF boş çiziliyor", darkPixels(r1) > 20)
        Thread.sleep(1500)
        shot("16-pdf-dondur-kirp")
        val out = File(ctx.cacheDir, "dk.pdf")
        Exporter.export(ctx, c.nb, c.store.dir(c.nb.id), main { c.nb.pages.toList() }, out, { null }) { _, _ -> }
        ParcelFileDescriptor.open(out, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { r ->
                assertEquals(2, r.pageCount)
                r.openPage(0).use { p -> assertTrue("döndürülmüş sayfa yatay olmalı", p.width > p.height) }
                r.openPage(1).use { p -> assertEquals(842f * 0.7f, p.height.toFloat(), 3f) }
            }
        }
    }
}
