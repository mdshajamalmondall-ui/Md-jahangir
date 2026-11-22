package com.example.editor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.local.GalleryPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

data class SavedBitmapResult(
    val uri: Uri,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val jpegBytes: ByteArray
)

object BitmapProcessor {
    private val httpClient = OkHttpClient.Builder().build()

    suspend fun loadBitmapForEditor(
        context: Context,
        photo: GalleryPhoto,
        driveAccessToken: String? = null,
        maxDimension: Int = 1440
    ): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            when {
                photo.drawableResId != null -> {
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeResource(context.resources, photo.drawableResId, opts)
                    val sample = calculateInSampleSize(opts.outWidth, opts.outHeight, maxDimension)
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    BitmapFactory.decodeResource(context.resources, photo.drawableResId, decodeOpts)
                }

                photo.uriString.startsWith("https://") -> {
                    val reqBuilder = Request.Builder().url(photo.uriString)
                    if (!driveAccessToken.isNullOrBlank()) {
                        reqBuilder.header("Authorization", "Bearer $driveAccessToken")
                    }
                    val bytes = httpClient.newCall(reqBuilder.build()).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        resp.body?.bytes() ?: error("Empty body")
                    }
                    decodeBytesSampled(bytes, maxDimension)
                }

                else -> {
                    val uri = Uri.parse(photo.uriString)
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, boundsOpts)
                    }
                    val sample = calculateInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimension)
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, decodeOpts)
                    }
                }
            }
        }.getOrNull()
    }

    private fun decodeBytesSampled(bytes: ByteArray, maxDimension: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (max(w, h) > maxDim && sample < 16) {
            sample *= 2
            w /= 2
            h /= 2
        }
        return sample
    }

    fun renderEditedBitmap(source: Bitmap, state: PhotoEditState): Bitmap {
        // 1. Apply 90-degree rotation & flips
        val baseMatrix = Matrix().apply {
            val sx = if (state.flipHorizontal) -1f else 1f
            val sy = if (state.flipVertical) -1f else 1f
            postScale(sx, sy, source.width / 2f, source.height / 2f)
            val rot = ((state.rotationQuarterTurns % 4) + 4) % 4 * 90f
            if (rot != 0f) {
                postRotate(rot)
            }
        }
        val oriented = Bitmap.createBitmap(source, 0, 0, source.width, source.height, baseMatrix, true)

        // 2. Apply straighten tilt with auto-crop scale so no black triangles appear
        val straightened = if (abs(state.straightenDegrees) > 0.2f) {
            val rad = Math.toRadians(abs(state.straightenDegrees).toDouble())
            val w = oriented.width.toFloat()
            val h = oriented.height.toFloat()
            val scaleW = (w * cos(rad) + h * sin(rad)) / w
            val scaleH = (w * sin(rad) + h * cos(rad)) / h
            val zoom = max(scaleW, scaleH).toFloat()
            val tiltMatrix = Matrix().apply {
                postRotate(state.straightenDegrees, w / 2f, h / 2f)
                postScale(zoom, zoom, w / 2f, h / 2f)
            }
            val temp = Bitmap.createBitmap(oriented.width, oriented.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(temp)
            c.drawBitmap(oriented, tiltMatrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            if (oriented !== source) oriented.recycle()
            temp
        } else {
            oriented
        }

        // 3. Apply aspect ratio center crop
        val cropped = state.cropAspect.ratio?.let { targetRatio ->
            val currentRatio = straightened.width.toFloat() / straightened.height.toFloat()
            val (cropW, cropH) = if (currentRatio > targetRatio) {
                val h = straightened.height
                val w = (h * targetRatio).roundToInt().coerceAtMost(straightened.width)
                w to h
            } else {
                val w = straightened.width
                val h = (w / targetRatio).roundToInt().coerceAtMost(straightened.height)
                w to h
            }
            val startX = ((straightened.width - cropW) / 2).coerceAtLeast(0)
            val startY = ((straightened.height - cropH) / 2).coerceAtLeast(0)
            val cropOut = Bitmap.createBitmap(straightened, startX, startY, cropW, cropH)
            if (straightened !== source && straightened !== cropOut) straightened.recycle()
            cropOut
        } ?: straightened

        // 4. Composite ColorMatrix + Vignette + Markup + Text
        val output = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val colorPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(buildColorMatrix(state))
        }
        canvas.drawBitmap(cropped, 0f, 0f, colorPaint)

        // Vignette overlay
        if (state.vignette > 0.01f) {
            val radius = max(output.width, output.height) * 0.72f
            val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    output.width / 2f,
                    output.height / 2f,
                    radius,
                    intArrayOf(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.argb((state.vignette * 215).roundToInt().coerceIn(0, 230), 0, 0, 0)
                    ),
                    floatArrayOf(0.42f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), vignettePaint)
        }

        // Freehand brush strokes
        val minSide = min(output.width, output.height).toFloat()
        state.strokes.forEach { stroke ->
            if (stroke.points.isNotEmpty()) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = stroke.colorValue.toInt()
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = max(4f, stroke.strokeWidthRatio * minSide)
                }
                val path = Path()
                val first = stroke.points.first()
                path.moveTo(first.x * output.width, first.y * output.height)
                if (stroke.points.size == 1) {
                    path.lineTo(first.x * output.width + 0.5f, first.y * output.height + 0.5f)
                } else {
                    for (i in 1 until stroke.points.size) {
                        val p = stroke.points[i]
                        path.lineTo(p.x * output.width, p.y * output.height)
                    }
                }
                canvas.drawPath(path, strokePaint)
            }
        }

        // Optional text sticker overlay
        if (state.textOverlay.isNotBlank()) {
            val textSizePx = max(28f, minSide * 0.068f)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = state.textColorValue.toInt()
                textSize = textSizePx
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(textSizePx * 0.16f, 0f, textSizePx * 0.06f, android.graphics.Color.argb(190, 0, 0, 0))
            }
            canvas.drawText(
                state.textOverlay.trim(),
                output.width / 2f,
                output.height * 0.86f,
                textPaint
            )
        }

        if (cropped !== source && cropped !== output) {
            cropped.recycle()
        }
        return output
    }

    private fun buildColorMatrix(state: PhotoEditState): ColorMatrix {
        val cm = ColorMatrix()

        // Preset base transform
        when (state.preset) {
            FilterPreset.ORIGINAL -> Unit
            FilterPreset.VIVID -> {
                val sat = ColorMatrix().apply { setSaturation(1.38f) }
                cm.postConcat(sat)
                cm.postConcat(contrastBrightnessMatrix(1.12f, 8f))
            }
            FilterPreset.NOIR -> {
                val sat = ColorMatrix().apply { setSaturation(0f) }
                cm.postConcat(sat)
                cm.postConcat(contrastBrightnessMatrix(1.24f, -6f))
            }
            FilterPreset.GOLDEN -> {
                cm.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.14f, 0.06f, 0f, 0f, 12f,
                            0.02f, 1.05f, 0f, 0f, 6f,
                            0f, 0f, 0.84f, 0f, -8f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            FilterPreset.CYBERPUNK -> {
                cm.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            1.15f, 0f, 0.18f, 0f, -6f,
                            0f, 1.02f, 0.12f, 0f, 0f,
                            0.12f, 0.08f, 1.28f, 0f, 10f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            FilterPreset.MATTE -> {
                val sat = ColorMatrix().apply { setSaturation(0.82f) }
                cm.postConcat(sat)
                cm.postConcat(contrastBrightnessMatrix(0.88f, 18f))
            }
            FilterPreset.SEPIA -> {
                cm.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            0.393f, 0.769f, 0.189f, 0f, 0f,
                            0.349f, 0.686f, 0.168f, 0f, 0f,
                            0.272f, 0.534f, 0.131f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
            FilterPreset.ARCTIC -> {
                cm.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            0.90f, 0f, 0.05f, 0f, -4f,
                            0f, 1.04f, 0.06f, 0f, 4f,
                            0.04f, 0.08f, 1.20f, 0f, 12f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                )
            }
        }

        // User Saturation
        if (abs(state.saturation - 1f) > 0.01f) {
            val sat = ColorMatrix().apply { setSaturation(state.saturation) }
            cm.postConcat(sat)
        }

        // User Contrast + Brightness + Fade
        val effectiveContrast = (state.contrast * (1f - state.fade * 0.35f)).coerceIn(0.4f, 2f)
        val effectiveBrightness = state.brightness * 125f + state.fade * 36f
        if (abs(effectiveContrast - 1f) > 0.01f || abs(effectiveBrightness) > 0.5f) {
            cm.postConcat(contrastBrightnessMatrix(effectiveContrast, effectiveBrightness))
        }

        // User Warmth
        if (abs(state.warmth) > 0.01f) {
            val w = state.warmth * 35f
            cm.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, w,
                        0f, 1f, 0f, 0f, w * 0.25f,
                        0f, 0f, 1f, 0f, -w,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }

        return cm
    }

    private fun contrastBrightnessMatrix(contrast: Float, brightnessOffset: Float): ColorMatrix {
        val translate = (-0.5f * contrast + 0.5f) * 255f + brightnessOffset
        return ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    suspend fun saveEditedBitmap(
        context: Context,
        bitmap: Bitmap,
        baseTitle: String
    ): Result<SavedBitmapResult> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanBase = baseTitle.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
                .ifBlank { "Lumina_Edit" }
            val fileName = "${cleanBase}_${System.currentTimeMillis()}.jpg"
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 94, baos)
            val bytes = baos.toByteArray()

            val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Lumina")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: error("Could not create MediaStore entry")
                resolver.openOutputStream(insertedUri)?.use { out ->
                    out.write(bytes)
                } ?: error("Could not open MediaStore output stream")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(insertedUri, values, null, null)
                insertedUri
            } else {
                val dir = File(context.filesDir, "edited_photos").apply { mkdirs() }
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { it.write(bytes) }
                Uri.fromFile(outFile)
            }

            SavedBitmapResult(
                uri = uri,
                width = bitmap.width,
                height = bitmap.height,
                sizeBytes = bytes.size.toLong(),
                jpegBytes = bytes
            )
        }
    }
}
