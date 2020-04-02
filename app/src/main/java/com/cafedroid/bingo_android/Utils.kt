package com.cafedroid.bingo_android

import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.annotations.SerializedName

const val BASE_URL = "https://d88b79a2.ngrok.io/"

object ApiEndpoints {
    const val GAME = "game/"
    const val JOIN = "join/"
    const val CREATE = "create/"
}

object ApiConstants {
    const val NAME = "name"
    const val USER = "user"
}

object ActiveGameRoom {
    var activeRoom: GameRoom? = null
}

object BingoSocket {
    var socket: Socket? = null

    fun connect() {
        socket = IO.socket(BASE_URL)
        socket?.connect()
    }
}

data class GameRoom(
    @SerializedName("_id") val roomId: String,
    @SerializedName("name") val roomName: String,
    @SerializedName("admin") val roomAdmin: String,
    @SerializedName("users") val roomMembers: List<String>,
    @SerializedName("state") val roomState: Int
)