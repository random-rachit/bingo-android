package com.cafedroid.bingo_android.game

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.cafedroid.bingo_android.ActiveGameRoom
import com.cafedroid.bingo_android.AdminAction
import com.cafedroid.bingo_android.ApiConstants
import com.cafedroid.bingo_android.BingoSocket
import com.cafedroid.bingo_android.ButtonGlowEvent
import com.cafedroid.bingo_android.GameEvent
import com.cafedroid.bingo_android.GameEventAdapter
import com.cafedroid.bingo_android.GameLockEvent
import com.cafedroid.bingo_android.GameState
import com.cafedroid.bingo_android.GameStateChangeEvent
import com.cafedroid.bingo_android.GameTableAdapter
import com.cafedroid.bingo_android.GameWinEvent
import com.cafedroid.bingo_android.MainActivity
import com.cafedroid.bingo_android.NUMBER_STACK
import com.cafedroid.bingo_android.NumberStackChangeEvent
import com.cafedroid.bingo_android.R
import com.cafedroid.bingo_android.ResponseEvent
import com.cafedroid.bingo_android.SocketAction
import com.cafedroid.bingo_android.TurnUpdateEvent
import com.cafedroid.bingo_android.USERNAME
import com.cafedroid.bingo_android.databinding.FragmentGameBinding
import com.cafedroid.bingo_android.initNumberStack
import com.cafedroid.bingo_android.isAdmin
import com.cafedroid.bingo_android.isMyTurn
import com.cafedroid.bingo_android.popNumberStack
import com.cafedroid.bingo_android.showToast
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class GameFragment : Fragment() {

    private lateinit var binding: FragmentGameBinding


    private var mAdapter: GameTableAdapter? = null
    private var eventAdapter: GameEventAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val STAGING_TIMER: Long = 30
    }

    private var buttonGlowTime = 0L
    private var gameLocked = false
    private var sent = false

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        startCountDown()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    backPressHandler()
                }

            })
    }

    private fun backPressHandler() {
        when (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState)) {
            GameState.IN_GAME -> {
                if (isMyTurn()) requireContext().showToast("Do your turn and you may leave.")
                else showLeaveDialog()
            }

            GameState.READY -> {
                showLeaveDialog()
            }

            else -> findNavController().popBackStack()
        }
    }

    private fun startCountDown() {
        if (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.READY) {
            binding.tvCountdownTimer.visibility = View.VISIBLE

            object : CountDownTimer(STAGING_TIMER * 1000, 1000) {
                override fun onFinish() {
                    binding.btnShuffle.visibility = View.INVISIBLE
                    if (isAdmin()) {
                        BingoSocket.socket?.emit(SocketAction.ACTION_ADMIN, JSONObject().apply {
                            put("action", AdminAction.LAUNCH_GAME.value)
                            put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
                            put(ApiConstants.USER, USERNAME)
                        })
                    }
                    requireContext().showToast("Game will start now.")
                }

                override fun onTick(millisUntilFinished: Long) {
                    val seconds = millisUntilFinished / 1000
                    if (seconds == 3.toLong()) {
                        binding.tvCountdownTimer.setTextColor(
                            ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                        )
                        mAdapter?.toggleTableLock(true)
                    }
                    binding.tvCountdownTimer.text =
                        String.format("00:${if (seconds < 0) "0" else ""}%s", seconds)
                }
            }.start()
        }
    }

    private fun initViews() {
        binding.run {
            mAdapter = GameTableAdapter(requireContext())
            eventAdapter = GameEventAdapter(requireContext())

            rvGameEvent.apply {
                layoutManager = LinearLayoutManager(requireContext())
                adapter = eventAdapter
                visibility = View.INVISIBLE
            }

            rvGameTable.apply {
                layoutManager = GridLayoutManager(requireContext(), 5)
                adapter = mAdapter
            }
            tvStackTop.text = NUMBER_STACK.peek().toString()
            btnShuffle.setOnClickListener {
                shuffleGameTable()
            }
            tvBingoButton.isEnabled = false
            tvBingoButton.setOnClickListener {
                val timeTakenToTap = System.currentTimeMillis() - buttonGlowTime
                if (sent.not()) {
                    BingoSocket.socket?.let {
                        it.emit(SocketAction.ACTION_WIN, JSONObject().apply {
                            put(ApiConstants.ID, ActiveGameRoom.activeRoom?.roomId)
                            put(ApiConstants.USER, USERNAME)
                            put(ApiConstants.TIME_TAKEN, timeTakenToTap)
                        })
                    }
                    sent = true
                }
            }
            tvButtonReset.setOnClickListener {
                ActiveGameRoom.activeRoom = null
                startActivity(Intent(requireContext(), MainActivity::class.java))
            }

            tvShareWin.setOnClickListener {
                captureView(clGameView)?.let {
                    shareImageUri(it)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    private fun shuffleGameTable() {
        if (mAdapter?.isTableLocked == true || GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.IN_GAME) {
            requireContext().showToast("Table is locked. Cannot shuffle now.")
            return
        }
        initNumberStack()
        val list = mAdapter?.mList
        list?.forEach {
            it.number = popNumberStack() ?: 0
        }
        list?.shuffle()
        mAdapter?.notifyDataSetChanged()
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    fun onGameEvent(event: ResponseEvent) {
        when (event) {
            is NumberStackChangeEvent -> if (NUMBER_STACK.empty()) {
                binding.flStackTopContainer.visibility = View.INVISIBLE
            } else {
                binding.flStackTopContainer.visibility = View.VISIBLE
                binding.tvStackTop.text = NUMBER_STACK.peek().toString()
            }

            is TurnUpdateEvent -> {
                if (isMyTurn().not()) mAdapter?.toggleTableLock(true)
                eventAdapter?.pushEvent(
                    GameEvent(
                        event.user,
                        "${if (event.user == USERNAME) "You" else event.user} pushed ${event.number}"
                    )
                )
                binding.rvGameEvent.scrollToPosition((eventAdapter?.itemCount ?: 0) - 1)
                mAdapter?.markDone(event.number, false)
                if (gameLocked.not()) setGameView()
            }

            is GameStateChangeEvent -> {
                if (GameState.getGameStateByValue(ActiveGameRoom.activeRoom?.roomState) == GameState.IN_GAME) setGameView(
                    true
                )
            }

            is ButtonGlowEvent -> {
                binding.tvBingoButton.visibility = View.VISIBLE
                binding.tvBingoButton.isEnabled = true
                if (binding.tvBingoButton.isEnabled) buttonGlowTime = System.currentTimeMillis()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrate()
                }
            }

            is GameLockEvent -> {
                mAdapter?.toggleTableLock(true)
                gameLocked = true
            }

            is GameWinEvent -> {
                setWinView(event.user)
                if (event.user == USERNAME) {
                    binding.lottieResultView.setAnimation(R.raw.celebration)
                    binding.lottieResultView.playAnimation()
                    requireContext().showToast("Congratulations, You won the game!")
                } else {
                    binding.lottieResultView.setAnimation(R.raw.bingo_lost)
                    binding.lottieResultView.playAnimation()
                    requireContext().showToast("${event.user} won the game! Better luck next time.")
                }
            }
        }
    }

    private fun setWinView(winner: String) {
        binding.tvBingoButton.visibility = View.INVISIBLE
        binding.rvGameEvent.visibility = View.INVISIBLE
        binding.tvButtonReset.visibility = View.VISIBLE
        binding.tvShareWin.visibility = View.VISIBLE
//        tv_display_name.text =
//            String.format("%s won!", if (winner == USERNAME) "Your" else winner)
//        Glide.with(this).load(ROBO_HASH_URL + winner).into(iv_display_bot)
    }

    private fun setGameView(stateChange: Boolean = false) {

        if (stateChange) {
            binding.flTurnMemberContainer.removeAllViews()
            val view = LayoutInflater.from(requireContext())
                .inflate(R.layout.member_item, binding.flTurnMemberContainer, false)
            view.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.transparent
                )
            )
            binding.flTurnMemberContainer.addView(view)
            binding.tvCountdownTimer.visibility = View.INVISIBLE
            binding.flTurnMemberContainer.visibility = View.VISIBLE
            binding.rvGameEvent.visibility = View.VISIBLE
        }
        val activeRoom = ActiveGameRoom.activeRoom
        activeRoom?.let {
            val activeUser =
                it.roomMembers[activeRoom.userTurn]
            mAdapter?.toggleTableLock(
                (activeRoom.roomMembers[activeRoom.userTurn] == USERNAME).not()
            )
//            cl_member_item_root.setBackgroundColor(
//                ContextCompat.getColor(
//                    this,
//                    if (activeUser == USERNAME) R.color.green else android.R.color.white
//                )
//            )
//            tv_display_name.text =
//                String.format("%s turn", if (activeUser == USERNAME) "Your" else "$activeUser\'s")
//            Glide.with(this).load(ROBO_HASH_URL + activeUser).into(iv_display_bot)
        }
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun vibrate() {
        val millis = 300L
        val vibrationManager =
            requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            vibrationManager.vibrate(
                VibrationEffect.createOneShot(
                    millis,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else
            vibrationManager.vibrate(
                VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
            )
    }

    private fun showLeaveDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.leave)
            .setMessage(R.string.leave_message)
            .setPositiveButton("Leave") { _, _ ->
                leaveRoom()
                backPressHandler()
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
    }

    private fun captureView(view: View): Uri? {
        val image = Bitmap.createBitmap(
            view.width,
            view.height,
            Bitmap.Config.RGB_565
        )
        //Draw the view inside the Bitmap
        view.draw(Canvas(image))

        //Store to sdcard
        return saveImage(image)
    }

    private fun saveImage(image: Bitmap): Uri? {
        val imagesFolder = File(requireActivity().filesDir, "images")
        var uri: Uri? = null
        try {
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "IMG_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(requireContext(), requireContext().packageName, file)
        } catch (e: IOException) {
            Log.d(
                this.javaClass.simpleName,
                "IOException while trying to write file for sharing: " + e.message
            )
        }
        return uri
    }

    private fun shareImageUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(
                Intent.EXTRA_TEXT,
                "Download and play now https://bingo.cafedroid.com/download"
            )
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            type = "image/png"
        }
        startActivity(intent)
        findNavController().popBackStack()
    }


}