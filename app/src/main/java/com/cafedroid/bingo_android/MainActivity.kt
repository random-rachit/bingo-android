package com.cafedroid.bingo_android

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import com.cafedroid.bingo_android.databinding.ActivityMainBinding
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

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
    }

    private fun initViews() {
        binding.cvBtnCreate.setOnClickListener {
            binding.etRoom.visibility = View.VISIBLE
            binding.mlActivityMain.transitionToEnd()
        }
        binding.lottieCreateBtn.setOnClickListener {
            if (validateInput()) {
                createRoom()
                binding.lottieCreateBtn.playAnimation()
            } else showToast("Enter the room name ☝️")
        }
    }

    private fun createRoom() {
        startActivityForResult(Intent(this, NameActivity::class.java), CREATE_ROOM_REQUEST)
    }

    private fun proceedToActiveRoom() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.cvBtnCreate.visibility = View.VISIBLE
        startActivity(Intent(this, LobbyActivity::class.java))
    }

    private fun validateInput(): Boolean = binding.etRoom.text.toString().isNotBlank()

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
            binding.lottieCreateBtn.isEnabled = false
            when (requestCode) {
                JOIN_ROOM_REQUEST -> {
                    binding.cvBtnCreate.visibility = View.INVISIBLE
                    binding.progressBar.visibility = View.VISIBLE
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
                            put(ApiConstants.NAME, binding.etRoom.text.toString().trim())
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
                binding.lottieCreateBtn.isEnabled = true
                binding.lottieCreateBtn.frame = 0
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
        if (binding.mlActivityMain.currentState == binding.mlActivityMain.endState) {
            binding.lottieCreateBtn.pauseAnimation()
            binding.lottieCreateBtn.frame = 0
            binding.etRoom.setText("")
            binding.etRoom.visibility = View.GONE
            binding.mlActivityMain.transitionToStart()
        } else super.onBackPressed()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (binding.mlActivityMain.currentState == binding.mlActivityMain.endState) {
            binding.lottieCreateBtn.pauseAnimation()
            binding.lottieCreateBtn.frame = 0
            binding.etRoom.setText("")
            binding.etRoom.visibility = View.GONE
            binding.mlActivityMain.transitionToStart()
        }
        intent?.data?.lastPathSegment?.let {
            if (gameId.isNullOrBlank()) {
                gameId = it
                startActivityForResult(Intent(this, NameActivity::class.java), JOIN_ROOM_REQUEST)
            }
        }
    }
}