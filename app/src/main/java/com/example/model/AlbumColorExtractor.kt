package com.example.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class ExtractedAlbumColor(
    val dominantColor: Color,
    val gradientTop: Color,
    val gradientMid: Color,
    val gradientBottom: Color,
    val isDark: Boolean,
    // Rich multi-color palette sampled directly from the artwork
    val palette: List<Color> = listOf(gradientTop, gradientMid, gradientBottom)
)

object AlbumColorExtractor {
    private val cache = mutableMapOf<String, ExtractedAlbumColor>()

    // Neutral fallback
    val DEFAULT = buildResult(120, 120, 130, emptyList())

    suspend fun extractFromAsset(context: Context, path: String): ExtractedAlbumColor = withContext(Dispatchers.IO) {
        if (cache.containsKey(path)) return@withContext cache[path]!!

        try {
            val assetPath = if (path.startsWith("file:///android_asset/")) {
                path.removePrefix("file:///android_asset/")
            } else {
                path
            }
            val stream: InputStream = context.assets.open(assetPath)
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()

            val result = extractFromBitmap(bitmap)
            cache[path] = result
            result
        } catch (e: Exception) {
            DEFAULT
        }
    }

    fun extractFromBitmap(bitmap: Bitmap): ExtractedAlbumColor {
        val scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, false)
        val pixels = IntArray(48 * 48)
        scaled.getPixels(pixels, 0, 48, 0, 0, 48, 48)

        val buckets = mutableMapOf<Int, BucketData>()

        for (pixel in pixels) {
            val a = (pixel shr 24) and 0xff
            if (a < 125) continue
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff

            val (h, s, l) = rgbToHsl(r, g, b)
            var weight = s * s
            if (l < 0.10f || l > 0.92f) weight *= 0.15f
            if (s < 0.10f) weight *= 0.1f
            weight += 0.01f

            val key = (h / 20f).roundToInt()
            val cur = buckets.getOrPut(key) { BucketData() }
            cur.r += r * weight
            cur.g += g * weight
            cur.b += b * weight
            cur.weight += weight
        }

        val sortedBuckets = buckets.values.filter { it.weight > 0f }.sortedByDescending { it.weight }

        if (sortedBuckets.isEmpty()) {
            return DEFAULT
        }

        val topBucket = sortedBuckets.first()
        val r = (topBucket.r / topBucket.weight).roundToInt()
        val g = (topBucket.g / topBucket.weight).roundToInt()
        val b = (topBucket.b / topBucket.weight).roundToInt()

        // Extract diverse secondary & tertiary colors from the artwork
        val extractedPalette = sortedBuckets.take(4).map { bkt ->
            val br = (bkt.r / bkt.weight).roundToInt().coerceIn(0, 255)
            val bg = (bkt.g / bkt.weight).roundToInt().coerceIn(0, 255)
            val bb = (bkt.b / bkt.weight).roundToInt().coerceIn(0, 255)
            Color(br, bg, bb)
        }

        return buildResult(r, g, b, extractedPalette)
    }

    private class BucketData(
        var r: Float = 0f,
        var g: Float = 0f,
        var b: Float = 0f,
        var weight: Float = 0f
    )

    private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val max = max(rf, max(gf, bf))
        val min = min(rf, min(gf, bf))
        val l = (max + min) / 2f
        val d = max - min

        var h = 0f
        var s = 0f

        if (d != 0f) {
            s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
            h = when (max) {
                rf -> ((gf - bf) / d + (if (gf < bf) 6f else 0f)) / 6f
                gf -> ((bf - rf) / d + 2f) / 6f
                else -> ((rf - gf) / d + 4f) / 6f
            }
        }
        return Triple(h * 360f, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Int, Int, Int> {
        val hNorm = h / 360f
        if (s == 0f) {
            val v = (l * 255).roundToInt()
            return Triple(v, v, v)
        }

        fun hue2rgb(p: Float, q: Float, t: Float): Float {
            var v = t
            if (v < 0f) v += 1f
            if (v > 1f) v -= 1f
            if (v < 1f / 6f) return p + (q - p) * 6f * v
            if (v < 1f / 2f) return q
            if (v < 2f / 3f) return p + (q - p) * (2f / 3f - v) * 6f
            return p
        }

        val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
        val p = 2f * l - q

        val r = hue2rgb(p, q, hNorm + 1f / 3f)
        val g = hue2rgb(p, q, hNorm)
        val b = hue2rgb(p, q, hNorm - 1f / 3f)

        return Triple((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt())
    }

    private fun buildResult(r: Int, g: Int, b: Int, paletteColors: List<Color>): ExtractedAlbumColor {
        val (h, s, l) = rgbToHsl(r, g, b)
        val top = hslToRgb(h, min(s * 1.15f, 1f), max(min(l, 0.55f), 0.30f))
        val mid = hslToRgb((h + 12f) % 360f, min(s * 0.95f, 1f), max(min(l * 1.15f, 0.75f), 0.45f))
        val bottom = hslToRgb((h + 24f) % 360f, min(s * 0.85f, 1f), max(min(l * 1.35f, 0.90f), 0.60f))

        val topColor = Color(top.first, top.second, top.third)
        val midColor = Color(mid.first, mid.second, mid.third)
        val bottomColor = Color(bottom.first, bottom.second, bottom.third)

        val fullPalette = if (paletteColors.size >= 2) {
            listOf(topColor) + paletteColors.take(3) + listOf(bottomColor)
        } else {
            listOf(topColor, midColor, bottomColor)
        }

        val luminance = (0.299f * top.first + 0.587f * top.second + 0.114f * top.third) / 255f
        val isDark = luminance < 0.55f

        return ExtractedAlbumColor(
            dominantColor = Color(r, g, b),
            gradientTop = topColor,
            gradientMid = midColor,
            gradientBottom = bottomColor,
            isDark = isDark,
            palette = fullPalette
        )
    }
}

