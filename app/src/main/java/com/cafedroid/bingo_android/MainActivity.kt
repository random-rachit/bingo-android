package com.cafedroid.bingo_android

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject


class MainActivity : BaseActivity() {

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
        cv_btn_create.setOnClickListener {
            et_room.visibility = View.VISIBLE
            ml_activity_main.transitionToEnd()
        }
        lottie_create_btn.setOnClickListener {
            lottie_create_btn.playAnimation()
            if (validateInput()) createRoom()
        }
    }

    private fun createRoom() {
        startActivityForResult(Intent(this, NameActivity::class.java), CREATE_ROOM_REQUEST)
    }

    private fun proceedToActiveRoom() {
        progressBar.visibility = View.INVISIBLE
        cv_btn_create.visibility = View.VISIBLE
        startActivity(Intent(this, LobbyActivity::class.java))
    }

    private fun validateInput(): Boolean = et_room.text.toString().isNotBlank()

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
        if (resultCode == Activity.RESULT_OK) {
            lottie_create_btn.isEnabled = false
            when (requestCode) {
                JOIN_ROOM_REQUEST -> {
                    cv_btn_create.visibility = View.INVISIBLE
                    progressBar.visibility = View.VISIBLE
                    val user = data?.extras?.getString(NameActivity.USER_KEY) ?: Build.MODEL
                    BingoSocket.socket?.let {
                        it.emit(SocketAction.ACTION_JOIN, JSONObject().apply {
                            put(ApiConstants.ID, gameId)
                            put(ApiConstants.USER, user)
                        })
                    }
                }
                CREATE_ROOM_REQUEST -> {
                    val user = data?.extras?.getString(NameActivity.USER_KEY) ?: Build.MODEL
                    BingoSocket.socket?.let {
                        it.emit(SocketAction.ACTION_CREATE, JSONObject().apply {
                            put(ApiConstants.NAME, et_room.text.toString().trim())
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
                lottie_create_btn.isEnabled = true
                lottie_create_btn.frame = 0
                gameId = null
            }
            is SocketErrorEvent -> {
                when (event.code) {
                    ErrorCode.GAME_EXPIRED -> showToast(event.message)
                    ErrorCode.MEMBER_LIMIT_REACHED -> showToast(event.message)
                    ErrorCode.USERNAME_TAKEN -> showToast(event.message)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (ml_activity_main.currentState == ml_activity_main.endState) {
            et_room.setText("")
            et_room.visibility = View.GONE
            ml_activity_main.transitionToStart()
        } else super.onBackPressed()
    }
}