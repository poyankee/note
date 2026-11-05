package com.poyankee.note.core

import kotlin.math.atan2

/** Afin dönüşüm: x' = a·x + c·y + e,  y' = b·x + d·y + f. */
data class Affine(val a: Float, val b: Float, val c: Float, val d: Float, val e: Float, val f: Float) {
    fun mapX(x: Float, y: Float) = a * x + c * y + e
    fun mapY(x: Float, y: Float) = b * x + d * y + f

    /** Önce [first], sonra bu dönüşüm. */
    fun after(first: Affine) = Affine(
        a * first.a + c * first.b, b * first.a + d * first.b,
        a * first.c + c * first.d, b * first.c + d * first.d,
        a * first.e + c * first.f + e, b * first.e + d * first.f + f,
    )

    fun inverse(): Affine {
        val det = a * d - b * c
        return Affine(d / det, -b / det, -c / det, a / det, (c * f - d * e) / det, (b * e - a * f) / det)
    }

    /** Dönme + öteleme ise eşdeğer benzerlik dönüşümü (ölçek 1). */
    fun toSimilarity() = Similarity(dx = e, dy = f, angle = atan2(b, a))

    companion object {
        val ID = Affine(1f, 0f, 0f, 1f, 0f, 0f)
        fun translate(x: Float, y: Float) = Affine(1f, 0f, 0f, 1f, x, y)
        fun scale(s: Float) = Affine(s, 0f, 0f, s, 0f, 0f)
    }
}

/**
 * PDF sayfası döndürme / kırpma geometrisi.
 * "Tam" koordinat = PDF görüntüleyicinin verdiği sayfa (fullW × fullH).
 * "Görünen" koordinat = kırpılmış ve döndürülmüş sayfa (defterdeki sayfa).
 */
object PdfGeom {
    fun cropSize(fullW: Float, fullH: Float, crop: Crop) =
        fullW * (1f - crop.l - crop.r) to fullH * (1f - crop.t - crop.b)

    fun displaySize(fullW: Float, fullH: Float, rotation: Int, crop: Crop): Pair<Float, Float> {
        val (cw, ch) = cropSize(fullW, fullH, crop)
        return if (rotation % 180 == 90) ch to cw else cw to ch
    }

    /** Tam sayfa koordinatından görünen koordinata. */
    fun fullToDisplay(fullW: Float, fullH: Float, rotation: Int, crop: Crop): Affine {
        val (cw, ch) = cropSize(fullW, fullH, crop)
        val t = Affine.translate(-crop.l * fullW, -crop.t * fullH)
        val r = when (((rotation % 360) + 360) % 360) {
            90 -> Affine(0f, 1f, -1f, 0f, ch, 0f)
            180 -> Affine(-1f, 0f, 0f, -1f, cw, ch)
            270 -> Affine(0f, -1f, 1f, 0f, 0f, cw)
            else -> Affine.ID
        }
        return r.after(t)
    }

    /**
     * Sayfa yeni döndürme/kırpma ayarına geçince üzerindeki yazıların taşınması:
     * eski görünen koordinat → tam sayfa → yeni görünen koordinat.
     */
    fun itemTransform(fullW: Float, fullH: Float, oldRot: Int, oldCrop: Crop, newRot: Int, newCrop: Crop): Similarity {
        val old = fullToDisplay(fullW, fullH, oldRot, oldCrop)
        val new = fullToDisplay(fullW, fullH, newRot, newCrop)
        return new.after(old.inverse()).toSimilarity()
    }

    /**
     * Ekranda görünen kenar paylarını (üst, sağ, alt, sol) döndürme öncesi kırpma paylarına çevirir.
     * Kullanıcı kırpmayı her zaman gördüğü yöne göre ayarlar.
     */
    fun cropFromDisplayMargins(rotation: Int, top: Float, right: Float, bottom: Float, left: Float): Crop =
        when (((rotation % 360) + 360) % 360) {
            90 -> Crop(l = top, t = right, r = bottom, b = left)
            180 -> Crop(l = right, t = bottom, r = left, b = top)
            270 -> Crop(l = bottom, t = left, r = top, b = right)
            else -> Crop(l = left, t = top, r = right, b = bottom)
        }

    /** Tersi: (üst, sağ, alt, sol). */
    fun displayMargins(rotation: Int, c: Crop): FloatArray = when (((rotation % 360) + 360) % 360) {
        90 -> floatArrayOf(c.l, c.t, c.r, c.b)
        180 -> floatArrayOf(c.b, c.l, c.t, c.r)
        270 -> floatArrayOf(c.r, c.b, c.l, c.t)
        else -> floatArrayOf(c.t, c.r, c.b, c.l)
    }
}
