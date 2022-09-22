package com.dreamgyf.android.ui.widget.swipe

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * @Author: dreamgyf
 * @Date: 2022/8/30
 */
class RvAdapter : RecyclerView.Adapter<RvAdapter.ViewHolder>() {

    class ViewHolder(itemView: View, val textView: TextView) : RecyclerView.ViewHolder(itemView)

    private val dataList = mutableListOf<Int>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val frameLayout = FrameLayout(parent.context)
        frameLayout.background = GradientDrawable().apply {
            color = ColorStateList.valueOf(Color.GRAY)
            cornerRadius = 50f
        }
        frameLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            300
        )
        val textView = TextView(parent.context)
        textView.setTextColor(Color.WHITE)
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.gravity = Gravity.CENTER
        frameLayout.addView(textView, lp)
        return ViewHolder(frameLayout, textView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = dataList[position].toString()
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<Int>) {
        dataList.clear()
        dataList.addAll(data)
        notifyDataSetChanged()
    }

    fun addData(data: List<Int>) {
        val start = dataList.size
        dataList.addAll(data)
        notifyItemRangeInserted(start, data.size)
    }
}