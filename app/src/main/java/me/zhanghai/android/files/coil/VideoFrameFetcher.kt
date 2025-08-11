/*
 * Copyright (c) 2022 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.coil

import android.graphics.Bitmap
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.DecodeUtils
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import me.zhanghai.android.files.compat.getFrameAtTimeCompat
import me.zhanghai.android.files.compat.getScaledFrameAtTimeCompat
import me.zhanghai.android.files.compat.use
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class VideoFrameFetcher(
    private val options: Options,
    private val setDataSource: MediaMetadataRetriever.() -> Unit
) : Fetcher {
    override suspend fun fetch(): FetchResult =
        MediaMetadataRetriever().use { retriever ->
            retriever.setDataSource()
            val rotation =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull() ?: 0
            var srcWidth: Int
            var srcHeight: Int
            when (rotation) {
                90, 270 -> {
                    srcWidth =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.toIntOrNull() ?: 0
                    srcHeight =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            ?.toIntOrNull() ?: 0
                }
                else -> {
                    srcWidth =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                            ?.toIntOrNull() ?: 0
                    srcHeight =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                            ?.toIntOrNull() ?: 0
                }
            }
            val durationMillis =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L
            // 1/3 is the first percentage tried by totem-video-thumbnailer.
            // @see https://gitlab.gnome.org/GNOME/totem/-/blob/master/src/totem-video-thumbnailer.c#L543
            val framePercent = (1.0 / 3.0)
            val frameMicros = TimeUnit.MICROSECONDS.convert(
                (framePercent * durationMillis).roundToLong(), TimeUnit.MILLISECONDS
            )
            val frameOption = MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            val bitmapParams = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                MediaMetadataRetriever.BitmapParams().apply { preferredConfig = Bitmap.Config.ARGB_8888 }
            } else {
                null
            }
            val outBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                && srcWidth > 0 && srcHeight > 0) {
                val dstWidth = options.size.widthPx(options.scale) { srcWidth }
                val dstHeight = options.size.heightPx(options.scale) { srcHeight }
                val rawScale = DecodeUtils.computeSizeMultiplier(
                    srcWidth = srcWidth,
                    srcHeight = srcHeight,
                    dstWidth = dstWidth,
                    dstHeight = dstHeight,
                    scale = options.scale
                )
                val scale = rawScale
                val width = (scale * srcWidth).roundToInt()
                val height = (scale * srcHeight).roundToInt()
                retriever.getScaledFrameAtTimeCompat(
                    frameMicros, frameOption, width, height, bitmapParams
                )
            } else {
                retriever.getFrameAtTimeCompat(frameMicros, frameOption, bitmapParams)?.also {
                    srcWidth = it.width
                    srcHeight = it.height
                }
            }
            val dstWidth = options.size.widthPx(options.scale) { srcWidth }
            val dstHeight = options.size.heightPx(options.scale) { srcHeight }
            val rawScale = DecodeUtils.computeSizeMultiplier(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = dstWidth,
                dstHeight = dstHeight,
                scale = options.scale
            )
            checkNotNull(outBitmap) { "Failed to decode frame at $frameMicros microseconds" }
            val scale = rawScale
            val width = (scale * srcWidth).roundToInt()
            val height = (scale * srcHeight).roundToInt()
            val isValidSize = outBitmap.width == width && outBitmap.height == height

            val isValidConfig = outBitmap.config?.isHardware != true
            val bitmap = if (isValidSize && isValidConfig) {
                outBitmap
            } else {
                val config = Bitmap.Config.ARGB_8888
                createBitmap(width, height, config).applyCanvas {
                    scale(scale.toFloat(), scale.toFloat())
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    drawBitmap(outBitmap, 0f, 0f, paint)
                    outBitmap.recycle()
                }
            }
            ImageFetchResult(
                image = bitmap.asImage(),
                isSampled = scale < 1.0,
                dataSource = DataSource.DISK
            )
        }

    abstract class Factory<T : Any> : Fetcher.Factory<T> {
        override fun create(data: T, options: Options, imageLoader: ImageLoader): Fetcher =
            VideoFrameFetcher(options) { setDataSource(data) }

        protected abstract fun MediaMetadataRetriever.setDataSource(data: T)
    }
}
