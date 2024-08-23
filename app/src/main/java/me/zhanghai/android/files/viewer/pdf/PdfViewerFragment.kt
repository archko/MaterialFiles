/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.viewer.pdf

import android.content.Intent
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import dev.chrisbanes.insetter.applySystemWindowInsetsToPadding
import java8.nio.file.Path
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.WriteWith
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.ImageViewerFragmentBinding
import me.zhanghai.android.files.file.fileProviderUri
import me.zhanghai.android.files.provider.common.delete
import me.zhanghai.android.files.util.ParcelableArgs
import me.zhanghai.android.files.util.ParcelableParceler
import me.zhanghai.android.files.util.ParcelableState
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.createSendImageIntent
import me.zhanghai.android.files.util.extraPath
import me.zhanghai.android.files.util.finish
import me.zhanghai.android.files.util.getState
import me.zhanghai.android.files.util.mediumAnimTime
import me.zhanghai.android.files.util.putState
import me.zhanghai.android.files.util.showToast
import me.zhanghai.android.files.util.startActivitySafe
import me.zhanghai.android.files.util.withChooser
import me.zhanghai.android.files.viewer.image.ConfirmDeleteDialogFragment
import me.zhanghai.android.systemuihelper.SystemUiHelper
import java.io.IOException

class PdfViewerFragment : Fragment(), ConfirmDeleteDialogFragment.Listener {
    private val args by args<Args>()
    private val argsPath by lazy { args.intent.extraPath }

    private lateinit var path: Path

    private lateinit var binding: ImageViewerFragmentBinding

    private lateinit var systemUiHelper: SystemUiHelper

    private lateinit var adapter: PdfViewerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        path = (savedInstanceState?.getState<State>()?.path ?: argsPath)!!

        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        ImageViewerFragmentBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (path == null) {
            finish()
            return
        }

        val activity = activity as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        // Our app bar will draw the status bar background.
        activity.window.statusBarColor = Color.TRANSPARENT
        binding.appBarLayout.applySystemWindowInsetsToPadding(left = true, top = true, right = true)
        systemUiHelper = SystemUiHelper(
            activity, SystemUiHelper.LEVEL_IMMERSIVE, SystemUiHelper.FLAG_IMMERSIVE_STICKY
        ) { visible: Boolean ->
            binding.appBarLayout.animate()
                .alpha(if (visible) 1f else 0f)
                .translationY(if (visible) 0f else -binding.appBarLayout.bottom.toFloat())
                .setDuration(mediumAnimTime.toLong())
                .setInterpolator(FastOutSlowInInterpolator())
                .start()
        }
        // This will set up window flags.
        systemUiHelper.show()
        binding.viewPager.apply {
            setBackgroundColor(Color.WHITE)
        }

        adapter = PdfViewerAdapter(this) { systemUiHelper.toggle() }.apply {
        }
        binding.viewPager.apply {
            // 1 is the default for the old androidx.viewpager.widget.ViewPager.
            offscreenPageLimit = 1
            adapter = this@PdfViewerFragment.adapter
        }
        val descriptor =
            ParcelFileDescriptor.open(path.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(descriptor)
        adapter.setPdfRender(pdfRenderer)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (path == null) {
            // We did finish the activity in onActivityCreated(), however we will still be called
            // here before the activity is actually finished.
            return
        }

        updateTitle()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putState(State(path))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        //inflater.inflate(R.menu.image_viewer, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_delete -> {
                confirmDelete()
                true
            }

            R.id.action_share -> {
                share()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    private fun confirmDelete() {
        ConfirmDeleteDialogFragment.show(path, this)
    }

    override fun delete(path: Path) {
        try {
            path.delete()
        } catch (e: IOException) {
            e.printStackTrace()
            showToast(e.toString())
            return
        }
        if (null == path) {
            finish()
            return
        }

        updateTitle()
        binding.viewPager.doOnPreDraw { binding.viewPager.requestTransform() }
    }

    private fun updateTitle() {
        requireActivity().title = path.fileName.toString()
    }

    private fun share() {
        val intent = path.fileProviderUri.createSendImageIntent()
            .apply { extraPath = path }
            .withChooser()
        startActivitySafe(intent)
    }

    @Parcelize
    class Args(val intent: Intent) : ParcelableArgs

    @Parcelize
    private class State(val path: @WriteWith<ParcelableParceler> Path) : ParcelableState
}
