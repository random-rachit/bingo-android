package com.cafedroid.bingo_android

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cafedroid.bingo_android.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

//    override fun onStart() {
//        super.onStart()
//        intent.data?.lastPathSegment?.let {
//            if (gameId.isNullOrBlank()) {
//                gameId = it
//                startActivityForResult(Intent(this, NameActivity::class.java), JOIN_ROOM_REQUEST)
//            }
//        }
//    }


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