package com.cafedroid.bingo_android

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.cafedroid.bingo_android.databinding.ActivityLobbyBinding
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class LobbyActivity : AppCompatActivity() {

    private var mAdapter: MemberListAdapter? = null

    private lateinit var binding: ActivityLobbyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLobbyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        setupBackPress()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object: OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showLeaveDialog()
            }
        })
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }


    private fun initView() {
        binding.tvGameState.text =
            String.format("Awaiting %s to start the game", ActiveGameRoom.activeRoom?.roomAdmin)
        binding.btnShare.setOnClickListener { onToolbarItemSelected(it) }
        binding.btnMore.setOnClickListener { onToolbarItemSelected(it) }
        mAdapter = MemberListAdapter(this)
        binding.rvMemberList.layoutManager = LinearLayoutManager(this)
        binding.rvMemberList.adapter = mAdapter
        refreshAdapter()
        if (isAdmin()) {
            binding.btnStart.visibility = View.VISIBLE
        } else {
            binding.btnStart.visibility = View.INVISIBLE
        }
        binding.btnStart.setOnClickListener {
            BingoSocket.socket?.let {
                it.emit(SocketAction.ACTION_START, JSONObject().apply {
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
        refreshAdapter()
        when (event) {
            is MemberUpdateEvent -> {
                Toast.makeText(applicationContext, event.message, Toast.LENGTH_SHORT).show()
            }

            is GameStateChangeEvent -> {
                if (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.READY)
                    startGameActivity()
            }
        }
    }

    private fun refreshAdapter() {
        mAdapter?.setMemberList(ActiveGameRoom.activeRoom?.roomMembers ?: emptyList())
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

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.lobby_menu, menu)
        return true
    }

    private fun onToolbarItemSelected(item: View) {
        when (item.id) {
            binding.btnShare.id -> {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Join my bingo room \"${ActiveGameRoom.activeRoom?.roomName}\" at:\n ${WEB_URL}/${ActiveGameRoom.activeRoom?.roomId}"
                    )
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Invite up to 5 friends"))
            }

            binding.btnMore.id -> {
                val popupMenu = PopupMenu(this, binding.btnMore)
                menuInflater.inflate(R.menu.lobby_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.btn_leave -> leaveRoom()
                    }
                    true
                }
                popupMenu.show()
            }
        }
    }

    private fun showLeaveDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.leave)
            .setMessage(R.string.leave_message)
            .setPositiveButton("Leave") { _, _ ->
                leaveRoom()
                finish()
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
    }
}
