package com.cafedroid.bingo_android

import android.graphics.Typeface
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
import org.json.JSONObject

class GameActivity : AppCompatActivity() {

    private var mAdapter: GameTableAdapter? = null

    companion object {
        private const val STAGING_TIMER: Long = 30
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        initViews()
        startCountDown()
    }

    private fun startCountDown() {
        if (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.READY) {
            tv_countdown_label.visibility = View.VISIBLE
            tv_countdown_timer.visibility = View.VISIBLE

            object : CountDownTimer(STAGING_TIMER * 1000, 1000) {
                override fun onFinish() {
                    btn_shuffle.visibility = View.GONE
                    if (isAdmin()) {
                        BingoSocket.socket?.emit(SocketAction.ACTION_ADMIN, JSONObject().apply {
                            put("action", AdminAction.LAUNCH_GAME.value)
                            put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
                            put(ApiConstants.USER, USERNAME)
                        })
                    }
                    Toast.makeText(this@GameActivity, "Game will start now.", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onTick(millisUntilFinished: Long) {
                    val seconds = millisUntilFinished/1000
                    if (seconds == 3.toLong()) {
                        tv_countdown_timer.setTextColor(
                            ContextCompat.getColor(this@GameActivity, android.R.color.holo_red_dark)
                        )
                        mAdapter?.toggleTableLock(true)
                    }
                    tv_countdown_timer.text = String.format("%s seconds", seconds)
                }
            }.start()
        }
    }

    private fun initViews() {
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
        if (mAdapter?.isTableLocked == true || GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.IN_GAME) {
            Toast.makeText(this, "Table is locked. Cannot shuffle now.", Toast.LENGTH_SHORT).show()
            return
        }
        initNumberStack()
        val list = mAdapter?.mList
        list?.forEach {
            it.number = popNumberStack() ?: 0
        }
        list?.shuffle()
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
            is TurnUpdateEvent -> {
                val hasWon = event.hasWon
                val isActiveUser =
                    ActiveGameRoom.activeRoom?.roomMembers?.get(event.turn) == USERNAME
                if (hasWon || isActiveUser.not()) mAdapter?.toggleTableLock(true)
                Toast.makeText(
                    this,
                    "${event.user} pushed ${event.number} ${if (hasWon) "and won the game" else ""}",
                    Toast.LENGTH_SHORT
                ).show()
                mAdapter?.markDone(event.number, false)
                setGameView()
            }
            is GameStateChangeEvent -> {
                if (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.IN_GAME) setGameView()
            }
        }
    }

    private fun setGameView() {
        mAdapter?.toggleTableLock(
            (ActiveGameRoom.activeRoom?.roomMembers?.get(
                ActiveGameRoom.activeRoom?.userTurn ?: 0
            ) == USERNAME).not()
        )
        tv_countdown_label.text = getString(R.string.in_game_status)
        val activeUser =
            ActiveGameRoom.activeRoom?.roomMembers?.get(ActiveGameRoom.activeRoom?.userTurn ?: 0)
        tv_countdown_timer.typeface =
            if (activeUser == USERNAME) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        tv_countdown_timer.text =
            String.format("%s turn", if (activeUser == USERNAME) "Your" else "$activeUser\'s")
    }

}
