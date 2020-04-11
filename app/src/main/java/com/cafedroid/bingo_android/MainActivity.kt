package com.cafedroid.bingo_android

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    companion object {
        const val CREATE_ROOM_REQUEST = 1000
        const val JOIN_ROOM_REQUEST = 1001
    }

    private var gameId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun initViews() {
        btn_create.setOnClickListener {
            if (validateInput()) createRoom()
        }
    }

    private fun createRoom() {

        startActivityForResult(Intent(this, NameActivity::class.java), CREATE_ROOM_REQUEST)
    }

    private fun proceedToActiveRoom() {
        startActivity(Intent(this, LobbyActivity::class.java))
    }

    private fun validateInput(): Boolean {
        return et_room.text.toString().isNotBlank() && et_user.text.toString().isNotBlank()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        intent.data?.lastPathSegment?.let {
            if (gameId.isNullOrBlank()) {
                gameId = it
                startActivityForResult(Intent(this, NameActivity::class.java), JOIN_ROOM_REQUEST)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            JOIN_ROOM_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    val user = data?.extras?.getString(NameActivity.USER_KEY) ?: Build.MODEL
                    BingoSocket.socket?.let {
                        it.emit(SocketAction.ACTION_JOIN, JSONObject().apply {
                            put(ApiConstants.ID, gameId)
                            put(ApiConstants.USER, user)
                        })
                    }
                }
            }
            CREATE_ROOM_REQUEST -> {
                if (resultCode == Activity.RESULT_OK) {
                    val user = data?.extras?.getString(NameActivity.USER_KEY) ?: Build.MODEL
                    BingoSocket.socket?.let {
                        it.emit(SocketAction.ACTION_CREATE, JSONObject().apply {
                            put(ApiConstants.NAME, et_room.text.toString())
                            put(ApiConstants.USER, user)
                        })
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleEvents(event: ResponseEvent) {
        when (event) {
            is GameJoinEvent -> {
                proceedToActiveRoom()
                gameId = null
            }
        }
    }
}
