package com.cafedroid.bingo_android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_game.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class GameActivity : AppCompatActivity() {

    private var mAdapter: GameTableAdapter? = null

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        initViews()
    }

    private fun initViews() {
        ActiveGameRoom.activeRoom =
            GameRoom("", "radom", "rachit", listOf("rachit"), GameState.READY.value)
        mAdapter = GameTableAdapter(this)
        rv_game_table.layoutManager = GridLayoutManager(this, 5)
        rv_game_table.adapter = mAdapter
        tv_stack_top.text = NUMBER_STACK.peek().toString()
        tv_stack_top.setOnClickListener {
            shuffleGameTable()
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun shuffleGameTable() {
        initNumberStack()
        val list = mAdapter?.mList
        list?.forEach {
            it.number = popNumberStack() ?: 0
        }
        list?.shuffle()
        list?.map {
            val index = list.indexOf(it)
            it.xPosition = index % 5
            it.yPosition = index / 5
        }
        mAdapter?.notifyDataSetChanged()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onTopChanged(event: ResponseEvent) {
        when (event.eventId?.toInt()) {
            100 -> tv_stack_top.text = if(NUMBER_STACK.size == 0) "" else NUMBER_STACK.peek().toString()
            NUMBER_SEND_EVENT -> {
                mAdapter?.notifyDataSetChanged()
            }
        }
    }
}
