package com.cafedroid.bingo_android

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_lobby.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class LobbyActivity : AppCompatActivity() {

    private var mAdapter: MemberListAdapter? = null

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
        btn_share.setOnClickListener { onToolbarItemSelected(it) }
        btn_more.setOnClickListener { onToolbarItemSelected(it) }
        mAdapter = MemberListAdapter(this)
        rv_member_list.layoutManager = LinearLayoutManager(this)
        rv_member_list.adapter = mAdapter
        refreshAdapter()
        if (isAdmin()) {
            btn_start.visibility = View.VISIBLE
        } else {
            btn_start.visibility = View.INVISIBLE
        }
        btn_start.setOnClickListener {
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
            R.id.btn_share -> {
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
            R.id.btn_more -> {
                val popupMenu = PopupMenu(this, btn_more)
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

    override fun onBackPressed() = showLeaveDialog()


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
}
