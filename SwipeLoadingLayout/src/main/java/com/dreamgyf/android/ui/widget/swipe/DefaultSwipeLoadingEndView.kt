package com.dreamgyf.android.ui.widget.swipe

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.dreamgyf.android.utils.ui.dp2px

/**
 * @Author: dreamgyf
 * @Date: 2022/7/15
 */
class DefaultSwipeLoadingEndView(private val context: Context) : SwipeLoadingAssistView() {

    private val view by lazy {
        FrameLayout(context).apply {
            val tvEnd = TextView(context)
            tvEnd.setTextColor(Color.parseColor("#B3B3B3"))
            tvEnd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            tvEnd.text = "没有更多啦~"
            val tvEndLP = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            tvEndLP.gravity = Gravity.CENTER
            addView(tvEnd, tvEndLP)
            setPadding(0, 10.dp2px(context), 0, 16.dp2px(context))
        }
    }

    override fun getView(): View {
        return view
    }

    override fun onShow() {}

    override fun onHide() {}

}