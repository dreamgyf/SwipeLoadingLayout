package com.dreamgyf.android.ui.widget.swipe

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.animation.Interpolator
import android.widget.OverScroller
import androidx.core.animation.addListener
import androidx.core.view.*
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * @Author: dreamgyf
 * @Date: 2022/5/18
 */

private val sQuinticInterpolator = Interpolator { time ->
    var t = time
    t -= 1.0f
    t * t * t * t * t + 1.0f
}

class SwipeLoadingLayout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr),
        NestedScrollingParent, NestedScrollingParent2, NestedScrollingParent3,
        NestedScrollingChild, NestedScrollingChild2, NestedScrollingChild3 {

    /**
     * 记录触发滑动的类型
     * 用于处理手指松开后的惯性滑动
     */
    private var nestedScrollingType = -1

    /**
     * 顶部下拉刷新 Start
     */

    private val defaultSwipeLoadingRefreshView by lazy {
        DefaultSwipeLoadingRefreshView(context)
    }

    private var customSwipeLoadingRefreshView: SwipeLoadingAssistView? = null
        set(value) {
            removeView(refreshView.getView())
            field = value
            addView(refreshView.getView(), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

    private val refreshView: SwipeLoadingAssistView
        get() {
            return customSwipeLoadingRefreshView ?: defaultSwipeLoadingRefreshView
        }

    private val refreshLayoutHeight: Int
        get() = refreshView.getView().measuredHeight

    /**
     * 下拉刷新可超过刷新视图的距离
     * 因为下拉刷新应用了阻尼感，所以几乎不可能碰到这里的限制
     */
    private val refreshLayoutScrollOverDistance: Int
        get() = measuredHeight

    private val refreshLayoutTop: Int
        get() = paddingTop + refreshLayoutScrollOverDistance

    private var topUnconsumed: Double = 0.0

    private var refreshShownHeight = 0

    private var enableRefresh = true

    private var refreshing = false

    private var onRefreshScrollAnimStartListener: ((animator: Animator) -> Unit)? = null

    private var onRefreshScrollAnimEndListener: ((animator: Animator) -> Unit)? = null

    private val refreshScrollAnim by lazy {
        ValueAnimator().apply {
            duration = 300
            addUpdateListener {
                val y = it.animatedValue as Int
                scrollY = y
                val height = refreshLayoutScrollOverDistance + refreshLayoutHeight - y
                onRefreshLayoutScroll(min(height, refreshLayoutHeight))
            }
            addListener(onStart = {
                onRefreshScrollAnimStartListener?.invoke(it)
            }, onEnd = {
                onRefreshScrollAnimEndListener?.invoke(it)
            })
        }
    }

    /**
     * 顶部下拉刷新 End
     */

    /**
     * 实际展示的视图 Start
     */

    private var targetView: View? = null

    private val targetViewTop: Int
        get() = refreshLayoutTop + refreshLayoutHeight

    private val targetViewHeight: Int
        get() = targetView?.measuredHeight ?: 0

    /**
     * 实际展示的视图 End
     */

    /**
     * 底部上拉加载更多 Start
     */

    private val defaultSwipeLoadingLoadMoreView by lazy {
        DefaultSwipeLoadingLoadMoreView(context)
    }

    private var customSwipeLoadingLoadMoreView: SwipeLoadingAssistView? = null
        set(value) {
            removeView(loadMoreView.getView())
            field = value
            addView(loadMoreView.getView(), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

    private val loadMoreView: SwipeLoadingAssistView
        get() {
            return customSwipeLoadingLoadMoreView ?: defaultSwipeLoadingLoadMoreView
        }

    private val loadingLayoutHeight: Int
        get() = loadMoreView.getView().measuredHeight

    private val loadingLayoutTop: Int
        get() = targetViewTop + targetViewHeight

    /**
     * 上拉加载更多预留的触发距离
     */
    private val loadMoreReservedDistance = 100

    private var loadMoreConsumedHeight = 0

    private var loading = false

    private var enableLoadMore = true

    /**
     * 底部上拉加载更多 End
     */

    /**
     * 底部没有更多了 Start
     */

    private val defaultSwipeLoadingEndView by lazy {
        DefaultSwipeLoadingEndView(context)
    }

    private var customSwipeLoadingEndView: SwipeLoadingAssistView? = null
        set(value) {
            removeView(endView.getView())
            field = value
            addView(endView.getView(), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

    private val endView: SwipeLoadingAssistView
        get() {
            return customSwipeLoadingEndView ?: defaultSwipeLoadingEndView
        }

    private val endLayoutTop: Int
        get() = targetViewTop + targetViewHeight

    private val endLayoutHeight: Int
        get() = endView.getView().measuredHeight

    private var endConsumedHeight = 0

    private var showEnd = false

    /**
     * 底部没有更多了 End
     */

    private var onRefreshListener: (() -> Unit)? = null

    private var onLoadMoreListener: (() -> Unit)? = null

    private val nestedScrollingParentHelper = NestedScrollingParentHelper(this)

    private val nestedScrollingChildHelper = NestedScrollingChildHelper(this)

    private val onNestedScrollConsumedCompat = intArrayOf(0, 0)

    private val parentScrollConsumed = intArrayOf(0, 0)

    private val parentOffsetInWindow = intArrayOf(0, 0)

    fun setSwipeLoadingRefreshView(refreshView: SwipeLoadingAssistView) {
        customSwipeLoadingRefreshView = refreshView
    }

    fun setSwipeLoadingLoadMoreView(loadMoreView: SwipeLoadingAssistView) {
        customSwipeLoadingLoadMoreView = loadMoreView
    }

    fun setSwipeLoadingEndView(endView: SwipeLoadingAssistView) {
        customSwipeLoadingEndView = endView
    }

    init {
        isNestedScrollingEnabled = true
        addView(refreshView.getView(), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(loadMoreView.getView(), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        addView(endView.getView(), LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        loadMoreView.getView().visibility = View.GONE
        endView.getView().visibility = View.GONE

        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeLoadingLayout)

            enableRefresh =
                    typedArray.getBoolean(R.styleable.SwipeLoadingLayout_enableRefresh, true)
            enableLoadMore =
                    typedArray.getBoolean(R.styleable.SwipeLoadingLayout_enableLoadMore, true)
            showEnd =
                typedArray.getBoolean(R.styleable.SwipeLoadingLayout_showEnd, false)

            if (showEnd) {
                enableLoadMore = false
            }

            typedArray.recycle()

        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val targetView = getTargetView() ?: return

        val widthSpec = MeasureSpec.makeMeasureSpec(
                measuredWidth - paddingLeft - paddingRight,
                MeasureSpec.EXACTLY
        )
        refreshView.getView()
                .measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        targetView.measure(
                widthSpec,
                MeasureSpec.makeMeasureSpec(
                        measuredHeight - paddingTop - paddingBottom,
                        MeasureSpec.AT_MOST
                )
        )
        loadMoreView.getView()
                .measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
        endView.getView()
                .measure(widthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val targetView = getTargetView() ?: return
        val left = paddingLeft
        val width = measuredWidth - paddingLeft - paddingRight

        refreshView.getView().layout(
                left, refreshLayoutTop, left + width, targetViewTop
        )

        targetView.layout(
                left, targetViewTop, left + width, loadingLayoutTop
        )

        loadMoreView.getView().layout(
                left, loadingLayoutTop, left + width, loadingLayoutTop + loadingLayoutHeight
        )

        endView.getView().layout(
                left, endLayoutTop, left + width, endLayoutTop + endLayoutHeight
        )

        if (changed) {
            scrollY = targetViewTop
        }

        layoutComplete = true
        val iterator = afterLayoutActions.iterator()
        while (iterator.hasNext()) {
            iterator.next().run()
            iterator.remove()
        }
    }

    private var layoutComplete = false

    private val afterLayoutActions = mutableListOf<Runnable>()

    private fun runAfterLayout(action: Runnable) {
        if (layoutComplete) {
            action.run()
        } else {
            afterLayoutActions.add(action)
        }
    }

    private var scrollState = SCROLL_STATE_IDLE

    companion object {
        const val SCROLL_STATE_IDLE = 0
        const val SCROLL_STATE_DRAGGING = 1
        const val SCROLL_STATE_FLINGING = 2
    }

    private var lastTouchY = 0

    private var nestedOffsetY = 0

    private val touchOffsetInWindow = intArrayOf(0, 0)

    /**
     * 最大fling距离
     */
    private val maxFlingVelocity by lazy {
        ViewConfiguration.get(context).scaledMaximumFlingVelocity
    }

    /**
     * 最小fling距离
     */
    private val minFlingVelocity by lazy {
        ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }

    /**
     * 触摸的最小触发距离
     */
    private val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }

    private val velocityTracker by lazy {
        VelocityTracker.obtain()
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        if (action == MotionEvent.ACTION_DOWN) {
            if (scrollState == SCROLL_STATE_FLINGING) {
                stopFling()
            }
        }

        return super.onInterceptTouchEvent(event)
    }

    /**
     * 这里需要处理targetView没有处理滑动事件或用户的触摸位置处于targetView以外的位置这两种情况
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked

        if (!isEnabled) return false

        //这里的nestedOffsetY和MotionEvent都是为了计算手指fling的距离的
        if (action == MotionEvent.ACTION_DOWN) {
            nestedOffsetY = 0
        }
        val vtev = MotionEvent.obtain(event)
        vtev.offsetLocation(0f, nestedOffsetY.toFloat())

        var eventAddedToVelocityTracker = false

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = (event.y + 0.5f).toInt()
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
            }
            MotionEvent.ACTION_MOVE -> {
                val y = (event.y + 0.5f).toInt()
                // dy > 0 : 手指向上滑动
                // dy < 0 : 手指向下滑动
                var dy = lastTouchY - y

                //当滑动操作超过最小触发距离，开始处理
                if (scrollState == SCROLL_STATE_IDLE) {
                    dy = if (dy > 0) {
                        max(0, dy - touchSlop)
                    } else {
                        min(0, dy + touchSlop)
                    }
                    if (dy != 0) {
                        scrollState = SCROLL_STATE_DRAGGING
                    }
                }

                if (scrollState == SCROLL_STATE_DRAGGING) {
                    nestedScroll(dy, null, touchOffsetInWindow, ViewCompat.TYPE_TOUCH)
                    nestedOffsetY += touchOffsetInWindow[1]
                    lastTouchY = y - touchOffsetInWindow[1]
                }
            }
            MotionEvent.ACTION_UP -> {
                velocityTracker.addMovement(vtev)
                eventAddedToVelocityTracker = true
                velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity.toFloat())
                val yvel = -velocityTracker.yVelocity
                if (!(yvel != 0f && fling(yvel))) {
                    scrollState = SCROLL_STATE_IDLE
                }
                finishDragScroll()
            }
            MotionEvent.ACTION_CANCEL -> {
                scrollState = SCROLL_STATE_IDLE
                finishDragScroll()
            }
        }

        if (!eventAddedToVelocityTracker) {
            velocityTracker.addMovement(vtev)
        }
        vtev.recycle()

        return true
    }

    private fun finishDragScroll() {
        velocityTracker.clear()
        if (topUnconsumed > 0) {
            finishRefreshLayout(calculateRealRefreshDistance(topUnconsumed))
        }
        topUnconsumed = 0.0
        stopNestedScroll(ViewCompat.TYPE_TOUCH)
    }

    private val nestedConsumed = intArrayOf(0, 0)

    private val nestedOffsetInWindow = intArrayOf(0, 0)

    private fun nestedScroll(dy: Int, consumed: IntArray?, offsetInWindow: IntArray?, type: Int) {
        consumed?.set(0, 0)
        consumed?.set(1, 0)

        offsetInWindow?.set(0, 0)
        offsetInWindow?.set(1, 0)

        var dyUnconsumed = dy
        var dyConsumed = 0

        //下拉后出现刷新布局，在未松手的情况下上滑，此时需要改变刷新布局的位置（此为最优先）
        if (enableRefresh && !refreshing && dyUnconsumed > 0 && topUnconsumed > 0) {
            if (dyUnconsumed > topUnconsumed) {
                topUnconsumed = 0.0
                dyUnconsumed -= topUnconsumed.toInt()
                dyConsumed += topUnconsumed.toInt()
            } else {
                topUnconsumed -= dyUnconsumed
                dyUnconsumed = 0
                dyConsumed = dy
            }
            val overScrollTop = calculateRealRefreshDistance(topUnconsumed)
            refreshShownHeight = min(overScrollTop.toInt(), refreshLayoutHeight)
            moveRefreshLayout(overScrollTop)
        }

        //当动作为上滑时，检查是否需要触发加载更多
        if (enableLoadMore && !loading && dy > 0) {
            checkLoadMore()
        }

        nestedConsumed[0] = 0
        nestedConsumed[1] = 0
        nestedOffsetInWindow[0] = 0
        nestedOffsetInWindow[1] = 0
        //先让父View预处理嵌套滑动
        if (dispatchNestedPreScroll(
                        0, dyUnconsumed,
                        nestedConsumed, nestedOffsetInWindow,
                        type
                )) {
            dyUnconsumed -= nestedConsumed[1]
            dyConsumed += nestedConsumed[1]

            if (offsetInWindow != null) {
                offsetInWindow[1] += nestedOffsetInWindow[1]
            }
        }

        //处理自身内部的滑动
        if (dyUnconsumed > 0) {    //向上滚动
            //正在刷新时上滑，此时需要改变刷新布局的位置
            if (enableRefresh && refreshing
                    && !refreshScrollAnim.isStarted
                    && refreshShownHeight > 0) {
                if (dyUnconsumed > refreshShownHeight) {
                    refreshShownHeight = 0
                    moveRefreshLayout(0f)
                    dyUnconsumed -= refreshShownHeight
                    dyConsumed += refreshShownHeight
                } else {
                    refreshShownHeight -= dyUnconsumed
                    moveRefreshLayout(refreshShownHeight.toFloat())
                    dyUnconsumed = 0
                    dyConsumed = dy
                }
            }
        } else if (dyUnconsumed < 0) {    //向下滚动
            //当处于加载更多状态（并且能看见加载更多布局），并且动作为下拉时，需要改变加载更多布局的位置
            if (enableLoadMore && loading && loadMoreConsumedHeight > 0) {
                if (-dyUnconsumed > loadMoreConsumedHeight) {
                    loadMoreConsumedHeight = 0
                    moveLoadingLayout(0f)
                    dyUnconsumed -= -loadMoreConsumedHeight
                    dyConsumed += -loadMoreConsumedHeight
                } else {
                    loadMoreConsumedHeight += dyUnconsumed
                    moveLoadingLayout(loadMoreConsumedHeight.toFloat())
                    dyUnconsumed = 0
                    dyConsumed = dy
                }
            }
            //当处于没有更多了状态（并且能看见没有更多了布局），并且动作为下拉时，需要改变没有更多了布局的位置
            if (showEnd && endConsumedHeight > 0) {
                if (-dyUnconsumed > endConsumedHeight) {
                    endConsumedHeight = 0
                    moveEndLayout(0f)
                    dyUnconsumed -= -endConsumedHeight
                    dyConsumed += -endConsumedHeight
                } else {
                    endConsumedHeight += dyUnconsumed
                    moveEndLayout(endConsumedHeight.toFloat())
                    dyUnconsumed = 0
                    dyConsumed = dy
                }
            }
        }

        //处理targetView的滚动
        if (dyUnconsumed != 0) {
            val targetView = targetView
            if (targetView != null) {
                if (targetView is RecyclerView) {
                    val offset = targetView.computeVerticalScrollOffset()
                    val scrollDy = if (dyUnconsumed > 0) {
                        val extent = targetView.computeVerticalScrollExtent()
                        val range = targetView.computeVerticalScrollRange()
                        val residueY = range - (offset + extent)
                        min(residueY, dyUnconsumed)
                    } else {
                        max(-offset, dyUnconsumed)
                    }

                    targetView.scrollBy(0, scrollDy)
                    dyUnconsumed -= scrollDy
                    dyConsumed += scrollDy
                } else {
                    val unitScrollDy = if (dyUnconsumed > 0) 1 else -1
                    while (targetView.canScrollVertically(dyUnconsumed)
                            && dyUnconsumed != 0) {
                        targetView.scrollBy(0, unitScrollDy)
                        dyUnconsumed -= unitScrollDy
                        dyConsumed += unitScrollDy
                    }
                }
            }
        }

        //处理自身内部的滑动
        if (dyUnconsumed > 0) {    //向上滚动
            //当targetView已经滑动到底部并且触发加载更多后
            //如果还有剩余的距离没被消费，则消费（最多只会消费剩余未显示高度）并改变加载更多布局的位置
            if (enableLoadMore && loading) {
                val loadingCanConsumeY = loadingLayoutHeight - loadMoreConsumedHeight - (measuredHeight - targetViewHeight)
                if (loadingCanConsumeY > 0) {
                    val loadingRealConsumeY = min(loadingCanConsumeY, dyUnconsumed)
                    loadMoreConsumedHeight += loadingRealConsumeY
                    loadMoreConsumedHeight = min(loadMoreConsumedHeight, loadingLayoutHeight)
                    moveLoadingLayout(loadMoreConsumedHeight.toFloat())
                    dyUnconsumed -= loadingRealConsumeY
                    dyConsumed += loadingRealConsumeY
                }
            }
            //当targetView已经滑动到底部并且触发没有更多了后
            //如果还有剩余的距离没被消费，则消费（最多只会消费剩余未显示高度）并改变没有更多了布局的位置
            if (showEnd && dyUnconsumed > 0) {
                val endCanConsumeY = endLayoutHeight - endConsumedHeight - (measuredHeight - targetViewHeight)
                if (endCanConsumeY > 0) {
                    val endRealConsumeY = min(endCanConsumeY, dyUnconsumed)
                    endConsumedHeight += endRealConsumeY
                    endConsumedHeight = min(endConsumedHeight, endLayoutHeight)
                    moveEndLayout(endConsumedHeight.toFloat())
                    dyUnconsumed -= endRealConsumeY
                    dyConsumed += endRealConsumeY
                }
            }
        } else if (dyUnconsumed < 0) {    //向下滚动
            //当处于刷新状态并且targetView已经滑动到顶部后
            //如果还有剩余的距离没被消费，则消费并改变刷新布局的位置
            if (enableRefresh && refreshing && !refreshScrollAnim.isStarted && dyUnconsumed < 0) {
                val refreshCanConsumeY = refreshLayoutHeight - refreshShownHeight
                val refreshRealConsumeY = min(refreshCanConsumeY, -dyUnconsumed)
                refreshShownHeight += refreshRealConsumeY
                refreshShownHeight = min(refreshShownHeight, refreshLayoutHeight)
                moveRefreshLayout(refreshShownHeight.toFloat())
                dyUnconsumed -= -refreshRealConsumeY
                dyConsumed += -refreshRealConsumeY
            }
        }

        nestedConsumed[0] = 0
        nestedConsumed[1] = 0
        nestedOffsetInWindow[0] = 0
        nestedOffsetInWindow[1] = 0
        dispatchNestedScroll(0, dyConsumed,
                0, dyUnconsumed,
                nestedOffsetInWindow,
                type,
                nestedConsumed)

        dyUnconsumed -= nestedConsumed[1]
        dyConsumed += nestedConsumed[1]

        if (offsetInWindow != null) {
            offsetInWindow[1] += nestedOffsetInWindow[1]
        }

        //当targetView滑动到顶部，父View也滑动完后，还有剩余的距离没被消费
        //触发下拉刷新机制，显示并调整下拉刷新布局的位置
        if (enableRefresh && !refreshing && dyUnconsumed < 0 && type == ViewCompat.TYPE_TOUCH) {
            topUnconsumed += abs(dyUnconsumed)
            val overScrollTop = calculateRealRefreshDistance(topUnconsumed)
            refreshShownHeight = min(overScrollTop.toInt(), refreshLayoutHeight)
            moveRefreshLayout(overScrollTop)
            dyUnconsumed = 0
            dyConsumed = dy
        }

        if (consumed != null) {
            consumed[1] = dyConsumed
        }
    }

    private val viewFlinger by lazy {
        ViewFlinger()
    }

    private fun fling(velocityY: Float): Boolean {
        if (abs(velocityY) < minFlingVelocity) return false

        if (!dispatchNestedPreFling(0f, velocityY)) {
            dispatchNestedFling(0f, velocityY, true)

            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH)
            viewFlinger.fling(velocityY)

            return true
        }

        return false
    }

    private fun stopFling() {
        viewFlinger.stop()
    }

    private inner class ViewFlinger : Runnable {

        private var lastFlingY = 0

        private val overScroller = OverScroller(context, sQuinticInterpolator)

        private val consumed = intArrayOf(0, 0)

        fun fling(velocityY: Float) {
            lastFlingY = 0
            overScroller.fling(0, 0, 0, velocityY.toInt(),
                    Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
            scrollState = SCROLL_STATE_FLINGING
            postOnAnimation()
        }

        fun stop() {
            removeCallbacks(this)
            overScroller.abortAnimation()
            stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
            scrollState = SCROLL_STATE_IDLE
        }

        private fun postOnAnimation() {
            removeCallbacks(this)
            ViewCompat.postOnAnimation(this@SwipeLoadingLayout, this)
        }

        override fun run() {
            if (overScroller.computeScrollOffset()) {
                val y = overScroller.currY
                val dy = y - lastFlingY
                lastFlingY = y

                nestedScroll(dy, consumed, null, ViewCompat.TYPE_NON_TOUCH)

                val dyUnconsumed = dy - consumed[1]
                if (dyUnconsumed != 0) {
                    overScroller.abortAnimation()
                }

                if (overScroller.isFinished) {
                    stop()
                } else {
                    postOnAnimation()
                }
            } else {
                stop()
            }
        }
    }

    /******************************************************************************
     * NestedScrollingParent
     *****************************************************************************/

    /**
     * NestedScrollingParent1
     */
    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int): Boolean {
        return onStartNestedScroll(child, target, nestedScrollAxes, ViewCompat.TYPE_TOUCH)
    }

    /**
     * NestedScrollingParent2
     */
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        return isEnabled
                && (enableRefresh || enableLoadMore || showEnd)
                && (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

    /**
     * NestedScrollingParent1
     */
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        onNestedScrollAccepted(child, target, axes, ViewCompat.TYPE_TOUCH)
    }

    /**
     * NestedScrollingParent2
     */
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) {
        if (nestedScrollingType != -1 && type != nestedScrollingType && topUnconsumed > 0) {
            finishRefreshLayout(calculateRealRefreshDistance(topUnconsumed))
        }
        nestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes)
        startNestedScroll(axes and ViewCompat.SCROLL_AXIS_VERTICAL)
        topUnconsumed = 0.0
        nestedScrollingType = type
    }

    /**
     * NestedScrollingParent1
     */
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        onNestedPreScroll(target, dx, dy, consumed, ViewCompat.TYPE_TOUCH)
    }

    /**
     * NestedScrollingParent2
     * dy > 0 : 手指向上滑动
     * dy < 0 : 手指向下滑动
     */
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        //下拉后出现刷新布局，在未松手的情况下上滑，此时需要改变刷新布局的位置（此为最优先）
        if (enableRefresh && !refreshing && dy > 0 && topUnconsumed > 0) {
            if (dy > topUnconsumed) {
                consumed[1] = topUnconsumed.toInt()
                topUnconsumed = 0.0
            } else {
                consumed[1] = dy
                topUnconsumed -= dy
            }
            val overScrollTop = calculateRealRefreshDistance(topUnconsumed)
            refreshShownHeight = min(overScrollTop.toInt(), refreshLayoutHeight)
            moveRefreshLayout(overScrollTop)
        }

        //当动作为上滑时，检查是否需要触发加载更多
        if (enableLoadMore && !loading && dy > 0) {
            checkLoadMore()
        }

        //处理父布局的嵌套滚动事件
        if (dispatchNestedPreScroll(
                        dx - consumed[0], dy - consumed[1], parentScrollConsumed, null
                )) {
            consumed[0] += parentScrollConsumed[0]
            consumed[1] += parentScrollConsumed[1]
        }

        var unconsumedDy = dy - consumed[1]

        //正在刷新时上滑，此时需要改变刷新布局的位置
        if (enableRefresh && refreshing && !refreshScrollAnim.isStarted && unconsumedDy > 0 && refreshShownHeight > 0) {
            if (unconsumedDy > refreshShownHeight) {
                consumed[1] += refreshShownHeight
                refreshShownHeight = 0
                moveRefreshLayout(0f)
            } else {
                refreshShownHeight -= unconsumedDy
                moveRefreshLayout(refreshShownHeight.toFloat())
                consumed[1] += unconsumedDy
            }
        }

        unconsumedDy = dy - consumed[1]

        //当处于加载更多状态（并且能看见加载更多布局），并且动作为下拉时，需要改变加载更多布局的位置
        if (enableLoadMore && loading) {
            if (unconsumedDy < 0 && loadMoreConsumedHeight > 0) {
                if (-unconsumedDy > loadMoreConsumedHeight) {
                    consumed[1] += -loadMoreConsumedHeight
                    loadMoreConsumedHeight = 0
                    moveLoadingLayout(loadMoreConsumedHeight.toFloat())
                } else {
                    loadMoreConsumedHeight += unconsumedDy
                    moveLoadingLayout(loadMoreConsumedHeight.toFloat())
                    consumed[1] += unconsumedDy
                }
            }
        }

        unconsumedDy = dy - consumed[1]

        //当处于没有更多了状态（并且能看见没有更多了布局），并且动作为下拉时，需要改变没有更多了布局的位置
        if (showEnd) {
            if (unconsumedDy < 0 && endConsumedHeight > 0) {
                if (-unconsumedDy > endConsumedHeight) {
                    consumed[1] += -endConsumedHeight
                    endConsumedHeight = 0
                    moveEndLayout(endConsumedHeight.toFloat())
                } else {
                    endConsumedHeight += unconsumedDy
                    moveEndLayout(endConsumedHeight.toFloat())
                    consumed[1] += unconsumedDy
                }
            }
        }
    }

    /**
     * NestedScrollingParent1
     */
    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                dyUnconsumed: Int) {
        onNestedScroll(
                target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                ViewCompat.TYPE_TOUCH, onNestedScrollConsumedCompat
        )
    }

    /**
     * NestedScrollingParent2
     */
    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                dyUnconsumed: Int, type: Int) {
        onNestedScroll(
                target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                type, onNestedScrollConsumedCompat
        )
    }

    /**
     * NestedScrollingParent3
     * dy > 0 : 手指向上滑动
     * dy < 0 : 手指向下滑动
     * 此时targetView已处理完内部的滚动
     */
    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                dyUnconsumed: Int, type: Int, consumed: IntArray) {
        //当处于刷新状态并且targetView已经滑动到顶部后
        //如果还有剩余的距离没被消费，则消费并改变刷新布局的位置
        if (enableRefresh && refreshing && !refreshScrollAnim.isStarted && dyUnconsumed < 0) {
            val refreshCanConsumeY = refreshLayoutHeight - refreshShownHeight
            val refreshRealConsumeY = min(refreshCanConsumeY, -dyUnconsumed)
            refreshShownHeight += refreshRealConsumeY
            refreshShownHeight = min(refreshShownHeight, refreshLayoutHeight)
            moveRefreshLayout(refreshShownHeight.toFloat())
            consumed[1] += -refreshRealConsumeY
        }

        //当targetView已经滑动到底部并且触发加载更多后
        //如果还有剩余的距离没被消费，则消费（最多只会消费剩余未显示高度）并改变加载更多布局的位置
        if (enableLoadMore && loading && dyUnconsumed > 0) {
            val loadingCanConsumeY = loadingLayoutHeight - loadMoreConsumedHeight - (measuredHeight - targetViewHeight)
            if (loadingCanConsumeY > 0) {
                val loadingRealConsumeY = min(loadingCanConsumeY, dyUnconsumed)
                loadMoreConsumedHeight += loadingRealConsumeY
                loadMoreConsumedHeight = min(loadMoreConsumedHeight, loadingLayoutHeight)
                moveLoadingLayout(loadMoreConsumedHeight.toFloat())
                consumed[1] += loadingRealConsumeY
            }
        }

        //当targetView已经滑动到底部并且触发没有更多了后
        //如果还有剩余的距离没被消费，则消费（最多只会消费剩余未显示高度）并改变没有更多了布局的位置
        if (showEnd && dyUnconsumed > 0) {
            val endCanConsumeY = endLayoutHeight - endConsumedHeight - (measuredHeight - targetViewHeight)
            if (endCanConsumeY > 0) {
                val endRealConsumeY = min(endCanConsumeY, dyUnconsumed)
                endConsumedHeight += endRealConsumeY
                endConsumedHeight = min(endConsumedHeight, endLayoutHeight)
                moveEndLayout(endConsumedHeight.toFloat())
                consumed[1] += endRealConsumeY
            }
        }

        //处理父布局嵌套滚动
        dispatchNestedScroll(
                dxConsumed,
                dyConsumed + consumed[1],
                dxUnconsumed,
                dyUnconsumed - consumed[1],
                parentOffsetInWindow,
                ViewCompat.TYPE_TOUCH,
                consumed
        )

        val unconsumedAfterParents = dyUnconsumed - consumed[1]
        val remainingDistanceToScroll = if (unconsumedAfterParents == 0) {
            dyUnconsumed + parentOffsetInWindow[1]
        } else {
            unconsumedAfterParents
        }

        //当targetView滑动到顶部，父View也滑动完后，还有剩余的距离没被消费
        //触发下拉刷新机制，显示并调整下拉刷新布局的位置
        if (enableRefresh && !refreshing
                && type == ViewCompat.TYPE_TOUCH
                && remainingDistanceToScroll < 0) {
            topUnconsumed += abs(remainingDistanceToScroll)
            val overScrollTop = calculateRealRefreshDistance(topUnconsumed)
            refreshShownHeight = min(overScrollTop.toInt(), refreshLayoutHeight)
            moveRefreshLayout(overScrollTop)
            consumed[1] += unconsumedAfterParents
        }
    }

    /**
     * NestedScrollingParent1
     */
    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        return dispatchNestedPreFling(velocityX, velocityY)
    }

    /**
     * NestedScrollingParent1
     */
    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float,
                               consumed: Boolean): Boolean {
        return dispatchNestedFling(velocityX, velocityY, consumed)
    }

    /**
     * NestedScrollingParent1
     */
    override fun onStopNestedScroll(child: View) {
        onStopNestedScroll(child, ViewCompat.TYPE_TOUCH)
    }

    /**
     * NestedScrollingParent2
     */
    override fun onStopNestedScroll(target: View, type: Int) {
        if (nestedScrollingType != -1 && type != nestedScrollingType) {
            return
        }

        nestedScrollingParentHelper.onStopNestedScroll(target, type)

        if (topUnconsumed > 0) {
            finishRefreshLayout(calculateRealRefreshDistance(topUnconsumed))
        }
        topUnconsumed = 0.0
        nestedScrollingType = -1

        stopNestedScroll(ViewCompat.TYPE_TOUCH)
        stopNestedScroll(ViewCompat.TYPE_NON_TOUCH)
    }

    /******************************************************************************
     * NestedScrollingParent
     *****************************************************************************/

    /******************************************************************************
     * NestedScrollingChild
     *****************************************************************************/

    /**
     * NestedScrollingChild1
     */
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        nestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    /**
     * NestedScrollingChild1
     */
    override fun isNestedScrollingEnabled(): Boolean {
        return nestedScrollingChildHelper.isNestedScrollingEnabled
    }

    /**
     * NestedScrollingChild1
     */
    override fun startNestedScroll(axes: Int): Boolean {
        return nestedScrollingChildHelper.startNestedScroll(axes)
    }

    /**
     * NestedScrollingChild2
     */
    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return nestedScrollingChildHelper.startNestedScroll(axes, type)
    }

    /**
     * NestedScrollingChild1
     */
    override fun stopNestedScroll() {
        nestedScrollingChildHelper.stopNestedScroll()
    }

    /**
     * NestedScrollingChild2
     */
    override fun stopNestedScroll(type: Int) {
        nestedScrollingChildHelper.stopNestedScroll(type)
    }

    /**
     * NestedScrollingChild1
     */
    override fun hasNestedScrollingParent(): Boolean {
        return nestedScrollingChildHelper.hasNestedScrollingParent()
    }

    /**
     * NestedScrollingChild2
     */
    override fun hasNestedScrollingParent(type: Int): Boolean {
        return nestedScrollingChildHelper.hasNestedScrollingParent(type)
    }

    /**
     * NestedScrollingChild1
     */
    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?,
                                         offsetInWindow: IntArray?): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    /**
     * NestedScrollingChild2
     */
    override fun dispatchNestedPreScroll(dx: Int, dy: Int, consumed: IntArray?,
                                         offsetInWindow: IntArray?, type: Int): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow, type
        )
    }

    /**
     * NestedScrollingChild1
     */
    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                      dyUnconsumed: Int, offsetInWindow: IntArray?): Boolean {
        return nestedScrollingChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow
        )
    }

    /**
     * NestedScrollingChild2
     */
    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                      dyUnconsumed: Int, offsetInWindow: IntArray?,
                                      type: Int): Boolean {
        return nestedScrollingChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type
        )
    }

    /**
     * NestedScrollingChild3
     */
    override fun dispatchNestedScroll(dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int,
                                      dyUnconsumed: Int, offsetInWindow: IntArray?, type: Int,
                                      consumed: IntArray) {
        nestedScrollingChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed,
                offsetInWindow, type, consumed
        )
    }

    /**
     * NestedScrollingChild1
     */
    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    /**
     * NestedScrollingChild1
     */
    override fun dispatchNestedFling(velocityX: Float, velocityY: Float,
                                     consumed: Boolean): Boolean {
        return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    /******************************************************************************
     * NestedScrollingChild
     *****************************************************************************/

    /**
     * 计算真正顶部刷新布局需要露出的距离
     * 这里是做了一个滑动阻尼的效果
     */
    private fun calculateRealRefreshDistance(topUnconsumed: Double): Float {
        var result = 0.0
        var residueDistance = topUnconsumed
        var factor = 0.8

        while (residueDistance >= 10 && factor > 0.2) {
            result += 10 * factor
            residueDistance -= 10
            factor -= 0.01
        }

        if (residueDistance > 0) {
            result += residueDistance * factor
        }

        return result.toFloat()
    }

    /**
     * 显示顶部刷新布局
     */
    private fun moveRefreshLayout(overScrollTop: Float) {
        val totalScrollUpDistance: Float =
                refreshLayoutScrollOverDistance.toFloat() + refreshLayoutHeight
        val dy = min(overScrollTop, totalScrollUpDistance)
        scrollY = (totalScrollUpDistance - dy).toInt()

        if (dy > 0) {
            onRefreshLayoutShow()
        } else {
            onRefreshLayoutHide()
        }

        onRefreshLayoutScroll(min(dy.toInt(), refreshLayoutHeight))
    }

    /**
     * 处理下拉刷新松手后，触发刷新以及刷新布局的回弹效果
     */
    private fun finishRefreshLayout(overScrollTop: Float) {
        if (overScrollTop >= refreshLayoutHeight) {
            setRefreshing(true)
        } else {
            backToTargetTop()
        }
    }

    private fun moveLoadingLayout(overScrollBottom: Float) {
        val totalScrollDownDistance = loadingLayoutHeight.toFloat()
        val dy = min(overScrollBottom, totalScrollDownDistance)
        scrollY = (targetViewTop + dy).toInt()

        if (dy > 0) {
            onLoadingLayoutShow()
        } else {
            onLoadingLayoutHide()
        }

        val spareHeight = scrollY - targetViewTop + measuredHeight - targetViewHeight
        val height = max(0, min(spareHeight, loadingLayoutHeight))
        onLoadingLayoutScroll(height)
    }

    private fun moveEndLayout(overScrollBottom: Float) {
        val totalScrollDownDistance = endLayoutHeight.toFloat()
        val dy = min(overScrollBottom, totalScrollDownDistance)
        scrollY = (targetViewTop + dy).toInt()

        if (dy > 0) {
            onEndLayoutShow()
        } else {
            onEndLayoutHide()
        }

        val spareHeight = scrollY - targetViewTop + measuredHeight - targetViewHeight
        val height = max(0, min(spareHeight, endLayoutHeight))
        onEndLayoutScroll(height)
    }

    fun setOnRefreshListener(listener: () -> Unit) {
        onRefreshListener = listener
    }

    fun setOnLoadMoreListener(listener: () -> Unit) {
        onLoadMoreListener = listener
    }

    fun enableRefresh(enable: Boolean) {
        if (enableRefresh != enable) {
            enableRefresh = enable
            if (!enable) {
                setRefreshing(false)
            }
        }
    }

    fun setRefreshing(refreshing: Boolean) {
        if (this.refreshing != refreshing) {
            this.refreshing = refreshing
            if (refreshing) {
                getRefreshScrollAnim(
                        onStart = {
                            onRefreshLayoutShow()
                        },
                        onEnd = {
                            onRefreshListener?.invoke()
                        },
                        scrollY, refreshLayoutTop
                ).start()
            } else {
                backToTargetTop()
            }
        }
    }

    private fun backToTargetTop() {
        getRefreshScrollAnim(
                onStart = null,
                onEnd = {
                    onRefreshLayoutHide()
                    refreshShownHeight = 0
                },
                scrollY, targetViewTop
        ).start()
    }

    private fun checkLoadMore() {
        val target = targetView ?: return

        if (target is RecyclerView) {
            val offset = target.computeVerticalScrollOffset()
            //RecyclerView中整个列表滚动的距离超出了整个列表的高度
            //computeVerticalScrollExtent为当前RecyclerView显示的区域高度，offset即为列表已经滚动了的距离
            if (target.computeVerticalScrollExtent() + offset >=
                    target.computeVerticalScrollRange() - loadMoreReservedDistance) {
                setLoadingMore(true)
            }
        } else {
            if (!target.canScrollVertically(1)) {
                setLoadingMore(true)
            }
        }
    }

    fun enableLoadMore(enable: Boolean) {
        if (enableLoadMore != enable) {
            enableLoadMore = enable
            if (enable) {
                showEnd(false)
            } else {
                setLoadingMore(false)
            }
        }
    }

    fun setLoadingMore(loading: Boolean) {
        if (this.loading != loading) {
            this.loading = loading
            if (loading) {
                showEnd(false)
                loadMoreView.getView().visibility = View.VISIBLE

                runAfterLayout {
                    val spareHeight = scrollY - targetViewTop + measuredHeight - targetViewHeight
                    val height = max(0, min(spareHeight, loadingLayoutHeight))
                    if (height > 0) {
                        onLoadingLayoutShow()
                    }
                    onLoadingLayoutScroll(height)
                }
                onLoadMoreListener?.invoke()
            } else {
                loadMoreView.getView().visibility = View.GONE
                if (loadMoreConsumedHeight > 0) {
                    scrollY = targetViewTop
                    targetView?.scrollBy(0, loadMoreConsumedHeight)
                }
                loadMoreConsumedHeight = 0
                onLoadingLayoutHide()
                onLoadingLayoutScroll(0)
            }
        }
    }

    fun showEnd(show: Boolean) {
        if (this.showEnd != show) {
            this.showEnd = show

            if (show) {
                val loadingMore = loading

                enableLoadMore(false)
                endView.getView().visibility = View.VISIBLE

                if (loadingMore && targetView?.canScrollVertically(1) == false) {
                    val consumedY = max(0, endLayoutHeight - (measuredHeight - targetViewHeight))
                    scrollY = targetViewTop + consumedY
                    endConsumedHeight = consumedY
                }

                runAfterLayout {
                    val spareHeight = scrollY - targetViewTop + measuredHeight - targetViewHeight
                    val height = max(0, min(spareHeight, endLayoutHeight))
                    if (height > 0) {
                        onEndLayoutShow()
                    }
                    onEndLayoutScroll(height)
                }
            } else {
                endView.getView().visibility = View.GONE
                if (endConsumedHeight > 0) {
                    scrollY = targetViewTop
                    targetView?.scrollBy(0, endConsumedHeight)
                }
                endConsumedHeight = 0
                onEndLayoutHide()
                onEndLayoutScroll(0)
            }
        }
    }

    private fun getRefreshScrollAnim(
            onStart: ((animator: Animator) -> Unit)? = null,
            onEnd: (animator: Animator) -> Unit,
            vararg values: Int
    ): Animator {
        onRefreshScrollAnimStartListener = onStart
        onRefreshScrollAnimEndListener = onEnd
        return refreshScrollAnim.apply {
            setIntValues(*values)
        }
    }

    private fun onRefreshLayoutShow() {
        refreshView.show()
    }

    private fun onRefreshLayoutHide() {
        refreshView.hide()
    }

    private fun onRefreshLayoutScroll(height: Int) {
        refreshView.onScroll(height, refreshLayoutHeight)
    }

    private fun onLoadingLayoutShow() {
        loadMoreView.show()
    }

    private fun onLoadingLayoutHide() {
        loadMoreView.hide()
    }

    private fun onLoadingLayoutScroll(height: Int) {
        loadMoreView.onScroll(height, loadingLayoutHeight)
    }

    private fun onEndLayoutShow() {
        endView.show()
    }

    private fun onEndLayoutHide() {
        endView.hide()
    }

    private fun onEndLayoutScroll(height: Int) {
        endView.onScroll(height, endLayoutHeight)
    }

    private fun getTargetView(): View? {
        if (targetView == null) {
            val childCount = childCount
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child == refreshView.getView()
                        || child == loadMoreView.getView()
                        || child == endView.getView()) {
                    continue
                }
                targetView = child
                break
            }
        }

        return targetView
    }
}