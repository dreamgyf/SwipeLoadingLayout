package com.dreamgyf.android.ui.widget.swipe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dreamgyf.android.ui.rv.SpaceItemDecoration
import com.dreamgyf.android.utils.ui.dp2px
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var swipeLoadingLayout: SwipeLoadingLayout

    private lateinit var recyclerView: RecyclerView

    private val adapter = RvAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeLoadingLayout = findViewById(R.id.swipe_loading_layout)
        recyclerView = findViewById(R.id.recycler_view)

        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.addItemDecoration(SpaceItemDecoration(16.dp2px(this), 16.dp2px(this)))

        swipeLoadingLayout.setOnRefreshListener {
            page = 1
            swipeLoadingLayout.enableLoadMore(true)
            loadData()
        }
        swipeLoadingLayout.setOnLoadMoreListener {
            loadData()
        }

        loadData()
    }

    private var page = 1

    private val pageSize = 15

    private fun loadData() {
        mockData(page, pageSize, onSuccess = { dataList ->
            swipeLoadingLayout.setRefreshing(false)

            if (page == 1) {
                adapter.setData(dataList)
            } else {
                adapter.addData(dataList)
            }
            page++

            if (dataList.size < pageSize) {
                swipeLoadingLayout.showEnd(true)
            } else {
                swipeLoadingLayout.setLoadingMore(false)
            }
        })
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun mockData(page: Int, pageSize: Int, onSuccess: (List<Int>) -> Unit) {
        GlobalScope.launch {
            delay(1500)
            val dataList = mutableListOf<Int>()
            if (page <= 3) {
                for (i in (page - 1) * pageSize + 1..page * pageSize) {
                    dataList.add(i)
                }
            }
            mainHandler.post {
                onSuccess(dataList)
            }
        }
    }
}