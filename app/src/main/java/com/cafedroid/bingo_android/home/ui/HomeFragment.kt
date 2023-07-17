package com.cafedroid.bingo_android.home.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cafedroid.bingo_android.ApiConstants
import com.cafedroid.bingo_android.BingoSocket
import com.cafedroid.bingo_android.ErrorCode
import com.cafedroid.bingo_android.GameJoinEvent
import com.cafedroid.bingo_android.NameActivity
import com.cafedroid.bingo_android.ResponseEvent
import com.cafedroid.bingo_android.SocketAction
import com.cafedroid.bingo_android.SocketErrorEvent
import com.cafedroid.bingo_android.databinding.FragmentHomeBinding
import com.cafedroid.bingo_android.showToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject


class HomeFragment : Fragment() {

    companion object {
        const val CREATE_ROOM_REQUEST = 1000
        const val JOIN_ROOM_REQUEST = 1001
    }

    private var gameId: String? = null

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun backPressed() {
        if (binding.mlActivityMain.currentState == binding.mlActivityMain.endState) {
            binding.lottieCreateBtn.pauseAnimation()
            binding.lottieCreateBtn.frame = 0
            binding.etRoom.setText("")
            binding.etRoom.visibility = View.GONE
            binding.mlActivityMain.transitionToStart()
        } else requireActivity().finish()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressed()
                }
            })
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
            } else requireContext().showToast("Enter the room name ☝️")
        }
    }

    private fun createRoom() {
        startActivityForResult(
            Intent(requireContext(), NameActivity::class.java),
            CREATE_ROOM_REQUEST
        )
    }

    private fun proceedToLobby() {
        binding.progressBar.visibility = View.INVISIBLE
        binding.cvBtnCreate.visibility = View.VISIBLE
        val action = HomeFragmentDirections.homeToLobby()
        findNavController().navigate(action)
    }

    private fun validateInput(): Boolean = binding.etRoom.text.toString().isNotBlank()

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
        requireActivity().intent.data?.lastPathSegment?.let {
            if (gameId.isNullOrBlank()) {
                gameId = it
                startActivityForResult(
                    Intent(requireContext(), NameActivity::class.java),
                    JOIN_ROOM_REQUEST
                )
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
                    //By passing socket emission for UI testing
                    proceedToLobby()

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
                proceedToLobby()
                binding.lottieCreateBtn.isEnabled = true
                binding.lottieCreateBtn.frame = 0
                gameId = null
            }

            is SocketErrorEvent -> {
                when (event.code) {
                    ErrorCode.GAME_EXPIRED -> requireContext().showToast(event.message)
                    ErrorCode.MEMBER_LIMIT_REACHED -> requireContext().showToast(event.message)
                    ErrorCode.USERNAME_TAKEN -> requireContext().showToast(event.message)
                }
            }
        }
    }

//    TODO: Handle this in activity and manage UI in fragment
//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent)
//        if (binding.mlActivityMain.currentState == binding.mlActivityMain.endState) {
//            binding.lottieCreateBtn.pauseAnimation()
//            binding.lottieCreateBtn.frame = 0
//            binding.etRoom.setText("")
//            binding.etRoom.visibility = View.GONE
//            binding.mlActivityMain.transitionToStart()
//        }
//        intent?.data?.lastPathSegment?.let {
//            if (gameId.isNullOrBlank()) {
//                gameId = it
//                startActivityForResult(Intent(this, NameActivity::class.java), JOIN_ROOM_REQUEST)
//            }
//        }
//    }
}