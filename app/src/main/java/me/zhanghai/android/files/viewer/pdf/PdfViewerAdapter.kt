/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.viewer.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.files.coil.fadeIn
import me.zhanghai.android.files.databinding.ImageViewerItemBinding
import me.zhanghai.android.files.util.fadeInUnsafe
import me.zhanghai.android.files.util.fadeOutUnsafe
import me.zhanghai.android.files.util.layoutInflater
import me.zhanghai.android.files.util.shortAnimTime

class PdfViewerAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val listener: (View) -> Unit
) : RecyclerView.Adapter<PdfViewerAdapter.ViewHolder>() {
    private var pdfRenderer: PdfRenderer? = null

    override fun getItemCount(): Int {
        return pdfRenderer?.pageCount ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ImageViewerItemBinding.inflate(parent.context.layoutInflater, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.image.setOnPhotoTapListener { view, _, _ -> listener(view) }
        loadImage(binding, position)
    }

    fun setPdfRender(pdfRenderer: PdfRenderer) {
        this.pdfRenderer = pdfRenderer
        notifyDataSetChanged()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)

        val binding = holder.binding
        binding.image.dispose()
    }

    private fun loadImage(binding: ImageViewerItemBinding, position: Int) {

        lifecycleOwner.lifecycleScope.launch {
            val imageInfo = try {
                withContext(Dispatchers.IO) {
                    renderPdfPage(
                        pdfRenderer!!.openPage(position),
                        1080,
                        1920
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showError(binding, e)
                return@launch
            }
            loadImageWithInfo(binding, imageInfo)
        }
    }

    private fun loadImageWithInfo(
        binding: ImageViewerItemBinding,
        imageInfo: Bitmap
    ) {
        binding.image.setImageBitmap(imageInfo)
    }

    private fun renderPdfPage(page: PdfRenderer.Page, width: Int, height: Int): Bitmap {
        val xscale = 1f * width / page.width
        val yscale = 1f * height / page.height
        var w: Int = width
        var h: Int = height
        if (xscale > yscale) {
            h = (page.height * xscale).toInt()
        } else {
            w = (page.width * yscale).toInt()
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    private fun showError(binding: ImageViewerItemBinding, throwable: Throwable) {
        binding.progress.fadeOutUnsafe()
        binding.errorText.text = throwable.toString()
        binding.errorText.fadeInUnsafe(true)
        binding.image.isVisible = false
        binding.largeImage.isVisible = false
    }

    class ViewHolder(val binding: ImageViewerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.image.isVisible = true
        }
    }
}
