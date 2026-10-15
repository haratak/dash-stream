package dev.personalaa.tube.car

import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import dev.personalaa.carsurface.TouchEventMapper
import dev.personalaa.carsurface.VirtualDisplayHost
import dev.personalaa.tube.TubeApplication

class TubeNavigationScreen(
    carContext: CarContext,
    private val app: TubeApplication,
) : Screen(carContext), SurfaceCallback {
    private val displayHost = VirtualDisplayHost(carContext)
    private var lastScrollY = 0f

    init {
        carContext.getCarService(AppManager::class.java).setSurfaceCallback(this)
        displayHost.attachContent(app.webViewController.obtainView())
        lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                    displayHost.release()
                }
            },
        )
    }

    override fun onGetTemplate(): Template {
        val back = Action.Builder()
            .setTitle("戻る")
            .setOnClickListener(
                OnClickListener {
                    app.webViewController.goBack()
                },
            )
            .build()
        val home = Action.Builder()
            .setTitle("ホーム")
            .setOnClickListener(
                OnClickListener {
                    app.webViewController.goHome()
                },
            )
            .build()
        val playPause = Action.Builder()
            .setTitle("再生/停止")
            .setOnClickListener(
                OnClickListener {
                    app.webViewController.togglePlayPause()
                },
            )
            .build()
        val search = Action.Builder()
            .setTitle("検索")
            .setOnClickListener(
                OnClickListener {
                    screenManager.push(TubeSearchScreen(carContext, app))
                },
            )
            .build()

        return NavigationTemplate.Builder()
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(back)
                    .addAction(home)
                    .addAction(playPause)
                    .addAction(search)
                    .build(),
            )
            .build()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        displayHost.onSurfaceAvailable(surfaceContainer)
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        displayHost.onSurfaceDestroyed(surfaceContainer)
    }

    override fun onClick(x: Float, y: Float) {
        displayHost.dispatchTouch { view ->
            TouchEventMapper.dispatchClick(view, x, y)
        }
    }

    override fun onScroll(x: Float, y: Float) {
        val deltaY = y - lastScrollY
        lastScrollY = y
        displayHost.dispatchTouch { view ->
            TouchEventMapper.dispatchScroll(view, x, y, deltaY)
        }
    }

    companion object {
        const val ACTION_SEARCH = "dev.personalaa.tube.ACTION_SEARCH"
        const val EXTRA_QUERY = "query"
    }
}

class TubeSearchScreen(
    carContext: CarContext,
    private val app: TubeApplication,
) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        return SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
            override fun onSearchTextChanged(searchText: String) {}

            override fun onSearchSubmitted(searchText: String) {
                val url = dev.personalaa.tube.youtube.YouTubeUrlParser.searchUrl(searchText)
                app.webViewController.loadUrl(url)
                screenManager.pop()
            }
        })
            .setHeaderAction(Action.BACK)
            .setSearchHint("YouTube を検索")
            .setItemList(
                ItemList.Builder()
                    .addItem(
                        Row.Builder()
                            .setTitle("ヒント")
                            .addText("動画 URL はスマホの YouTube 共有が便利です。")
                            .build(),
                    )
                    .build(),
            )
            .build()
    }
}
