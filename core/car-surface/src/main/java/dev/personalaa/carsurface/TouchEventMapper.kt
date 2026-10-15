package dev.personalaa.carsurface

import android.os.SystemClock
import android.view.MotionEvent
import androidx.car.app.SurfaceContainer

object TouchEventMapper {
    fun click(container: SurfaceContainer, x: Float, y: Float): MotionEvent {
        val down = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_DOWN,
            x,
            y,
            0,
        )
        val up = MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis() + 50,
            MotionEvent.ACTION_UP,
            x,
            y,
            0,
        )
        down.recycle()
        return up
    }

    fun scroll(container: SurfaceContainer, x: Float, y: Float, deltaY: Float): MotionEvent {
        return MotionEvent.obtain(
            SystemClock.uptimeMillis(),
            SystemClock.uptimeMillis(),
            MotionEvent.ACTION_SCROLL,
            x,
            y,
            0,
        ).apply {
            // ACTION_SCROLL uses axis values; WebView handles scroll via dispatchTouchEvent chain.
            offsetLocation(0f, deltaY)
        }
    }

    fun dispatchClick(view: android.view.View, x: Float, y: Float) {
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, x, y, 0)
        view.dispatchTouchEvent(down)
        view.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    fun dispatchScroll(view: android.view.View, x: Float, y: Float, deltaY: Float) {
        val now = SystemClock.uptimeMillis()
        val move = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE, x, y - deltaY, 0)
        view.dispatchTouchEvent(move)
        move.recycle()
    }
}
