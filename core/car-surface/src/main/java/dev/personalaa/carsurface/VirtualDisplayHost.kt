package dev.personalaa.carsurface

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.car.app.SurfaceContainer

/**
 * Renders an arbitrary [View] tree into the Android Auto navigation [Surface].
 */
class VirtualDisplayHost(
    private val appContext: Context,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var virtualDisplay: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var root: FrameLayout? = null
    private var currentContainer: SurfaceContainer? = null
    private var contentView: View? = null

    fun attachContent(view: View) {
        contentView = view
        root?.let { mountContent(it, view) }
    }

    fun onSurfaceAvailable(container: SurfaceContainer) {
        mainHandler.post {
            releaseDisplay()
            currentContainer = container
            val surface = container.surface ?: return@post
            if (!surface.isValid) return@post

            val dm = appContext.getSystemService(DisplayManager::class.java)
            val density = container.dpi.takeIf { it > 0 } ?: appContext.resources.displayMetrics.densityDpi
            val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION

            virtualDisplay = dm.createVirtualDisplay(
                "aa-tube-display",
                container.width,
                container.height,
                density,
                surface,
                flags,
            )

            val display = virtualDisplay?.display ?: return@post
            presentation = object : Presentation(appContext, display) {
                override fun onCreate(savedInstanceState: android.os.Bundle?) {
                    super.onCreate(savedInstanceState)
                    val frame = FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                    setContentView(frame)
                    root = frame
                    contentView?.let { mountContent(frame, it) }
                }
            }
            presentation?.show()
        }
    }

    fun onSurfaceDestroyed(container: SurfaceContainer) {
        mainHandler.post {
            if (currentContainer?.surface == container.surface) {
                releaseDisplay()
                currentContainer = null
            }
        }
    }

    fun dispatchTouch(block: (View) -> Unit) {
        mainHandler.post {
            val target = contentView ?: return@post
            block(target)
        }
    }

    fun release() {
        mainHandler.post { releaseDisplay() }
    }

    private fun mountContent(frame: FrameLayout, view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
        frame.addView(
            view,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    private fun releaseDisplay() {
        runCatching { presentation?.dismiss() }
        presentation = null
        root = null
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
    }
}
