package com.cafedroid.bingo_android.lobby

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cafedroid.bingo_android.ActiveGameRoom
import com.cafedroid.bingo_android.ApiConstants
import com.cafedroid.bingo_android.BingoSocket
import com.cafedroid.bingo_android.GameState
import com.cafedroid.bingo_android.GameStateChangeEvent
import com.cafedroid.bingo_android.MemberListAdapter
import com.cafedroid.bingo_android.MemberUpdateEvent
import com.cafedroid.bingo_android.R
import com.cafedroid.bingo_android.ResponseEvent
import com.cafedroid.bingo_android.SocketAction
import com.cafedroid.bingo_android.USERNAME
import com.cafedroid.bingo_android.WEB_URL
import com.cafedroid.bingo_android.databinding.FragmentLobbyBinding
import com.cafedroid.bingo_android.isAdmin
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class LobbyFragment : Fragment() {

    private lateinit var binding: FragmentLobbyBinding

    private var mAdapter: MemberListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLobbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setupBackPress()
    }


    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
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
        binding.btnShare.setOnClickListener { inviteFriends() }
        binding.btnLeave.setOnClickListener { leaveRoom() }
        mAdapter = MemberListAdapter(requireContext())
        binding.rvMemberList.layoutManager = LinearLayoutManager(requireContext())
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
        val action = LobbyFragmentDirections.lobbyToGame()
        findNavController().navigate(action)
//        startActivity(Intent(requireContext(), GameActivity::class.java))
//        requireActivity().finish()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun handleSocketEvents(event: ResponseEvent) {
        refreshAdapter()
        when (event) {
            is MemberUpdateEvent -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
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
        findNavController().popBackStack()
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun inviteFriends() {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "Join my bingo room \"${ActiveGameRoom.activeRoom?.roomName}\" at:\n $WEB_URL/${ActiveGameRoom.activeRoom?.roomId}"
            )
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Invite up to 5 friends"))
    }

    private fun showLeaveDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave)
            .setMessage(R.string.leave_message)
            .setPositiveButton("Leave") { _, _ ->
                leaveRoom()
                requireActivity().finish()
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
    }

}