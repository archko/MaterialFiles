/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.coil

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.createBitmap
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.DecodeUtils
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import kotlin.math.roundToInt

class PdfPageFetcher(
    private val options: Options,
    private val openParcelFileDescriptor: () -> ParcelFileDescriptor
) : Fetcher {
    override suspend fun fetch(): FetchResult =
        openParcelFileDescriptor().use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val pageIndex = 0
                renderer.openPage(pageIndex).use { page ->
                    val srcWidth = page.width
                    check(srcWidth > 0) {
                        "PDF page $pageIndex width $srcWidth isn't greater than 0"
                    }
                    val srcHeight = page.height
                    check(srcWidth > 0) {
                        "PDF page $pageIndex height $srcHeight isn't greater than 0"
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
                    val scale = rawScale

                    val width = (scale * srcWidth).roundToInt()
                    val height = (scale * srcHeight).roundToInt()
                    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val backgroundColor = Color.WHITE
                    bitmap.eraseColor(backgroundColor)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    ImageFetchResult(
                        image = bitmap.asImage(),
                        isSampled = scale < 1.0,
                        dataSource = DataSource.DISK
                    )
                }
            }
        }

    companion object {
        const val PDF_BACKGROUND_COLOR_KEY = "coil#pdf_background_color"
        const val PDF_PAGE_INDEX_KEY = "coil#pdf_page_index"
    }

    abstract class Factory<T : Any> : Fetcher.Factory<T> {
        override fun create(data: T, options: Options, imageLoader: ImageLoader): Fetcher =
            PdfPageFetcher(options) { openParcelFileDescriptor(data) }

        protected abstract fun openParcelFileDescriptor(data: T): ParcelFileDescriptor
    }
}
