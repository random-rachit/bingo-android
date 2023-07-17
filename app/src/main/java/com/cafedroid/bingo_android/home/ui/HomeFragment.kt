package com.cafedroid.bingo_android.home.ui

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
import com.cafedroid.bingo_android.ResponseEvent
import com.cafedroid.bingo_android.SocketAction
import com.cafedroid.bingo_android.SocketErrorEvent
import com.cafedroid.bingo_android.databinding.FragmentHomeBinding
import com.cafedroid.bingo_android.showToast
import com.cafedroid.bingo_android.utils.NAME_KEY
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject

class HomeFragment : Fragment() {

    private var proceedNavigation: (user: String) -> Unit = {
        requireContext().showToast("Something went wrong.")
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
        binding.run {
            if (mlActivityMain.currentState == mlActivityMain.endState) {
                lottieCreateBtn.pauseAnimation()
                lottieCreateBtn.frame = 0
                etRoom.setText("")
                etRoom.visibility = View.GONE
                mlActivityMain.transitionToStart()
            } else requireActivity().finish()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupCallbacks()
    }

    private fun setupCallbacks() {
        requireActivity().onBackPressedDispatcher.addCallback(
            requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressed()
                }
            })

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(NAME_KEY)
            ?.observe(viewLifecycleOwner, proceedNavigation)
    }

    private fun initViews() {
        binding.cvBtnCreate.setOnClickListener {
            binding.etRoom.visibility = View.VISIBLE
            binding.mlActivityMain.transitionToEnd()
        }
        binding.lottieCreateBtn.setOnClickListener {
            if (validateInput()) {
                proceedNavigation = ::createRoom
                binding.lottieCreateBtn.playAnimation()
                inputName()
            } else requireContext().showToast("Enter the room name ☝️")
        }
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
                proceedNavigation = ::joinRoom
                inputName()
            }
        }
    }

    private fun inputName() {
        val action = HomeFragmentDirections.homeToName()
        findNavController().navigate(action)
    }

    private fun joinRoom(user: String) {
        binding.cvBtnCreate.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        BingoSocket.socket?.let {
            it.emit(SocketAction.ACTION_JOIN, JSONObject().apply {
                put(ApiConstants.ID, gameId)
                put(ApiConstants.USER, user)
            })
        }
    }

    private fun createRoom(user: String) {
        BingoSocket.socket?.let {
            it.emit(SocketAction.ACTION_CREATE, JSONObject().apply {
                put(ApiConstants.NAME, binding.etRoom.text.toString().trim())
                put(ApiConstants.USER, user)
            })
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
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