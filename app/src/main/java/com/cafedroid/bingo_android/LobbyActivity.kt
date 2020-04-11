package com.cafedroid.bingo_android

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_lobby.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class LobbyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

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
        if (isAdmin()) {
            btn_start.visibility = View.VISIBLE
        } else btn_start.visibility = View.INVISIBLE
        btn_start.setOnClickListener {
            BingoSocket.socket?.let {
                it.emit("start", JSONObject().apply {
                    put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
                    put(ApiConstants.USER, USERNAME)
                })
            }
        }
    }

    private fun startGameActivity() {
        startActivity(Intent(this, GameActivity::class.java))
        finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun handleSocketEvents(event: ResponseEvent) {
        initView()
        when (event) {
            is MemberUpdateEvent -> {
                Toast.makeText(applicationContext, event.message, Toast.LENGTH_SHORT).show()
            }
            is GameJoinEvent -> {

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.game_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share_btn -> {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Join my bingo room \"${ActiveGameRoom.activeRoom?.roomName}\" at:\n ${BASE_URL}/${ActiveGameRoom.activeRoom?.roomId}"
                    )
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Invite Friends"))
            }
            R.id.btn_leave -> leaveRoom()
        }
        return super.onOptionsItemSelected(item)
    }
}
