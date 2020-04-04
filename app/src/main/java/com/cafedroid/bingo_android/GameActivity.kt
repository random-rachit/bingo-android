package com.cafedroid.bingo_android

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_game.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class GameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        initView()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }


    private fun initView() {
        tv_game_state.text =
            String.format("Awaiting %s to start the game", ActiveGameRoom.activeRoom?.roomAdmin)
        tv_users.text = "Members: ${ActiveGameRoom.activeRoom?.roomMembers}"

        btn_leave.setOnClickListener { leaveRoom() }
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun handleSocketEvents(event: ResponseEvent) {
        when (event) {
            is MemberUpdateEvent -> {
                Toast.makeText(applicationContext, event.message, Toast.LENGTH_SHORT).show()
                tv_users.text = "Members: ${ActiveGameRoom.activeRoom?.roomMembers}"
            }
            is GameJoinEvent -> {
                tv_users.text = "Members: ${ActiveGameRoom.activeRoom?.roomMembers}"
            }
        }
    }

    private fun leaveRoom() {
        BingoSocket.socket?.let {
            it.emit("leave", JSONObject().apply {
                put(ApiConstants.USER, USERNAME)
                put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
            })
        }
        ActiveGameRoom.activeRoom = null
        finish()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

}
