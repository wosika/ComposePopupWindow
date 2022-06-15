package com.wosika.compose.popup;

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.*

@Composable
fun PopupWindow(content: @Composable () -> Unit) {
    val view = LocalView.current
    val parentComposition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val id = rememberSaveable { UUID.randomUUID() }

    val popupWindowLayout = remember {
        PopupWindowLayout(
            view,
            id
        ).apply {
            setContent(parentComposition) {
                currentContent()
            }
        }
    }

    DisposableEffect(popupWindowLayout) {
        popupWindowLayout.show()
        onDispose { popupWindowLayout.dismiss() }
    }
}

@SuppressLint("ViewConstructor")
private class PopupWindowLayout(
    private val composeView: View,
    uniqueId: UUID,
) : AbstractComposeView(composeView.context) {

    private val windowManager =
        composeView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val params = createLayoutParams()

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    init {
        id = android.R.id.content
        ViewTreeLifecycleOwner.set(this, ViewTreeLifecycleOwner.get(composeView))
        ViewTreeViewModelStoreOwner.set(this, ViewTreeViewModelStoreOwner.get(composeView))

        this.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        setTag(androidx.compose.ui.R.id.compose_view_saveable_id_tag, "CustomLayout:$uniqueId")
    }

    private var content: @Composable () -> Unit by mutableStateOf({})

    @Composable
    override fun Content() {
        content()
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
    }

    private fun createLayoutParams(): WindowManager.LayoutParams =
        WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
            token = composeView.applicationWindowToken
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        }


    fun show() {
        windowManager.addView(this, params)
    }

    fun dismiss() {
        disposeComposition()
        ViewTreeLifecycleOwner.set(this, null)
        windowManager.removeViewImmediate(this)
    }
}