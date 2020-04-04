package com.cafedroid.bingo_android

import android.util.Log
import com.github.nkzawa.emitter.Emitter
import com.github.nkzawa.socketio.client.IO
import com.github.nkzawa.socketio.client.Socket
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject

const val BASE_URL = "http://1d98a8b0.ngrok.io"

const val GAME_JOIN_EVENT = 1001
const val MEMBER_UPDATE_EVENT = 1003

var USERNAME = ""

object ApiEndpoints {
    const val GAME = "game/"
    const val JOIN = "join/"
    const val CREATE = "create/"
}

fun Any.getLoggerTag(): String {
    return this.javaClass.simpleName
}

object ApiConstants {
    const val ID = "id"
    const val NAME = "name"
    const val USER = "user"
}

object ActiveGameRoom {
    var activeRoom: GameRoom? = null
}

private val socketEventListener: Emitter.Listener = Emitter.Listener {
    Log.e("Utils", "${it[0]}")
    when (val eventId = (it[0] as JSONObject)["id"]) {
        GAME_JOIN_EVENT -> {
            val gameJoinEvent = Gson().fromJson(it[0].toString(), GameJoinEvent::class.java)
            ActiveGameRoom.activeRoom = gameJoinEvent.room
            EventBus.getDefault().post(gameJoinEvent)
        }
        MEMBER_UPDATE_EVENT -> {
            val roomUpdateEvent = Gson().fromJson(it[0].toString(), MemberUpdateEvent::class.java)
            ActiveGameRoom.activeRoom = roomUpdateEvent.room
            EventBus.getDefault().post(roomUpdateEvent)
        }
        else -> {
            Log.e("SocketEventListener", "$eventId not handled")
        }
    }
}

object BingoSocket {
    var socket: Socket? = null

    fun connect() {
        socket = IO.socket(BASE_URL)
        socket?.connect()
        socket?.on("event", socketEventListener)
    }
}

data class GameRoom(
    @SerializedName("_id") val roomId: String,
    @SerializedName("name") val roomName: String,
    @SerializedName("admin") var roomAdmin: String,
    @SerializedName("users") var roomMembers: List<String>,
    @SerializedName("state") var roomState: Int
)