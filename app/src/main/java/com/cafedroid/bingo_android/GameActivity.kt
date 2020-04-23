package com.cafedroid.bingo_android

import android.content.Context
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.member_item.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class GameActivity : AppCompatActivity() {

    private var mAdapter: GameTableAdapter? = null

    companion object {
        private const val STAGING_TIMER: Long = 30
    }

    private var buttonGlowTime = 0L
    private var gameLocked = false
    private var sent = false

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
                    val seconds = millisUntilFinished / 1000
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
        tv_bingo_button.isEnabled = false
        tv_bingo_button.setOnClickListener {
            val timeTakenToTap = System.currentTimeMillis() - buttonGlowTime
            if (sent.not()) {
                BingoSocket.socket?.let {
                    it.emit(SocketAction.ACTION_WIN, JSONObject().apply {
                        put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
                        put(ApiConstants.USER, USERNAME)
                        put(ApiConstants.TIME_TAKEN, timeTakenToTap)
                    })
                }
                sent = true
            }
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
                tv_stack_top.visibility = View.INVISIBLE
            } else {
                tv_stack_top.visibility = View.VISIBLE
                tv_stack_top.text = NUMBER_STACK.peek().toString()
            }
            is TurnUpdateEvent -> {
                val isActiveUser =
                    ActiveGameRoom.activeRoom?.roomMembers?.get(event.turn) == USERNAME
                if (isActiveUser.not()) mAdapter?.toggleTableLock(true)
                Toast.makeText(
                    this,
                    "${event.user} pushed ${event.number}",
                    Toast.LENGTH_SHORT
                ).show()
                mAdapter?.markDone(event.number, false)
                if (gameLocked.not()) setGameView()
            }
            is GameStateChangeEvent -> {
                if (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.IN_GAME) setGameView(
                    true
                )
            }
            is ButtonGlowEvent -> {
                tv_bingo_button.isEnabled = true
                if (bingo_button.isEnabled) buttonGlowTime = System.currentTimeMillis()
                vibrate(300)
            }
            is GameLockEvent -> {
                mAdapter?.toggleTableLock(true)
                gameLocked = true
            }
            is GameWinEvent -> {
                if (event.user == USERNAME) {
                    Toast.makeText(
                        this,
                        "Congratulations, You have won the game!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "${event.user} won the game! Better luck next time.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setGameView(stateChange: Boolean = false) {

        if (stateChange) {
            fl_turn_member_container.removeAllViews()
            fl_turn_member_container.addView(
                LayoutInflater.from(this)
                    .inflate(R.layout.member_item, fl_turn_member_container, false)
            )
            tv_countdown_timer.visibility = View.INVISIBLE
            fl_turn_member_container.visibility = View.VISIBLE
            bingo_button.visibility = View.VISIBLE
        }
        val activeRoom = ActiveGameRoom.activeRoom
        activeRoom?.let {
            val activeUser =
                it.roomMembers[activeRoom.userTurn]
            mAdapter?.toggleTableLock(
                (activeRoom.roomMembers[activeRoom.userTurn] == USERNAME).not()
            )
            cl_member_item_root.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    if (activeUser == USERNAME) R.color.green else android.R.color.white
                )
            )
            tv_display_name.text =
                String.format("%s turn", if (activeUser == USERNAME) "Your" else "$activeUser\'s")
            Glide.with(this).load(ROBO_HASH_URL + activeUser).into(iv_display_bot)
            tv_countdown_label.text = getString(R.string.in_game_status)
        }
    }

    private fun vibrate(millis: Long) {
        val vibrationManager = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            vibrationManager.vibrate(millis)
        } else
            vibrationManager.vibrate(
                VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
            )
    }
}
