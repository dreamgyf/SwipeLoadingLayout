package com.dreamgyf.android.ui.widget.swipe

import android.animation.ValueAnimator
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.dreamgyf.android.utils.ui.dp2px
import com.dreamgyf.android.utils.ui.dp2pxF

/**
 * @Author: dreamgyf
 * @Date: 2022/7/15
 */
private const val IV_LOAD_MORE_ID = 8888

class DefaultSwipeLoadingLoadMoreView(private val context: Context) : SwipeLoadingAssistView() {

    private val view by lazy {
        FrameLayout(context).apply {
            val loadMoreImageView = ImageView(context)
            val loadMoreImageViewLP = FrameLayout.LayoutParams(20.dp2px(context), 20.dp2px(context))
            loadMoreImageViewLP.gravity = Gravity.CENTER
            addView(loadMoreImageView, loadMoreImageViewLP)
            loadMoreImageView.setImageResource(R.drawable.ic_loading_20px_black)
            loadMoreImageView.pivotX = 10.dp2pxF(context)
            loadMoreImageView.pivotY = 10.dp2pxF(context)
            loadMoreImageView.id = IV_LOAD_MORE_ID
            _ivLoadMore = loadMoreImageView
            setPadding(0, 10.dp2px(context), 0, 16.dp2px(context))
        }
    }

    private var _ivLoadMore: ImageView? = null

    private val ivLoadMore: ImageView
        get() {
            return _ivLoadMore ?: view.findViewById(IV_LOAD_MORE_ID)
        }

    private val anim by lazy {
        ValueAnimator.ofInt(0, 8).apply {
            duration = 800
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val phase = it.animatedValue as Int
                val angle = 360 / 8 * phase
                ivLoadMore.rotation = angle.toFloat()
            }
        }
    }

    override fun getView(): View {
        return view
    }

    override fun onShow() {
        if (!anim.isStarted) {
            anim.start()
        }
    }

    override fun onHide() {
        anim.cancel()
    }

}