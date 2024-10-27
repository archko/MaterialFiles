/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.viewer.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.View
import android.view.ViewGroup
import androidx.collection.arrayMapOf
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import coil.dispose
import me.zhanghai.android.files.databinding.PdfViewerItemBinding
import me.zhanghai.android.files.util.displayHeight
import me.zhanghai.android.files.util.displayWidth
import me.zhanghai.android.files.util.layoutInflater

class PdfViewerAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val listener: (View) -> Unit
) : RecyclerView.Adapter<PdfViewerAdapter.ViewHolder>() {
    private var pdfRenderer: PdfRenderer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var width = 1080
    private var height = 1920
    private var sizeMap = arrayMapOf<Int, Size>()

    init {
        width = (lifecycleOwner as PdfViewerFragment).requireContext().displayWidth
        height = lifecycleOwner.requireContext().displayHeight
    }

    fun updateSize(w: Int, h: Int) {
        width = w
        height = h
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
        AsyncTask.SERIAL_EXECUTOR.execute {
            val pc = pdfRenderer.pageCount
            /*for (i in 0 until pc) {
                val page = pdfRenderer.openPage(i)
                sizeMap[i] = Size(page.width, page.height)
            }*/
            val page = pdfRenderer.openPage(0)
            sizeMap[0] = Size(page.width, page.height)
            page.close()
            mainHandler.post {
                notifyDataSetChanged()
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)

        val binding = holder.binding
        binding.image.dispose()
    }

    private fun loadImage(binding: PdfViewerItemBinding, position: Int) {
        binding.image.tag = position
        binding.image.setImageBitmap(null)
        AsyncTask.SERIAL_EXECUTOR.execute {
            val imageInfo: Bitmap = try {
                renderPdfPage(
                    pdfRenderer!!.openPage(position),
                    width,
                    height
                )
            } catch (e: Exception) {
                e.printStackTrace()
                showError(binding, e)
                return@execute
            }
            mainHandler.post {
                if (binding.image.tag == position) {
                    loadImageWithInfo(binding, imageInfo)
                }
            }
        }
    }

    private fun loadImageWithInfo(
        binding: PdfViewerItemBinding,
        bitmap: Bitmap?
    ) {
        bitmap?.let {
            val w = bitmap.width
            val h = bitmap.height
            //Log.d("result", "width:$w-$h")
            binding.image.setImageBitmap(bitmap)
            var lp = binding.image.layoutParams
            if (lp == null) {
                lp = LayoutParams(w, h)
            }
            lp.width = w
            lp.height = h
            binding.image.layoutParams = lp
        }
    }

    private fun getSize(pW: Int, pH: Int): Size {
        val xscale = 1f * width / pW
        val w: Int = width
        val h: Int = (pH * xscale).toInt()
        //Log.d("create", "width:${width}-${height}, result:$w-$h, page;$pW, $pH, scale:$xscale")
        return Size(w, h)
    }

    private fun renderPdfPage(page: PdfRenderer.Page, width: Int, height: Int): Bitmap {
        val size = getSize(page.width, page.height)

        //Log.d("render", "${page.index} width:${size.width}-${size.height}, page:${page.width}-${page.height}")
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    private fun showError(binding: PdfViewerItemBinding, throwable: Throwable) {
    }

    inner class ViewHolder(val binding: PdfViewerItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            if (sizeMap.size > 0) {
                val p0size = sizeMap[0]
                val pw = p0size!!.width
                val ph = p0size.height
                val size = getSize(pw, ph)

                //Log.d("create", "width:${size.width}-${size.height}, page:$pw-$ph")
                var lp = binding.image.layoutParams
                if (lp == null) {
                    lp = LayoutParams(size.width, size.height)
                }
                lp.width = size.width
                lp.height = size.height
                binding.image.layoutParams = lp
            }
        }
    }
}
