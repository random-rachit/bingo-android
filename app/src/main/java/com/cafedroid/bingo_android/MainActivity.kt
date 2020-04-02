package com.cafedroid.bingo_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.ParsedRequestListener
import com.github.nkzawa.socketio.client.IO
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
    }

    private fun initViews() {
        btn_create.setOnClickListener {
            if (validateInput()) {
                createRoom()
            }
        }
    }

    private fun createRoom() {
        AndroidNetworking.post(BASE_URL + ApiEndpoints.GAME + ApiEndpoints.CREATE)
            .addBodyParameter(ApiConstants.NAME, et_room.text.toString())
            .addBodyParameter(ApiConstants.USER, et_user.text.toString())
            .build().getAsObject(GameRoom::class.java, object : ParsedRequestListener<GameRoom> {
                override fun onResponse(response: GameRoom?) {
                    ActiveGameRoom.activeRoom = response
                    proceedToActiveRoom()
                }

                override fun onError(anError: ANError?) {
                    anError?.printStackTrace()
                }
            })
    }

    private fun proceedToActiveRoom() {
        startActivity(Intent(this, GameActivity::class.java))
    }

    private fun validateInput(): Boolean {
        return et_room.text.toString().isNotBlank() && et_user.text.toString().isNotBlank()
    }
}
