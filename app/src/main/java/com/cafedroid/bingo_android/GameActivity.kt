package com.cafedroid.bingo_android

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_game.*
import kotlinx.android.synthetic.main.member_item.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class GameActivity : BaseActivity() {

    private var mAdapter: GameTableAdapter? = null
    private var eventAdapter: GameEventAdapter? = null

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
            tv_countdown_timer.visibility = View.VISIBLE

            object : CountDownTimer(STAGING_TIMER * 1000, 1000) {
                override fun onFinish() {
                    btn_shuffle.visibility = View.INVISIBLE
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
                    tv_countdown_timer.text =
                        String.format("00:${if (seconds < 0) "0" else ""}%s", seconds)
                }
            }.start()
        }
    }

    private fun initViews() {
        mAdapter = GameTableAdapter(this)
        eventAdapter = GameEventAdapter(this)
        rv_game_table.layoutManager = GridLayoutManager(this, 5)
        rv_game_event.layoutManager = LinearLayoutManager(this)
        rv_game_table.adapter = mAdapter
        rv_game_event.adapter = eventAdapter
        rv_game_event.visibility = View.INVISIBLE
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
        tv_button_reset.setOnClickListener {
            ActiveGameRoom.activeRoom = null
            startActivity(Intent(this, MainActivity::class.java))
        }

        tv_share_win.setOnClickListener {
            captureView(cl_game_view)?.let {
                shareImageUri(it)
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
                fl_stack_top_container.visibility = View.INVISIBLE
            } else {
                fl_stack_top_container.visibility = View.VISIBLE
                tv_stack_top.text = NUMBER_STACK.peek().toString()
            }
            is TurnUpdateEvent -> {
                if (isMyTurn().not()) mAdapter?.toggleTableLock(true)
                eventAdapter?.pushEvent(
                    GameEvent(
                        event.user,
                        "${if (event.user == USERNAME) "You" else event.user} pushed ${event.number}"
                    )
                )
                rv_game_event.scrollToPosition((eventAdapter?.itemCount ?: 0) - 1)
                mAdapter?.markDone(event.number, false)
                if (gameLocked.not()) setGameView()
            }
            is GameStateChangeEvent -> {
                if (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.IN_GAME) setGameView(
                    true
                )
            }
            is ButtonGlowEvent -> {
                tv_bingo_button.visibility = View.VISIBLE
                tv_bingo_button.isEnabled = true
                if (tv_bingo_button.isEnabled) buttonGlowTime = System.currentTimeMillis()
                vibrate(300)
            }
            is GameLockEvent -> {
                mAdapter?.toggleTableLock(true)
                gameLocked = true
            }
            is GameWinEvent -> {
                setWinView(event.user)
                if (event.user == USERNAME) {
                    lottie_result_view.setAnimation(R.raw.celebration)
                    lottie_result_view.playAnimation()
                    Toast.makeText(
                        this,
                        "Congratulations, You won the game!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    lottie_result_view.setAnimation(R.raw.bingo_lost)
                    lottie_result_view.playAnimation()
                    Toast.makeText(
                        this,
                        "${event.user} won the game! Better luck next time.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setWinView(winner: String) {
        tv_bingo_button.visibility = View.INVISIBLE
        rv_game_event.visibility = View.INVISIBLE
        tv_button_reset.visibility = View.VISIBLE
        tv_share_win.visibility = View.VISIBLE
        tv_display_name.text =
            String.format("%s won!", if (winner == USERNAME) "Your" else winner)
        Glide.with(this).load(ROBO_HASH_URL + winner).into(iv_display_bot)
    }

    private fun setGameView(stateChange: Boolean = false) {

        if (stateChange) {
            fl_turn_member_container.removeAllViews()
            val view = LayoutInflater.from(this)
                .inflate(R.layout.member_item, fl_turn_member_container, false)
            view.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            fl_turn_member_container.addView(view)
            tv_countdown_timer.visibility = View.INVISIBLE
            fl_turn_member_container.visibility = View.VISIBLE
            rv_game_event.visibility = View.VISIBLE
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
        }
    }

    private fun leaveRoom() {
        BingoSocket.socket?.let {
            it.emit(SocketAction.ACTION_LEAVE_ROOM, JSONObject().apply {
                put(ApiConstants.USER, USERNAME)
                put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
            })
        }
        ActiveGameRoom.activeRoom = null
        finish()
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

    override fun onBackPressed() {
        when (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState)) {
            GameState.IN_GAME -> {
                if (isMyTurn()) showToast("Do your turn and you may leave.")
                else showLeaveDialog()
            }
            GameState.READY -> {
                showLeaveDialog()
            }
            else -> super.onBackPressed()
        }
    }

    private fun showLeaveDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.leave)
            .setMessage(R.string.leave_message)
            .setPositiveButton("Leave") { _, _ ->
                leaveRoom()
                super.onBackPressed()
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
    }

    private fun captureView(view: View): Uri? {
        val image = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.RGB_565
        )
        //Draw the view inside the Bitmap
        view.draw(Canvas(image))

        //Store to sdcard
        return saveImage(image)
    }

    private fun saveImage(image: Bitmap): Uri? {
        val imagesFolder = File(cacheDir, "images")
        var uri: Uri? = null
        try {
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "IMG_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(this, packageName, file)
        } catch (e: IOException) {
            Log.d(
                this.javaClass.simpleName,
                "IOException while trying to write file for sharing: " + e.message
            )
        }
        return uri
    }

    private fun shareImageUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(
                Intent.EXTRA_TEXT,
                "Download and play now https://bingo.cafedroid.com/download"
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "image/png"
        }
        startActivity(intent)
    }
}
