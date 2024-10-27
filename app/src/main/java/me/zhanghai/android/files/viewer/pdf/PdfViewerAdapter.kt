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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.android.files.databinding.PdfViewerItemBinding
import me.zhanghai.android.files.util.displayHeight
import me.zhanghai.android.files.util.displayWidth
import me.zhanghai.android.files.util.layoutInflater

class PdfViewerAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val listener: (View) -> Unit
) : RecyclerView.Adapter<PdfViewerAdapter.ViewHolder>() {
    private var pdfRenderer: PdfRenderer? = null
    private var width = 1080
    private var height = 1920

    init {
        width = (lifecycleOwner as PdfViewerFragment).requireContext().displayWidth
        height = lifecycleOwner.requireContext().displayHeight
    }

    override fun getItemCount(): Int {
        return pdfRenderer?.pageCount ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(PdfViewerItemBinding.inflate(parent.context.layoutInflater, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        binding.image.setOnClickListener { view -> listener(view) }
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

    private fun loadImage(binding: PdfViewerItemBinding, position: Int) {
        lifecycleOwner.lifecycleScope.launch {
            val imageInfo = try {
                withContext(Dispatchers.IO) {
                    renderPdfPage(
                        pdfRenderer!!.openPage(position),
                        width,
                        height
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
        binding: PdfViewerItemBinding,
        bitmap: Bitmap
    ) {
        binding.image.setImageBitmap(bitmap)
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

    private fun showError(binding: PdfViewerItemBinding, throwable: Throwable) {
    }

    class ViewHolder(val binding: PdfViewerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.image.isVisible = true
        }
    }
}
