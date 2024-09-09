/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.viewer.pdf

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.fragment.app.commit
import java8.nio.file.Path
import me.zhanghai.android.files.app.AppActivity
import me.zhanghai.android.files.util.extraPath
import me.zhanghai.android.files.util.putArgs

class PdfViewerActivity : AppActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Calls ensureSubDecor().
        findViewById<View>(android.R.id.content)
        if (savedInstanceState == null) {
            val intent = intent
            val fragment = PdfViewerFragment()
                .putArgs(PdfViewerFragment.Args(intent))
            supportFragmentManager.commit { add(android.R.id.content, fragment) }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    companion object {

        fun putExtras(intent: Intent, path: Path) {
            // All extra put here must be framework classes, or we may crash the resolver activity.
            intent.extraPath = path
        }
    }
}
