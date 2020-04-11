package com.cafedroid.bingo_android

import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        startCountDown(30)
    }

    private fun startCountDown(time: Long) {
        if (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.READY) {
            tv_countdown_label.visibility = View.VISIBLE
            tv_countdown_timer.visibility = View.VISIBLE

            object : CountDownTimer(time * 1000, 1000) {
                override fun onFinish() {
                    Toast.makeText(this@GameActivity, "Game will start", Toast.LENGTH_SHORT).show()
                }

                override fun onTick(millisUntilFinished: Long) {
                    val seconds = millisUntilFinished/1000
                    if (seconds == 3.toLong()) {
                        tv_countdown_timer.setTextColor(
                            ContextCompat.getColor(this@GameActivity, android.R.color.holo_red_dark)
                        )
                        mAdapter?.lockGameTable(true)
                    }
                    tv_countdown_timer.text = "$seconds seconds"
                }
            }.start()
        }
    }

    private fun initViews() {
        ActiveGameRoom.activeRoom =
            GameRoom("", "radom", "rachit", listOf("rachit"), GameState.READY.value, 0)
        mAdapter = GameTableAdapter(this)
        rv_game_table.layoutManager = GridLayoutManager(this, 5)
        rv_game_table.adapter = mAdapter
        tv_stack_top.text = NUMBER_STACK.peek().toString()
        btn_shuffle.setOnClickListener {
            shuffleGameTable()
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun shuffleGameTable() {
        if (mAdapter?.isTableLocked == true) {
            Toast.makeText(this, "Table is locked. Cannot shuffle now.", Toast.LENGTH_SHORT).show()
            return
        }
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
    fun onGameEvent(event: ResponseEvent) {
        when (event) {
            is NumberStackChangeEvent -> if (NUMBER_STACK.empty()) {
                tv_stack_top.visibility =
                    View.INVISIBLE
            } else {
                tv_stack_top.visibility = View.VISIBLE
                tv_stack_top.text = NUMBER_STACK.peek().toString()
            }
            is BingoNumberUpdateEvent -> {
                val hasWon = event.hasWon
                if (hasWon) mAdapter?.lockGameTable(true)
                Toast.makeText(
                    this,
                    "${event.user} pushed ${event.number} ${if (hasWon) "and won the game" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
                mAdapter?.markDone(event.number)
            }
        }
    }
}
