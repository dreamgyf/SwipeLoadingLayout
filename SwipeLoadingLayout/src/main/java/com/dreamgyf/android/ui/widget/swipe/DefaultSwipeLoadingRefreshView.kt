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
private const val IV_REFRESH_ID = 8888

class DefaultSwipeLoadingRefreshView(private val context: Context) : SwipeLoadingAssistView() {

    private val view by lazy {
        FrameLayout(context).apply {
            val refreshImageView = ImageView(context)
            val refreshImageViewLP = FrameLayout.LayoutParams(20.dp2px(context), 20.dp2px(context))
            refreshImageViewLP.gravity = Gravity.CENTER
            addView(refreshImageView, refreshImageViewLP)
            refreshImageView.setImageResource(R.drawable.ic_loading_20px_black)
            refreshImageView.pivotX = 10.dp2pxF(context)
            refreshImageView.pivotY = 10.dp2pxF(context)
            refreshImageView.id = IV_REFRESH_ID
            _ivRefresh = refreshImageView
            setPadding(0, 20.dp2px(context), 0, 10.dp2px(context))
        }
    }

    private var _ivRefresh: ImageView? = null

    private val ivRefresh: ImageView
        get() {
            return _ivRefresh ?: view.findViewById(IV_REFRESH_ID)
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
                ivRefresh.rotation = angle.toFloat()
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