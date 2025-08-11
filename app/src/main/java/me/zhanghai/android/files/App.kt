package me.zhanghai.android.files

import android.app.Application
import android.graphics.Bitmap
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.allowRgb565
import coil3.request.bitmapConfig
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import me.zhanghai.android.files.app.application
import me.zhanghai.android.files.coil.AppIconApplicationInfoFetcherFactory
import me.zhanghai.android.files.coil.AppIconApplicationInfoKeyer
import me.zhanghai.android.files.coil.AppIconPackageNameFetcherFactory
import me.zhanghai.android.files.coil.AppIconPackageNameKeyer
import me.zhanghai.android.files.coil.PathAttributesFetcher
import me.zhanghai.android.files.coil.PathAttributesKeyer
import okio.Path.Companion.toOkioPath

/**
 * @author: archko 2025/8/15 :22:05
 */
class App : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        var directory = externalCacheDir?.resolve("image_cache")
        if (directory == null) {
            directory = cacheDir.resolve("image_cache")
        }
        return ImageLoader.Builder(this)
            .crossfade(true)
            .allowRgb565(true)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, MAX_MEMORY_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(directory.toOkioPath())
                    .maxSizePercent(MAX_DISK_CACHE_SIZE_PERCENTAGE)
                    .build()
            }
            .components {
                add(AppIconApplicationInfoKeyer())
                add(AppIconApplicationInfoFetcherFactory(application))
                add(AppIconPackageNameKeyer())
                add(AppIconPackageNameFetcherFactory(application))
                add(PathAttributesKeyer())
                add(PathAttributesFetcher.Factory(application))
                /*add(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoderDecoder.Factory()
                    } else {
                        GifDecoder.Factory()
                    }
                )*/
                add(SvgDecoder.Factory(false))
            }
            .build()
    }

    public companion object Companion {
        public var app: App? = null
            private set

        //一张图片4-5mb,200mb大概缓存50张
        public const val MAX_CACHE: Int = 300 * 1024 * 1024
        private val MAX_MEMORY_CACHE_SIZE_PERCENTAGE = 0.3
        private val MAX_DISK_CACHE_SIZE_PERCENTAGE = 0.2
    }
}