package com.dreamgyf.android.ui.widget.swipe

import android.view.View

/**
 * @Author: dreamgyf
 * @Date: 2022/7/15
 */
abstract class SwipeLoadingAssistView {

    private var showing = false

    internal fun show() {
        if (!showing) {
            showing = true
            onShow()
        }
    }

    internal fun hide() {
        if (showing) {
            showing = false
            onHide()
        }
    }

    abstract fun getView(): View

    abstract fun onShow()

    abstract fun onHide()

    open fun onScroll(shownHeight: Int, totalHeight: Int) {}

}